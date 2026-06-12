package com.example.ai

import android.util.Base64
import com.example.BuildConfig
import com.example.vfs.VirtualFile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ChatMessage(
    val role: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CodeChange(
    val action: String,
    val path: String,
    val content: String? = null
)

data class AgentCommand(
    val engine: String,
    val script: String
)

class AICollabManager(private val geminiApiService: GeminiApiService) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val fsm = FSMOrchestrator()

    private val systemPrompt = """
        You are an expert FSM-driven Agent. Use ultra-condensed shorthand text protocol. No JSON. No markdown backticks.
        Format (one per line):
        M:[user msg]
        P:[step1]|[step2]
        FC:[action]|[path]|[base64_encoded_code]
        AC:[engine]|[script]
        Available AC engines: termux,cloud_shell,userland,drive_sync,github_sync,vscode_server,android_studio,gcp_deploy,nvidia_tools,cli_internet,vision_web,setup_auth,upload_file,export_workspace,dynamic_skill
    """.trimIndent()

    suspend fun sendMessage(prompt: String, currentFiles: List<VirtualFile>, onApplyChanges: (List<CodeChange>, List<AgentCommand>) -> Unit) {
        if (prompt.isBlank()) return

        fsm.transitionTo(AgentState.ANALYZING)
        _isProcessing.value = true
        _messages.value = _messages.value + ChatMessage("user", prompt)

        try {
            fsm.transitionTo(AgentState.PLANNING)
            val userPromptParts = buildString {
                appendLine("uReq:${prompt}|ctx:${fsm.encodeContext()}")
                appendLine("---vfs---")
                currentFiles.forEach { file ->
                    appendLine("f:${file.path}")
                    appendLine(file.content)
                }
            }

            val request = GenerateContentRequest(
                systemInstruction = Content(listOf(Part(systemPrompt))),
                contents = listOf(Content(listOf(Part(userPromptParts)))),
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(
                        thinkingLevel = "HIGH",
                        thinkingBudget = 2048
                    )
                )
            )

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
                _messages.value = _messages.value + ChatMessage("system", "NO_API_KEY")
                fsm.transitionTo(AgentState.ERROR)
                return
            }

            fsm.transitionTo(AgentState.EXECUTING)
            val response = withContext(Dispatchers.IO) {
                geminiApiService.generateContent("gemini-3.1-pro-preview", apiKey, request)
            }

            val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (textResponse != null) {
                try {
                    val lines = textResponse.lines()
                    var agentMsg = ""
                    val plan = mutableListOf<String>()
                    val changes = mutableListOf<CodeChange>()
                    val agentCmds = mutableListOf<AgentCommand>()

                    for (line in lines) {
                        try {
                            if (line.startsWith("M:")) {
                                agentMsg += line.substring(2) + "\n"
                            } else if (line.startsWith("P:")) {
                                plan.addAll(line.substring(2).split("|").filter { it.isNotBlank() })
                            } else if (line.startsWith("FC:")) {
                                val parts = line.substring(3).split("|")
                                if (parts.size >= 2) {
                                    val action = parts[0]
                                    val path = parts[1]
                                    val contentB64 = parts.getOrNull(2)
                                    val content = if (contentB64 != null && contentB64.isNotBlank()) {
                                        String(Base64.decode(contentB64, Base64.DEFAULT))
                                    } else null
                                    changes.add(CodeChange(action, path, content))
                                }
                            } else if (line.startsWith("AC:")) {
                                val parts = line.substring(3).split("|", limit = 2)
                                if (parts.size == 2) {
                                    agentCmds.add(AgentCommand(parts[0], parts[1]))
                                }
                            }
                        } catch(e: Exception) {}
                    }
                    
                    val displayMessage = buildString {
                        if (agentMsg.isNotBlank()) appendLine(agentMsg)
                        if (plan.isNotEmpty()) {
                            appendLine("[P]:")
                            plan.forEach { p -> appendLine("- $p") }
                        }
                    }

                    if (displayMessage.isNotBlank()) {
                        _messages.value = _messages.value + ChatMessage("agent", displayMessage)
                    }

                    if (changes.isNotEmpty() || agentCmds.isNotEmpty()) {
                        onApplyChanges(changes, agentCmds)
                    }
                    
                    fsm.transitionTo(AgentState.VERIFYING)

                } catch (e: Exception) {
                    _messages.value = _messages.value + ChatMessage("system", "ERR_PARSE")
                    fsm.transitionTo(AgentState.ERROR)
                }
            } else {
                _messages.value = _messages.value + ChatMessage("system", "ERR_EMPTY_RESPONSE")
                fsm.transitionTo(AgentState.ERROR)
            }
            
            if (fsm.currentState.value != AgentState.ERROR) {
                fsm.transitionTo(AgentState.IDLE)
            }

        } catch (e: Exception) {
            _messages.value = _messages.value + ChatMessage("system", "ERR_NET: ${e.message}")
            fsm.transitionTo(AgentState.ERROR)
        } finally {
            _isProcessing.value = false
        }
    }
}
