package com.example.zeromile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zeromile.ai.ChatCompletion
import com.example.zeromile.ai.ChatToolExecutor
import com.example.zeromile.ai.ChatTurn
import com.example.zeromile.ai.ChatTurnPart
import com.example.zeromile.ai.GeminiChatClient
import com.example.zeromile.data.model.ChatAction
import com.example.zeromile.data.model.ChatMessage
import com.example.zeromile.data.model.ChatRole
import com.example.zeromile.data.model.ChatUiState
import com.example.zeromile.data.model.VoiceLanguage
import com.example.zeromile.data.repository.CivicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Drives the Zeromile assistant chat: conversation state, Gemini turns, and the tool loop.
 *
 * The ViewModel owns the running transcript twice on purpose:
 *  - [ChatUiState.messages] is the render model (bubbles, actions, timestamps).
 *  - [history] is the Gemini wire transcript, including function call/response turns.
 * Keeping them separate means UI-only concepts never leak into the API payload.
 */
class ChatViewModel(
    private val chatClient: GeminiChatClient = GeminiChatClient(),
    private val toolExecutor: ChatToolExecutor = ChatToolExecutor()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(isConfigured = chatClient.isConfigured))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val history = mutableListOf<ChatTurn>()

    // ---------------------------------------------------------------- input handling

    fun onDraftChange(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    fun setLanguage(language: VoiceLanguage) {
        _uiState.update { it.copy(language = language) }
    }

    fun setListening(isListening: Boolean) {
        _uiState.update { it.copy(isListening = isListening) }
    }

    fun onInterimTranscript(interim: String) {
        _uiState.update { it.copy(interimTranscript = interim) }
    }

    fun onSpeechError(message: String) {
        _uiState.update {
            it.copy(
                isListening = false,
                interimTranscript = "",
                errorMessage = message
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearConversation() {
        history.clear()
        _uiState.update {
            it.copy(messages = emptyList(), draft = "", errorMessage = null, isSending = false)
        }
    }

    // ---------------------------------------------------------------- conversation

    /**
     * Sends [text] (or the current draft) to the assistant.
     *
     * @param userId the active citizen id, used by the complaint-status tools.
     * @param userName optional display name for a warmer greeting.
     */
    fun send(
        text: String = _uiState.value.draft,
        userId: String,
        userName: String? = null
    ) {
        val message = text.trim()
        if (message.isBlank() || _uiState.value.isSending) return

        val userBubble = ChatMessage(role = ChatRole.USER, text = message)
        _uiState.update {
            it.copy(
                messages = it.messages + userBubble,
                draft = "",
                isSending = true,
                errorMessage = null,
                interimTranscript = ""
            )
        }

        if (!chatClient.isConfigured) {
            appendOfflineReply(message, userId)
            return
        }

        history.add(ChatTurn.user(message))
        trimHistory()
        viewModelScope.launch {
            runAssistantTurn(userId = userId, userName = userName)
        }
    }

    /**
     * Keeps the wire transcript bounded. Trimming only from the front can leave an orphaned
     * functionResponse turn (which the API rejects), so any leading tool responses are dropped
     * along with it.
     */
    private fun trimHistory() {
        while (history.size > MAX_HISTORY_TURNS) {
            history.removeAt(0)
        }
        while (history.isNotEmpty() &&
            history.first().parts.any { it is ChatTurnPart.FunctionResponse }
        ) {
            history.removeAt(0)
        }
    }

    private suspend fun runAssistantTurn(userId: String, userName: String?) {
        var assistantText: String? = null
        var pendingAction: ChatAction? = null
        var lastToolName: String? = null
        var failure: String? = null
        var round = 0

        val systemPrompt = buildSystemPrompt(language = _uiState.value.language, userName = userName)

        while (round < MAX_TOOL_ROUNDS) {
            when (val result = chatClient.complete(systemPrompt, history.toList())) {
                is ChatCompletion.Reply -> {
                    assistantText = result.text
                    history.add(ChatTurn.model(result.text))
                    break
                }

                is ChatCompletion.ToolCall -> {
                    history.add(
                        ChatTurn(
                            role = ChatTurn.ROLE_MODEL,
                            parts = listOf(
                                ChatTurnPart.FunctionCall(result.name, result.arguments)
                            )
                        )
                    )

                    val toolResult = toolExecutor.execute(result.name, result.arguments, userId)
                    lastToolName = result.name
                    if (toolResult.forcedAction != null) {
                        pendingAction = toolResult.forcedAction
                    }
                    history.add(ChatTurn.toolResponse(toolResult.name, toolResult.response))
                }

                is ChatCompletion.Failure -> {
                    failure = result.message
                    break
                }
            }
            round++
        }

        val finishedText = assistantText
        if (finishedText != null) {
            appendAssistant(finishedText, action = pendingAction, toolName = lastToolName)
        } else {
            appendError(failure ?: "The assistant could not complete that request. Please try again.")
        }
    }

    private fun appendAssistant(text: String, action: ChatAction?, toolName: String?) {
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(
                    role = ChatRole.ASSISTANT,
                    text = text,
                    action = action,
                    toolName = toolName
                ),
                isSending = false,
                errorMessage = null
            )
        }
    }

    private fun appendError(message: String) {
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(
                    role = ChatRole.ASSISTANT,
                    text = message,
                    isError = true
                ),
                isSending = false
            )
        }
    }

    // ---------------------------------------------------------------- offline mode

    /**
     * When no Gemini key is configured the chat still has to be useful, so complaint-number
     * lookups are answered straight from the repository and everything else gets an honest
     * explanation instead of a silent failure.
     */
    private fun appendOfflineReply(message: String, userId: String) {
        viewModelScope.launch {
            val reference = COMPLAINT_NUMBER_REGEX.find(message)?.value
            if (reference != null) {
                val details = toolExecutor.execute(
                    name = "check_complaint_status",
                    arguments = JSONObject().put("complaintNumber", reference),
                    userId = userId
                )
                val found = details.response.optBoolean("found", false)
                val text = if (found) {
                    offlineStatusSummary(details.response)
                } else {
                    "I couldn't find complaint $reference. Check the number in the Activity tab and try again."
                }
                appendAssistant(text, action = null, toolName = "check_complaint_status")
            } else {
                appendAssistant(
                    text = "The assistant needs a Gemini API key before it can hold a conversation. " +
                        "Complaint lookups still work — send me a complaint number like NMC-2026-004821 " +
                        "and I'll read it back from the ledger. You can also file or track complaints " +
                        "from the AI and Activity tabs meanwhile.",
                    action = null,
                    toolName = null
                )
            }
        }
    }

    private fun offlineStatusSummary(response: JSONObject): String {
        val number = response.optString("complaintNumber", "")
        val status = response.optString("status", "Submitted")
        val service = response.optString("service", "civic service")
        val department = response.optString("department", "Nagpur Municipal Corporation")
        val assignedTeam = response.optString("assignedTeam", "")
        val latestNote = response.optJSONArray("recentStatusHistory")
            ?.let { history ->
                if (history.length() == 0) null
                else history.optJSONObject(history.length() - 1)?.optString("note", "")
            }
            .orEmpty()

        val builder = StringBuilder()
        builder.append("$number is currently $status")
        if (service.isNotBlank()) builder.append(" — $service")
        builder.append(". It is with $department")
        if (assignedTeam.isNotBlank()) builder.append(", assigned to $assignedTeam")
        builder.append(".")
        if (latestNote.isNotBlank()) builder.append(" Latest note: $latestNote")
        return builder.toString()
    }

    // ---------------------------------------------------------------- prompt

    private fun buildSystemPrompt(language: VoiceLanguage, userName: String?): String {
        val address = if (!userName.isNullOrBlank()) " The citizen's name is $userName." else ""
        return """
            You are Zeromile AI, the assistant inside Zeromile Connect, the official civic
            grievance app for Nagpur Municipal Corporation (NMC), Maharashtra.
            $address

            Behaviour rules, in priority order:
            1. Reply in the language of the citizen's latest message (English, Marathi or Hindi).
               Default to ${language.displayName} when it is unclear.
            2. Be short: two to four sentences, plain chat text. No headings, tables or bullet walls.
            3. Call a tool before stating anything about services, departments, complaint status,
               complaint numbers or resolution timelines. Never invent them.
            4. You cannot file, edit or resolve complaints. When the citizen wants to report a
               new issue, gather the location and what happened, then call start_complaint so the
               official form opens pre-filled. Say clearly that the form is where it gets submitted.
            5. Emergencies (fire, medical, crime in progress, gas leak): tell the citizen to use the
               Emergency tab or dial 112 now, before anything else.
            6. If a lookup returns nothing, say so plainly and suggest the next step. Never guess
               a complaint number or a status.
        """.trimIndent()
    }

    companion object {
        /** Tool round-trips allowed per user message before giving up. */
        private const val MAX_TOOL_ROUNDS = 4

        /** Wire turns kept in context; roughly ten exchanges. */
        private const val MAX_HISTORY_TURNS = 20
        private val COMPLAINT_NUMBER_REGEX = Regex("NMC-\\d{4}-\\d{6}", RegexOption.IGNORE_CASE)

        /**
         * Factory used from the Activity so the chat shares the same [CivicRepository]
         * instance (and therefore the same local complaint cache) as the citizen screens.
         */
        fun factory(repository: CivicRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(
                        chatClient = GeminiChatClient(),
                        toolExecutor = ChatToolExecutor(repository)
                    ) as T
                }
            }
    }
}
