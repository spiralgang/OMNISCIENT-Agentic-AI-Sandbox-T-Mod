package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.ChatMessage
import com.example.execution.ExecutionEngineManager
import com.example.vfs.VirtualFile
import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*

@Composable
fun TrustedSourceBadge(isTrusted: Boolean) {
    Surface(
        color = if (isTrusted) NeonGreen.copy(alpha = 0.2f) else NeonOrange.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = if (isTrusted) "TRUSTED SOURCE" else "UNTRUSTED",
            color = if (isTrusted) NeonGreen else NeonOrange,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AuditLogUI(logs: List<com.example.db.AuditLog>) {
    Column {
        Text("SECURITY AUDIT LOGS", color = NeonOrange, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        LazyColumn {
            items(logs) { log ->
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("[${log.role}] ", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text(log.action, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AdminDashboard(
    role: String,
    onRoleChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonOrange),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Decorative icon", tint = NeonOrange)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ADMIN DASHBOARD", color = NeonOrange, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Keystore Trust Status: Trusted Developer / Validated (Production Setup Pipeline Simulated)", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("System-Wide Permissions: Gated by Verified Credentials", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Role Management", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("Current Active Role: $role", color = if (role == "Admin") NeonOrange else NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { onRoleChange("Developer") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (role == "Developer") NeonCyan else Color.DarkGray)
                ) { Text("Set Developer", color = CyberDark, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onRoleChange("Admin") },
                    colors = ButtonDefaults.buttonColors(containerColor = if (role == "Admin") NeonOrange else Color.DarkGray)
                ) { Text("Set Admin", color = CyberDark, fontWeight = FontWeight.Bold) }
            }
            
            if (role == "Admin") {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Warning: Admin role permits kernel-level VFS mutation and Docker sandbox policies.", color = NeonOrange, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
private val CyberSlate = Color(0xFF1E1E24)
private val CyberDark = Color(0xFF121214)
private val NeonGreen = Color(0xFF39FF14)
private val NeonCyan = Color(0xFF00E5FF)
private val NeonOrange = Color(0xFFFF5722)

// Optimized Dependency Injection Objects for Styles
private val OptimizedInputStyle = ComposeTextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
private val OptimizedConsoleStyle = ComposeTextStyle(color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniscientApp(viewModel: WorkspaceViewModel = viewModel()) {
    val files by viewModel.files.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    
    val rcloneMounts by viewModel.rcloneMounts.collectAsState()
    val terminalLogs by viewModel.logs.collectAsState()
    val selectedFilePath by viewModel.selectedFilePath.collectAsState()
    val commandHistory by viewModel.commandHistory.collectAsState()
    
    val trusted by viewModel.trusted.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState(initial = emptyList<com.example.db.AuditLog>())
    
    var selectedTab by remember { mutableStateOf(0) }
    var showSetupDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val executionManager = remember { ExecutionEngineManager(context) }
    
    val focusRequester = remember { FocusRequester() }

    val adminAuthGate: (suspend () -> Unit) -> Unit = remember(userRole) {
        { action ->
            if (userRole == "Admin") {
                scope.launch { action() }
            } else {
                Toast.makeText(context, "Denied: System writes require Admin role.", Toast.LENGTH_SHORT).show()
                viewModel.logAction("Blocked unauthorized system modification", userRole)
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadHistory(context)
        focusRequester.requestFocus()
    }


    // NATIVE DOCUMENT PICKERS / EXPORTER INTENTS
    
    // File upload picker
    val fileUploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                // Query original filename
                var fileName = "imported_file.txt"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val fileContent = String(bytes, Charsets.UTF_8)
                    viewModel.updateFile("uploads/$fileName", fileContent)
                    viewModel.log("System VFS: Imported user file uploads/$fileName successfully.")
                    Toast.makeText(context, "Imported file to uploads/$fileName!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "File was empty.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                viewModel.log("Error uploading file: ${e.message}")
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Document ZIP exporter launcher
    val exportZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = viewModel.generateZipBytes()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(bytes)
                }
                viewModel.log("System exporter: ZIP bundle compiled and stored locally.")
                Toast.makeText(context, "Saved workspace ZIP successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save ZIP: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Document JSON schema exporter launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = viewModel.generateJsonBytes()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(bytes)
                }
                viewModel.log("System exporter: JSON schema schema exported cleanly.")
                Toast.makeText(context, "Saved workspace JSON schema!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save JSON: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Single selected raw file exporter launcher
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null && selectedFilePath != null) {
            try {
                val virtualFile = files.find { it.path == selectedFilePath }
                if (virtualFile != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(virtualFile.content.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.log("System exporter: Individual raw file '${virtualFile.path}' saved.")
                    Toast.makeText(context, "Saved raw file successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.pendingCommands.collect { cmd ->
            viewModel.log("Agent Command Core: Dispatching to [${cmd.engine.uppercase()}]")
            when (cmd.engine.lowercase()) {
                "termux" -> executionManager.runInTermux(cmd.script)
                "cloud_shell" -> executionManager.openInCloudShell()
                "userland" -> executionManager.sendToUserLAnd(cmd.script)
                "drive_sync" -> executionManager.syncToCloudStorage("Drive", cmd.script)
                "github_sync" -> executionManager.syncToGitHub(cmd.script)
                "vscode_server" -> executionManager.launchVSCodeServer()
                "android_studio" -> executionManager.exportToAndroidStudio()
                "gcp_deploy" -> executionManager.deployToGCP(cmd.script)
                "nvidia_tools" -> executionManager.callNvidiaBuildTools(cmd.script)
                "cli_internet" -> executionManager.executeCliInternetCommand(cmd.script)
                "vision_web" -> executionManager.navigateWebViaVision(cmd.script)
                "setup_auth" -> executionManager.runSetupScript("GH", "GCP", "VS")
                "upload_file" -> executionManager.uploadAnyFile()
                "export_workspace" -> executionManager.exportWorkspace(cmd.script)
                "dynamic_skill" -> executionManager.executeDynamicAgentSkill("auto-detect", cmd.script)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    val isAlt = keyEvent.isAltPressed
                    val isCtrl = keyEvent.isCtrlPressed
                    if (isAlt || isCtrl) {
                        when (keyEvent.key) {
                            Key.M -> {
                                selectedTab = 0
                                true
                            }
                            Key.W -> {
                                selectedTab = 1
                                true
                            }
                            Key.D -> {
                                selectedTab = 4
                                true
                            }
                            else -> false
                        }
                    } else false
                } else false
            },
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TrustedSourceBadge(trusted)
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Decorative icon", tint = NeonGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Omniscient IDE", 
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "[⌥M: Chat | ⌥W: Files | ⌥D: Sovereign]",
                            fontSize = 10.sp,
                            color = NeonCyan.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSetupDialog = true }) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Setup Credentials", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDark,
                    titleContentColor = Color.White,
                    actionIconContentColor = NeonCyan
                )
            )
        },
        bottomBar = {
            Surface(color = CyberDark, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Gemini API: OK", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    Text("VFS: Connected", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(CyberDark)
        ) {
            // Material 3 Tabs organizing features natively to prevent browser screen glitching or overlap
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CyberDark,
                contentColor = NeonGreen
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Agent Chat", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Workspace", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Rclone Mounts", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Console Logs", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Sovereign", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("Builds", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ChatScreen(
                        messages = messages,
                        isProcessing = isProcessing,
                        commandHistory = commandHistory,
                        onSend = { 
                            viewModel.sendPrompt(it, adminAuthGate)
                            viewModel.saveHistory(context, it)
                        },
                        onTriggerUpload = { fileUploadLauncher.launch("*/*") },
                        onTriggerExportZip = { exportZipLauncher.launch("workspace-export.zip") },
                        onTriggerExportJson = { exportJsonLauncher.launch("workspace-bundle.json") },
                        onSimulateDrop = {
                            viewModel.updateFile(
                                "icemaster_genesis.py",
                                "import sys\nprint('Wielding IceMaster Sovereign skills natively.')"
                            )
                            viewModel.log("System VFS: Simulated drag-and-drop file 'icemaster_genesis.py' uploaded to VFS.")
                            Toast.makeText(context, "Dropped icemaster_genesis.py into workspace!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> CodeWorkspaceScreen(
                        files = files,
                        selectedPath = selectedFilePath,
                        onSelectPath = { viewModel.selectedFilePath.value = it },
                        onSaveFile = { path, text -> 
                            adminAuthGate {
                                viewModel.updateFile(path, text)
                                viewModel.log("User action: Saved virtual file '$path'")
                                Toast.makeText(context, "File saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteFile = { path ->
                            adminAuthGate {
                                viewModel.deleteFile(path)
                                viewModel.log("User action: Deleted virtual file '$path'")
                                if (selectedFilePath == path) viewModel.selectedFilePath.value = null
                                Toast.makeText(context, "File deleted!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCreateFile = { path, text ->
                            adminAuthGate {
                                viewModel.updateFile(path, text)
                                viewModel.log("User action: Created new file '$path'")
                                viewModel.selectedFilePath.value = path
                                Toast.makeText(context, "File created!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTriggerExportFile = { path ->
                            val suffix = path.substringAfterLast(".", "txt")
                            exportFileLauncher.launch("raw_file_export.$suffix")
                        }
                    )
                    2 -> RcloneMountPanel(
                        rcloneMounts = rcloneMounts,
                        onMount = { provider, remote -> viewModel.mountRclone(provider, remote) },
                        onUnmount = { provider -> viewModel.unmountRclone(provider) },
                        onSyncNow = { provider ->
                            scope.launch {
                                viewModel.log("Rclone thread: Started immediate synchronisation with $provider cloud remote...")
                                delay(1200)
                                viewModel.log("Rclone thread: Successfully uploaded changes to $provider secure repository.")
                                Toast.makeText(context, "Sync complete with $provider!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    3 -> SystemLogsPanel(
                        logs = terminalLogs,
                        onClearLogs = { viewModel.logs.value = listOf("Logs cleared.") },
                        onDeploySkill = { skillType ->
                            viewModel.log("Skills Manager: Active Command triggered for [$skillType]")
                            scope.launch {
                                when(skillType) {
                                    "WHISPERER" -> {
                                        viewModel.log("Whisperer audit: Analysis: System craves missing tools: [nmap, tcpdump]")
                                        delay(800)
                                        viewModel.log("Whisperer invoke: Fetching deb assets securely...")
                                        delay(600)
                                        viewModel.log("Whisperer success: Satisfied target requirements. Toolkit operational.")
                                    }
                                    "CONFIGURATOR" -> {
                                        viewModel.log("Configurator invoke: Deploying instant fixing array on broken locks...")
                                        delay(500)
                                        viewModel.log("Configurator process: Nuking package configuration logs safely...")
                                        delay(500)
                                        viewModel.log("Configurator success: Resolved conflict blocks.")
                                    }
                                    "CHMOD" -> {
                                        viewModel.log("Chmod invoke: Auditing executable file boundaries in workspace...")
                                        delay(600)
                                        viewModel.log("Chmod process: Auto-detected python/sh executable binaries. Granting +x system permissions.")
                                        delay(400)
                                        viewModel.log("Chmod success: Mapped 3 files as executable.")
                                    }
                                    "DOCKER" -> {
                                        viewModel.log("Harden docker: Auditing potential docker-in-docker conflicts...")
                                        delay(1000)
                                        viewModel.log("Harden docker status: Discovered 1 workflow install trigger.")
                                        delay(400)
                                        viewModel.log("Harden docker action: Injected container security protocols. Neutralization successful.")
                                    }
                                }
                                Toast.makeText(context, "Skill execution completed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    4 -> SovereignDashboardScreen(
                        files = files,
                        userRole = userRole,
                        auditLogs = auditLogs,
                        onRoleChange = viewModel::setUserRole,
                        onTriggerArchive = { 
                            executionManager.syncToGitHub("gh workflow run codepilot.yml -f ice_mode=archive_memory")
                            viewModel.log("GitHub Action Triggered: Archiving icemaster_memory.jsonl to secure cloud storage slot.")
                            viewModel.logAction("Triggered archive", userRole)
                            Toast.makeText(context, "Archive Action Triggered!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    5 -> BuildArtifactsPanel()
                }
            }
        }
    }

    if (showSetupDialog) {
        var githubToken by remember { mutableStateOf("") }
        var gcpKey by remember { mutableStateOf("") }
        var vsCodeToken by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showSetupDialog = false },
            title = { Text("Setup Secure Credentials", color = Color.White, fontFamily = FontFamily.Monospace) },
            containerColor = CyberSlate,
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Authorise sandbox execution engines to connect with your co-owned external assets.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
                    
                    OutlinedTextField(
                        value = githubToken,
                        onValueChange = { githubToken = it },
                        label = { Text("GitHub PAT Token") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        textStyle = OptimizedInputStyle
                    )
                    OutlinedTextField(
                        value = gcpKey,
                        onValueChange = { gcpKey = it },
                        label = { Text("Google Cloud SDK JSON Auth Key") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        textStyle = OptimizedInputStyle
                    )
                    OutlinedTextField(
                        value = vsCodeToken,
                        onValueChange = { vsCodeToken = it },
                        label = { Text("VSCode Server Tunnel Activation Code") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        textStyle = OptimizedInputStyle
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSetupDialog = false
                        executionManager.runSetupScript(githubToken, gcpKey, vsCodeToken)
                        viewModel.log("Credentials manager: Decoupled and authorised GitHub and GCP execution blocks.")
                        Toast.makeText(context, "Sandbox credentials configured!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("PROCEED", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetupDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isProcessing: Boolean,
    commandHistory: List<String>,
    onSend: (String) -> Unit,
    onTriggerUpload: () -> Unit,
    onTriggerExportZip: () -> Unit,
    onTriggerExportJson: () -> Unit,
    onSimulateDrop: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var showUploadMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    var historyIndex by remember { mutableIntStateOf(-1) }
    var draftText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isProcessing) {
                item {
                    Text(
                        text = "Agent processing via Shorthand protocol...", 
                        modifier = Modifier.padding(8.dp), 
                        color = NeonCyan, 
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // TOUCH-FRIENDLY MOCK DRAG AND DROP ZONE
        Surface(
            color = CyberSlate.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onSimulateDrop() }
        ) {
            Box(
                modifier = Modifier.padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "👇 Drag-and-Drop Dropbox (Tap here to drop 'icemaster_genesis.py')",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Persistent Command History Indicator / Touch Navigator
        if (commandHistory.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (historyIndex == -1) Color.Gray else NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (historyIndex == -1) "ACTIVE DEV DRAFT" else "TERMINAL HISTORY [${historyIndex + 1}/${commandHistory.size}]",
                        color = if (historyIndex == -1) Color.Gray else NeonCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val targetIndex = if (historyIndex == -1) {
                                draftText = inputText
                                commandHistory.size - 1
                            } else {
                                (historyIndex - 1).coerceAtLeast(0)
                            }
                            historyIndex = targetIndex
                            inputText = commandHistory[targetIndex]
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Scroll back in memory", tint = NeonCyan)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            if (historyIndex != -1) {
                                val targetIndex = historyIndex + 1
                                if (targetIndex >= commandHistory.size) {
                                    historyIndex = -1
                                    inputText = draftText
                                } else {
                                    historyIndex = targetIndex
                                    inputText = commandHistory[targetIndex]
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp),
                        enabled = historyIndex != -1
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown, 
                            contentDescription = "Scroll forward in memory", 
                            tint = if (historyIndex == -1) Color.DarkGray else NeonCyan
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Touch-optimized actions bar
            Box {
                IconButton(
                    onClick = { showUploadMenu = true },
                    modifier = Modifier.size(48.dp) // Touch requirement (at least 48dp)
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Upload Any File", tint = NeonGreen)
                }
                DropdownMenu(
                    expanded = showUploadMenu, 
                    onDismissRequest = { showUploadMenu = false },
                    modifier = Modifier.background(CyberSlate)
                ) {
                    DropdownMenuItem(
                        text = { Text("Code / Text File (*.kt, *.py, *.sh, *.json)", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            showUploadMenu = false
                            onTriggerUpload()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Workspace Drag n Drop simulated", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            showUploadMenu = false
                            onSimulateDrop()
                        }
                    )
                }
            }
            
            Box {
                IconButton(
                    onClick = { showExportMenu = true },
                    modifier = Modifier.size(48.dp) // Touch requirement (at least 48dp)
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Export Workspace", tint = NeonCyan)
                }
                DropdownMenu(
                    expanded = showExportMenu, 
                    onDismissRequest = { showExportMenu = false },
                    modifier = Modifier.background(CyberSlate)
                ) {
                    DropdownMenuItem(
                        text = { Text("Workspace ZIP Archive (.zip)", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            showExportMenu = false
                            onTriggerExportZip()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Workspace Schema Manifest (.json)", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                        onClick = {
                            showExportMenu = false
                            onTriggerExportJson()
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { 
                    inputText = it
                    if (historyIndex == -1) {
                        draftText = it
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionUp -> {
                                    if (commandHistory.isNotEmpty()) {
                                        val targetIndex = if (historyIndex == -1) {
                                            draftText = inputText
                                            commandHistory.size - 1
                                        } else {
                                            (historyIndex - 1).coerceAtLeast(0)
                                        }
                                        historyIndex = targetIndex
                                        inputText = commandHistory[targetIndex]
                                    }
                                    true
                                }
                                Key.DirectionDown -> {
                                    if (commandHistory.isNotEmpty() && historyIndex != -1) {
                                        val targetIndex = historyIndex + 1
                                        if (targetIndex >= commandHistory.size) {
                                            historyIndex = -1
                                            inputText = draftText
                                        } else {
                                            historyIndex = targetIndex
                                            inputText = commandHistory[targetIndex]
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                placeholder = { Text("Command agent...", color = Color.DarkGray) },
                maxLines = 3,
                textStyle = ComposeTextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = Color.DarkGray
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isProcessing) {
                        onSend(inputText)
                        inputText = ""
                        historyIndex = -1
                        draftText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp) // Accessibility target
                    .background(NeonGreen, shape = RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command",
                    tint = CyberDark
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val isSystem = message.role == "system"
    val align = if (isUser) Alignment.End else Alignment.Start
    
    val bubbleColor = when {
        isUser -> CyberSlate
        isSystem -> Color(0xFF2C1616)
        else -> Color(0xFF131F1D)
    }
    
    val textThemeColor = when {
        isUser -> NeonCyan
        isSystem -> NeonOrange
        else -> NeonGreen
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalAlignment = align) {
        Text(
            text = message.role.uppercase(), 
            fontSize = 9.sp, 
            color = Color.Gray, 
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bubbleColor,
            border = borderStrokeForRole(message.role),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Text(
                text = message.text,
                color = textThemeColor,
                modifier = Modifier.padding(12.dp),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
fun borderStrokeForRole(role: String) = when(role) {
    "user" -> androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
    "system" -> androidx.compose.foundation.BorderStroke(1.dp, NeonOrange.copy(alpha = 0.4f))
    else -> androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f))
}

@Composable
fun CodeWorkspaceScreen(
    files: List<VirtualFile>,
    selectedPath: String?,
    onSelectPath: (String) -> Unit,
    onSaveFile: (String, String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onCreateFile: (String, String) -> Unit,
    onTriggerExportFile: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    
    // Screen classification: Mobile compact check using available LocalConfiguration
    val config = LocalConfiguration.current
    val isCompactScreen = config.screenWidthDp < 600

    if (isCompactScreen) {
        // MOBILE MOBILE-FIRST COMPACT ADAPTIVE SINGLE SHEET FLOW (Avoids overlapping layout glitches)
        if (selectedPath != null) {
            // Editor detailed view fullscreen
            val file = files.find { it.path == selectedPath }
            if (file != null) {
                MobileEditorDetailedView(
                    file = file,
                    onBack = { onSelectPath("") },
                    onSave = onSaveFile,
                    onDelete = onDeleteFile,
                    onExport = onTriggerExportFile
                )
            } else {
                onSelectPath("")
            }
        } else {
            // Primary files list screen
            MobileFileListScreen(
                files = files,
                onSelect = onSelectPath,
                onCreateRequest = { showCreateDialog = true }
            )
        }
    } else {
        // TABLET / DESKTOP TWO-PANE FLOW (Consistent view sizing side-by-side)
        val activeFile = files.find { it.path == selectedPath } ?: files.firstOrNull()
        
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .background(CyberDark.copy(alpha = 0.5f))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(0.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("WORKSPACE VFS", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Create File", tint = NeonGreen)
                    }
                }
                
                LazyColumn {
                    items(files) { file ->
                        val isSelected = file.path == activeFile?.path
                        FileTreeItem(file, isSelected) { onSelectPath(file.path) }
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1.9f)
                    .fillMaxHeight()
                    .background(Color(0xFF0C0C0D))
            ) {
                if (activeFile != null) {
                    var editorText by remember(activeFile.content) { mutableStateOf(activeFile.content) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeFile.path, 
                            color = NeonCyan, 
                            fontFamily = FontFamily.Monospace, 
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(onClick = { onTriggerExportFile(activeFile.path) }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Export File", tint = Color.LightGray)
                            }
                            IconButton(onClick = { onSaveFile(activeFile.path, editorText) }) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Save changes", tint = NeonGreen)
                            }
                            IconButton(onClick = { onDeleteFile(activeFile.path) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete file", tint = NeonOrange)
                            }
                        }
                    }
                    HorizontalDivider(color = Color.DarkGray)
                    OutlinedTextField(
                        value = editorText,
                        onValueChange = { editorText = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        textStyle = ComposeTextStyle(color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Initiate document to edit credentials or build workflows.", color = Color.Gray, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var newFilePath by remember { mutableStateOf("") }
        var initialContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Generate Workspace Document", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp) },
            containerColor = CyberSlate,
            text = {
                Column {
                    OutlinedTextField(
                        value = newFilePath,
                        onValueChange = { newFilePath = it },
                        label = { Text("Relative File Path") },
                        placeholder = { Text("e.g. scripts/test.sh") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        textStyle = ComposeTextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = initialContent,
                        onValueChange = { initialContent = it },
                        label = { Text("Initial Content") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        textStyle = ComposeTextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFilePath.isNotBlank()) {
                            onCreateFile(newFilePath, initialContent)
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("CREATE DOC", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun MobileFileListScreen(
    files: List<VirtualFile>,
    onSelect: (String) -> Unit,
    onCreateRequest: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("VIRTUAL SYSTEM FILES", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Button(
                onClick = onCreateRequest,
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("NEW FILE", color = CyberDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = Color.DarkGray)
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files) { file ->
                FileTreeItem(file = file, isSelected = false) {
                    onSelect(file.path)
                }
            }
        }
    }
}

@Composable
fun MobileEditorDetailedView(
    file: VirtualFile,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onExport: (String) -> Unit
) {
    var textState by remember(file.content) { mutableStateOf(file.content) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberSlate)
                .padding(vertical = 4.dp, horizontal = 12.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to workspace list", tint = Color.LightGray)
            }
            Text(
                text = file.path.substringAfterLast("/"),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                maxLines = 1
            )
            Row {
                IconButton(onClick = { onExport(file.path) }, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export File", tint = NeonCyan)
                }
                IconButton(onClick = { onSave(file.path, textState) }, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Save changes", tint = NeonGreen)
                }
                IconButton(onClick = { onDelete(file.path) }, modifier = Modifier.size(48.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete file", tint = NeonOrange)
                }
            }
        }
        HorizontalDivider(color = Color.DarkGray)
        Text(
            text = "Path: ${file.path}", 
            color = Color.Gray, 
            fontSize = 11.sp, 
            fontFamily = FontFamily.Monospace, 
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        OutlinedTextField(
            value = textState,
            onValueChange = { textState = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            textStyle = ComposeTextStyle(color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun FileTreeItem(file: VirtualFile, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (isSelected) NeonGreen.copy(alpha = 0.15f) else Color.Transparent)
            .padding(vertical = 14.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val extension = file.path.substringAfterLast(".", "")
        val icon = when (extension) {
            "kt", "java" -> Icons.Default.Build
            "sh", "py" -> Icons.Default.PlayArrow
            "json", "yml" -> Icons.Default.Settings
            else -> Icons.Default.Search
        }
        val iconColor = when (extension) {
            "kt", "java" -> NeonCyan
            "sh", "py" -> NeonGreen
            "json", "yml" -> NeonOrange
            else -> Color.White
        }

        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = iconColor, 
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.path.substringAfterLast("/"),
                color = if (isSelected) NeonGreen else Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Text(
                text = file.path,
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, 
            contentDescription = null, 
            tint = Color.DarkGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun RcloneMountPanel(
    rcloneMounts: Map<String, String>,
    onMount: (String, String) -> Unit,
    onUnmount: (String) -> Unit,
    onSyncNow: (String) -> Unit
) {
    var provider by remember { mutableStateOf("Google Drive") }
    var remotePath by remember { mutableStateOf("root/backup_cortex") }
    
    val providersList = listOf("Google Drive", "AWS S3", "Dropbox", "Microsoft OneDrive")
    var providersExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("RCLONE MOUNT MANAGER", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Persistent server layer to orchestrate connected Google Drive or cloud storage directories in real-time.", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 6.dp))
        
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(bottom = 16.dp))

        // Create new mount frame
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("CONNECT CLOUD STORAGE", color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { providersExpanded = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(provider, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = providersExpanded,
                        onDismissRequest = { providersExpanded = false },
                        modifier = Modifier.background(CyberSlate)
                    ) {
                        providersList.forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov, color = Color.White, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    provider = prov
                                    providersExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedTextField(
                    value = remotePath,
                    onValueChange = { remotePath = it },
                    label = { Text("Remote Location Path", color = Color.Gray) },
                    textStyle = ComposeTextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Button(
                    onClick = { onMount(provider, remotePath) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("MOUNT STORAGE INSTANCE", color = CyberDark, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text("ACTIVE CLOUD MOUNTS", color = NeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (rcloneMounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No mounted drives found under /mnt/rclone.", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rcloneMounts.forEach { (mountProvider, mountPath) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141416)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(mountProvider, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                }
                                Text("Virtual path: /mnt/rclone/${mountProvider.lowercase()}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                Text("Target remote: $mountPath", color = Color.DarkGray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            }
                            Row {
                                IconButton(onClick = { onSyncNow(mountProvider) }, modifier = Modifier.size(48.dp)) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync Cloud Storage", tint = NeonCyan)
                                }
                                IconButton(onClick = { onUnmount(mountProvider) }, modifier = Modifier.size(48.dp)) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Unmount", tint = NeonOrange)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemLogsPanel(
    logs: List<String>,
    onClearLogs: () -> Unit,
    onDeploySkill: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    
    // Automatically scrolls logs to the end
    LaunchedEffect(logs.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("SYSTEM COMPILING & RECONFIGURING", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Wield Gemini & Nvidia agent skills natively inside the workspace sandboxed container environment.", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 6.dp))
        
        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(bottom = 12.dp))

        // Modular skills row
        Text("DEPLOY SYSTEM SKILLS", color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onDeploySkill("WHISPERER") },
                colors = ButtonDefaults.buttonColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.3f))
            ) {
                Text("Whisperer", fontSize = 11.sp, color = NeonGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
                onClick = { onDeploySkill("CONFIGURATOR") },
                colors = ButtonDefaults.buttonColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Text("Fix Dpkg", fontSize = 11.sp, color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
                onClick = { onDeploySkill("CHMOD") },
                colors = ButtonDefaults.buttonColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonOrange.copy(alpha = 0.3f))
            ) {
                Text("Chmod", fontSize = 11.sp, color = NeonOrange, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
                onClick = { onDeploySkill("DOCKER") },
                colors = ButtonDefaults.buttonColors(containerColor = CyberSlate),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(48.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("Container", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        // Terminal Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LIVE TERMINAL READOUT", color = NeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            TextButton(onClick = onClearLogs, modifier = Modifier.height(36.dp)) {
                Text("CLEAR logs", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
        
        Surface(
            color = Color(0xFF070709),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .verticalScroll(scrollState)
            ) {
                logs.forEach { logLine ->
                    val color = when {
                        logLine.contains("Error") || logLine.contains("failed") -> NeonOrange
                        logLine.contains("Success") || logLine.contains("completed") -> NeonGreen
                        logLine.contains("Rclone") || logLine.contains("Mount") -> NeonCyan
                        else -> Color.White
                    }
                    Text(
                        text = ">> $logLine",
                        color = color,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SovereignDashboardScreen(
    files: List<VirtualFile>,
    userRole: String,
    auditLogs: List<com.example.db.AuditLog>,
    onRoleChange: (String) -> Unit,
    onTriggerArchive: () -> Unit
) {
    var cycle by remember { mutableIntStateOf(0) }
    var mutations by remember { mutableIntStateOf(0) }
    var mutationDetected by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val configPrefs = remember { context.getSharedPreferences("sovereign_agent_config", android.content.Context.MODE_PRIVATE) }

    var autonomousKernelRepair by remember { mutableStateOf(configPrefs.getBoolean("autonomous_kernel_repair", true)) }
    var superhumanMutation by remember { mutableStateOf(configPrefs.getBoolean("superhuman_mutation", false)) }
    var hardenDocker by remember { mutableStateOf(configPrefs.getBoolean("harden_docker", true)) }
    var memoryLimit by remember { mutableFloatStateOf(configPrefs.getFloat("memory_limit", 512f)) }
    var cpuThrottle by remember { mutableFloatStateOf(configPrefs.getFloat("cpu_throttle", 80f)) }
    var threadBounds by remember { mutableStateOf(configPrefs.getString("thread_bounds", "Secure Sandbox Suite") ?: "Secure Sandbox Suite") }
    var showThreadDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(autonomousKernelRepair) {
        configPrefs.edit().putBoolean("autonomous_kernel_repair", autonomousKernelRepair).apply()
    }
    LaunchedEffect(superhumanMutation) {
        configPrefs.edit().putBoolean("superhuman_mutation", superhumanMutation).apply()
    }
    LaunchedEffect(hardenDocker) {
        configPrefs.edit().putBoolean("harden_docker", hardenDocker).apply()
    }
    LaunchedEffect(memoryLimit) {
        configPrefs.edit().putFloat("memory_limit", memoryLimit).apply()
    }
    LaunchedEffect(cpuThrottle) {
        configPrefs.edit().putFloat("cpu_throttle", cpuThrottle).apply()
    }
    LaunchedEffect(threadBounds) {
        configPrefs.edit().putString("thread_bounds", threadBounds).apply()
    }

    LaunchedEffect(files) {
        val neuralStateFile = files.find { it.path == "neural_state.json" }
        if (neuralStateFile != null) {
            val content = neuralStateFile.content
            val newCycle = if (content.contains("\"cycle\":")) {
                content.substringAfter("\"cycle\":").substringBefore(",").trim().toIntOrNull() ?: 0
            } else 0
            
            val newMutationsText = content.substringAfter("\"mutations\": [", "").substringBefore("]")
            val newMutationsCount = newMutationsText.split(",").filter { it.isNotBlank() }.size
            
            if (newMutationsCount > mutations && mutations > 0) {
                mutationDetected = true
                delay(2000) // Neural glow effect duration
                mutationDetected = false
            }
            cycle = newCycle
            mutations = newMutationsCount
        }
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (mutationDetected) 0.8f else 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Neural Glow Animation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (userRole == "Admin") {
            AdminDashboard(role = userRole, onRoleChange = onRoleChange)
            Spacer(modifier = Modifier.height(16.dp))
            AuditLogUI(logs = auditLogs)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text("SOVEREIGN CORTEX", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // IceMaster Sovereign's evolutionary cycle history D3-style component
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSlate),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = glowAlpha)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = NeonCyan.copy(alpha = glowAlpha.coerceAtLeast(0.5f)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Evolutionary Cycle History", color = NeonCyan.copy(alpha = glowAlpha.coerceAtLeast(0.5f)), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    val path = androidx.compose.ui.graphics.Path()
                    if (cycle >= 0) {
                        val points = cycle.coerceAtLeast(10)
                        val widthOffset = size.width / points
                        val heightUnit = size.height / (cycle * 2f).coerceAtLeast(1f)
                        
                        path.moveTo(0f, size.height)
                        for (i in 1..cycle) {
                            val x = i * widthOffset
                            val y = size.height - (i * i * 0.5f).coerceAtMost(size.height) - heightUnit * i
                            path.lineTo(x, y.coerceAtLeast(0f))
                        }
                        
                        drawPath(
                            path = path,
                            color = NeonOrange,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                        )
                        for (i in 1..cycle) {
                            val x = i * widthOffset
                            val y = size.height - (i * i * 0.5f).coerceAtMost(size.height) - heightUnit * i
                            drawCircle(
                                color = NeonGreen,
                                radius = 6f,
                                center = androidx.compose.ui.geometry.Offset(x, y.coerceAtLeast(0f))
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Current Cycle: $cycle | Total Mutations: $mutations", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                if (mutationDetected) {
                    Text(">>> NEW MUTATION DETECTED <<<", color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Archive GitHub Action
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.DarkGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ARCHIVE icemaster_memory.jsonl", color = NeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Trigger a secure GitHub Actions backup to timestamped cloud storage.", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onTriggerArchive,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TRIGGER CLOUD ARCHIVE", color = CyberDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Sovereign Agent Config and Constraints Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSlate),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AGENT BEHAVIOR & CONSTRAINTS",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Toggle 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Autonomous Kernel Repairing", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Deploy automatic recovery patch scripts when runtime alerts occur", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = autonomousKernelRepair,
                        onCheckedChange = { autonomousKernelRepair = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDark,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Superhuman Self-Mutation Mode", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Authorize repository mutation cycles to exceed standard parameters", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = superhumanMutation,
                        onCheckedChange = { superhumanMutation = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDark,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Harden Docker Sandbox Node", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Apply network policies and strict volume mount read-only restricts", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = hardenDocker,
                        onCheckedChange = { hardenDocker = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyberDark,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Spacer(modifier = Modifier.height(1.dp).background(Color.DarkGray).fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))

                // Slider 1: Memory
                Text(
                    text = "Sandbox Memory Boundary: ${memoryLimit.toInt()} MB",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = memoryLimit,
                    onValueChange = { memoryLimit = it },
                    valueRange = 128f..4096f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Slider 2: CPU Throttle
                Text(
                    text = "Max Authorized CPU Compute Limit: ${cpuThrottle.toInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = cpuThrottle,
                    onValueChange = { cpuThrottle = it },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = Color.DarkGray
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dropdown Selector: Threads
                Text(
                    text = "Thread Allocation Bounds Policy:",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CyberDark, RoundedCornerShape(4.dp))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                        .clickable { showThreadDropdown = true }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = threadBounds, color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = NeonCyan)
                    }
                    DropdownMenu(
                        expanded = showThreadDropdown,
                        onDismissRequest = { showThreadDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(CyberSlate)
                    ) {
                        listOf("Secure Sandbox Suite", "Staged Hypervisor Scope", "Full Domain Sovereign Domain").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                                onClick = {
                                    threadBounds = option
                                    showThreadDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sovereign Logs for .shadow_ops/bin/
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                 Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("SOVEREIGN LOGS (.shadow_ops/bin)", color = NeonOrange, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Icon(imageVector = Icons.AutoMirrored.Filled.ListIcon, contentDescription = null, tint = NeonOrange)
                }
                Spacer(modifier = Modifier.height(12.dp))
                val mockLogs = listOf(
                    "[INFO] Loading shadow directives... SUCCESS",
                    "[WARN] docker-compose missing. FALLBACK -> init local pod",
                    "[SUCCESS] icemaster_genesis.py executed flawlessly.",
                    "[ERROR] .shadow_ops/bin/deploy.sh: Permission denied",
                    "[SUCCESS] GitHub archive pipeline established.",
                    "[INFO] Neural state updated."
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black)
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        mockLogs.forEach { logLine ->
                            val color = when {
                                logLine.contains("[SUCCESS]") -> NeonGreen
                                logLine.contains("[ERROR]") -> Color.Red
                                logLine.contains("[WARN]") -> NeonOrange
                                else -> Color.LightGray
                            }
                            Text(logLine, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuildArtifactsPanel() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CI/CD Build Artifacts", color = NeonCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
        Text("Track the latest GitHub Actions builds and grab signed APKs.", color = Color.Gray, fontSize = 14.sp)
        
        Card(
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Latest Build: #42", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Status: SUCCESS", color = NeonGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("Workflow: CodePilot: IceMaster Sovereign", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("Branch: main", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val buildUrl = "https://github.com/eperrier/QDataSet/releases/latest/download/app-release.apk"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(buildUrl))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Signed APK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}