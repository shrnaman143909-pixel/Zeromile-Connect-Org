package com.example.zeromile.data.model

import java.util.UUID

/**
 * Conversation roles supported by the Zeromile assistant.
 * The assistant is a citizen-facing chat surface; there is no separate system role
 * because the system prompt is carried out-of-band by the transport layer.
 */
enum class ChatRole {
    USER,
    ASSISTANT
}

/**
 * Inline next-step a chat bubble can offer, e.g. handing the conversation over to the
 * existing voice/text complaint intake flow.
 */
enum class ChatActionKind {
    START_COMPLAINT,
    OPEN_SERVICES,
    OPEN_ACTIVITY
}

data class ChatAction(
    val kind: ChatActionKind,
    val label: String,
    val payload: String = ""
)

/**
 * A single chat bubble.
 *
 * @param toolName set when the bubble was produced after a tool call, used for subtle
 *   "looked up your complaint" style hints and for debugging transcripts.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val action: ChatAction? = null,
    val toolName: String? = null
)

/**
 * Immutable UI state for the assistant chat surface.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val language: VoiceLanguage = VoiceLanguage.ENGLISH,
    val isListening: Boolean = false,
    val interimTranscript: String = "",
    val isConfigured: Boolean = true
) {
    val isEmpty: Boolean get() = messages.isEmpty()
    val canSend: Boolean get() = draft.isNotBlank() && !isSending
}
