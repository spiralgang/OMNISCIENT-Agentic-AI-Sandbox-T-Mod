package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AICollabManager
import com.example.ai.AgentCommand
import com.example.ai.ChatMessage
import com.example.vfs.VirtualFile
import com.example.vfs.VirtualFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WorkspaceViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val container = (application as com.example.SovereignApplication).container
    private val vfs = container.vfs
    private val aiCollab = container.aiCollabManager
    private val db = container.database
    private val auditLogDao = db.auditLogDao()

    val files: StateFlow<List<VirtualFile>> = vfs.files.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val messages: StateFlow<List<ChatMessage>> = aiCollab.messages.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val isProcessing: StateFlow<Boolean> = aiCollab.isProcessing.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val auditLogs: StateFlow<List<com.example.db.AuditLog>> = auditLogDao.getAllLogs().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _userRole = kotlinx.coroutines.flow.MutableStateFlow("Developer")
    val userRole: StateFlow<String> = _userRole.asStateFlow()
    
    val trusted: StateFlow<Boolean> = kotlinx.coroutines.flow.MutableStateFlow(true).asStateFlow()

    fun setUserRole(role: String) {
        _userRole.value = role
        logAction("Role changed to $role", role)
    }

    fun logAction(action: String, role: String) {
        viewModelScope.launch {
            auditLogDao.insertLog(com.example.db.AuditLog(action = action, role = role))
        }
    }

    private val _pendingCommands = MutableSharedFlow<AgentCommand>()
    val pendingCommands = _pendingCommands.asSharedFlow()

    val rcloneMounts = kotlinx.coroutines.flow.MutableStateFlow<Map<String, String>>(emptyMap())
    val logs = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(
        listOf("Sovereign Executive Core: initialized.", "Listening for workspace events...")
    )
    val selectedFilePath = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    val commandHistory = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())

    fun loadHistory(context: android.content.Context) {
        val prefs = context.getSharedPreferences("sovereign_terminal", android.content.Context.MODE_PRIVATE)
        val saved = prefs.getString("command_history", "") ?: ""
        if (saved.isNotEmpty()) {
            val list = saved.split("||_SEPERATOR_||").filter { it.isNotBlank() }
            commandHistory.value = list
        }
    }

    fun saveHistory(context: android.content.Context, command: String) {
        if (command.isNotBlank()) {
            val current = commandHistory.value.toMutableList()
            // Avoid duplicate consecutive entries
            if (current.isEmpty() || current.last() != command) {
                current.add(command)
                commandHistory.value = current
                
                val prefs = context.getSharedPreferences("sovereign_terminal", android.content.Context.MODE_PRIVATE)
                val saved = current.joinToString("||_SEPERATOR_||")
                prefs.edit().putString("command_history", saved).apply()
            }
        }
    }

    fun log(msg: String) {
        val current = logs.value.toMutableList()
        current.add(msg)
        logs.value = current
    }

    fun mountRclone(provider: String, remote: String) {
        val current = rcloneMounts.value.toMutableMap()
        current[provider] = remote
        rcloneMounts.value = current
        log("Rclone service: Mount [$provider] targeted at '$remote'")
        
        // Insert mockup files inside Virtual File System
        updateFile(
            "mnt/rclone/${provider.lowercase()}/cloud_status.yaml",
            """mount_point: "/mnt/rclone/${provider.lowercase()}"
provider: "${provider.uppercase()}"
remote_address: "${remote}"
mounted_at: "${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}"
file_system: "rclone-vfs"
cache_mode: "writes"
encryption: "secure-gcm"
status: "MOUNTED_AND_ACTIVE"
"""
        )
        updateFile(
            "mnt/rclone/${provider.lowercase()}/shared_workspace_notes.txt",
            "Collaboration hub connected to $provider.\nModified files sync automatically inside the background Termux/rclone thread."
        )
    }

    fun unmountRclone(provider: String) {
        val current = rcloneMounts.value.toMutableMap()
        if (current.containsKey(provider)) {
            current.remove(provider)
            rcloneMounts.value = current
            log("Rclone service: Unmounted remote connectivity for [$provider]")
            deleteFile("mnt/rclone/${provider.lowercase()}/cloud_status.yaml")
            deleteFile("mnt/rclone/${provider.lowercase()}/shared_workspace_notes.txt")
        }
    }

    fun sendPrompt(prompt: String, adminGate: suspend (suspend () -> Unit) -> Unit) {
        viewModelScope.launch {
            aiCollab.sendMessage(prompt, files.value) { changes, commands ->
                for (change in changes) {
                    when (change.action.lowercase()) {
                        "create", "update" -> {
                            if (change.content != null) {
                                viewModelScope.launch(Dispatchers.Main) {
                                    adminGate { vfs.updateFile(change.path, change.content) }
                                }
                            }
                        }
                        "delete" -> {
                            viewModelScope.launch(Dispatchers.Main) {
                                adminGate { vfs.deleteFile(change.path) }
                            }
                        }
                    }
                }
                viewModelScope.launch {
                    for (cmd in commands) {
                        _pendingCommands.emit(cmd)
                    }
                }
            }
        }
    }

    fun updateFile(path: String, content: String) {
        vfs.updateFile(path, content)
    }

    fun deleteFile(path: String) {
        vfs.deleteFile(path)
    }

    fun generateZipBytes(): ByteArray {
        val currentFiles = files.value
        val baos = java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(baos).use { zos ->
            for (file in currentFiles) {
                val entry = java.util.zip.ZipEntry(file.path)
                zos.putNextEntry(entry)
                zos.write(file.content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    fun generateJsonBytes(): ByteArray {
        val currentFiles = files.value
        val stringBuilder = StringBuilder()
        stringBuilder.append("[\n")
        currentFiles.forEachIndexed { index, file ->
            val escapedContent = file.content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            stringBuilder.append("  {\n")
            stringBuilder.append("    \"path\": \"${file.path}\",\n")
            stringBuilder.append("    \"content\": \"$escapedContent\"\n")
            stringBuilder.append("  }")
            if (index < currentFiles.size - 1) stringBuilder.append(",")
            stringBuilder.append("\n")
        }
        stringBuilder.append("]")
        return stringBuilder.toString().toByteArray(Charsets.UTF_8)
    }
}
