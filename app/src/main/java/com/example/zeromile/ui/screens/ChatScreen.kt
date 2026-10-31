package com.example.zeromile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.zeromile.data.model.ChatAction
import com.example.zeromile.data.model.ChatActionKind
import com.example.zeromile.data.model.ChatMessage
import com.example.zeromile.data.model.ChatRole
import com.example.zeromile.data.model.ChatUiState
import com.example.zeromile.data.model.VoiceLanguage
import com.example.zeromile.speech.AndroidSpeechManager
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.LanguageSelector
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.viewmodel.ChatViewModel

/**
 * Multi-turn civic assistant surface.
 *
 * Layout is intentionally chat-first: a scrollable transcript, quick-start prompts when the
 * conversation is empty, and an input bar that accepts typing or Nagpur's three languages by
 * voice. Everything it can *do* is a hand-off into an existing screen — the chat never writes
 * municipal data itself.
 */
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    userId: String,
    userName: String?,
    onBack: () -> Unit,
    onStartComplaint: (String) -> Unit,
    onOpenServices: () -> Unit,
    onOpenActivity: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by chatViewModel.uiState.collectAsState()
    val speechManager = remember { AndroidSpeechManager(context) }
    val speechState by speechManager.state.collectAsState()
    val listState = rememberLazyListState()
    var showLanguages by remember { mutableStateOf(false) }

    DisposableEffect(speechManager) {
        onDispose { speechManager.destroy() }
    }

    LaunchedEffect(speechState.isListening) {
        chatViewModel.setListening(speechState.isListening)
    }
    LaunchedEffect(speechState.interimText) {
        if (speechState.interimText.isNotBlank()) {
            chatViewModel.onInterimTranscript(speechState.interimText)
        }
    }
    LaunchedEffect(speechState.finalText) {
        val spoken = speechState.finalText.trim()
        if (spoken.isNotBlank()) {
            chatViewModel.onDraftChange(spoken)
            chatViewModel.send(text = spoken, userId = userId, userName = userName)
        }
    }
    LaunchedEffect(speechState.errorMessage) {
        speechState.errorMessage?.let { chatViewModel.onSpeechError(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            speechManager.startListening(uiState.language)
        } else {
            chatViewModel.onSpeechError("Microphone access is needed for voice messages.")
        }
    }

    val toggleListening = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (speechState.isListening) {
            speechManager.stopListening()
        } else {
            speechManager.startListening(uiState.language)
        }
    }

    // Keep the newest bubble in view while the assistant is thinking or answering.
    LaunchedEffect(uiState.messages.size, uiState.isSending) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp)
            .testTag("chat_screen")
    ) {
        ChatHeader(
            isConfigured = uiState.isConfigured,
            canClear = uiState.messages.isNotEmpty(),
            onBack = onBack,
            onClear = chatViewModel::clearConversation,
            onToggleLanguages = { showLanguages = !showLanguages }
        )

        AnimatedVisibility(visible = showLanguages, enter = fadeIn(), exit = fadeOut()) {
            LanguageSelector(
                selectedLanguage = uiState.language,
                onLanguageSelected = { chatViewModel.setLanguage(it) },
                enabled = !uiState.isSending
            )
        }

        if (uiState.isEmpty) {
            ChatEmptyState(
                modifier = Modifier.weight(1f),
                onPromptSelected = { prompt ->
                    chatViewModel.onDraftChange(prompt)
                    chatViewModel.send(text = prompt, userId = userId, userName = userName)
                }
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("chat_transcript"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        onAction = { action ->
                            when (action.kind) {
                                ChatActionKind.START_COMPLAINT -> onStartComplaint(action.payload)
                                ChatActionKind.OPEN_SERVICES -> onOpenServices()
                                ChatActionKind.OPEN_ACTIVITY -> onOpenActivity()
                            }
                        }
                    )
                }

                if (uiState.isSending) {
                    item { TypingBubble() }
                }
            }
        }

        uiState.errorMessage?.let { error ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = chatViewModel::dismissError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        ChatInputBar(
            draft = uiState.draft,
            interimTranscript = uiState.interimTranscript,
            isListening = uiState.isListening,
            canSend = uiState.canSend,
            onDraftChange = chatViewModel::onDraftChange,
            onSend = { chatViewModel.send(userId = userId, userName = userName) },
            onToggleListening = toggleListening
        )
    }
}

@Composable
private fun ChatHeader(
    isConfigured: Boolean,
    canClear: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onToggleLanguages: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Zeromile AI",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (isConfigured) "Ask about services or your complaints" else "Limited mode — complaint lookups only",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onToggleLanguages, modifier = Modifier.testTag("chat_language_button")) {
            Text(text = "EN / मराठी / हिंदी", style = MaterialTheme.typography.labelSmall)
        }

        if (canClear) {
            IconButton(onClick = onClear, modifier = Modifier.testTag("chat_clear_button")) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Start a new chat")
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    modifier: Modifier = Modifier,
    onPromptSelected: (String) -> Unit
) {
    val prompts = listOf(
        "Where is my pothole complaint?",
        "Which department handles garbage in Dharampeth?",
        "I want to report a broken streetlight",
        "आवाज प्रदूषणाची तक्रार कशी करू?"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Namaste. What can I help you with today?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            text = "I can find the right NMC department, check the status of a complaint, or start a new complaint for you. Speak or type in English, मराठी or हिंदी.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        prompts.forEach { prompt ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_prompt_chip")
                    .clickable { onPromptSelected(prompt) }
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onAction: (ChatAction) -> Unit) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isUser) "chat_bubble_user" else "chat_bubble_assistant"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        val bubbleColor = when {
            isUser -> MaterialTheme.colorScheme.primary
            message.isError -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        }
        val textColor = when {
            isUser -> MaterialTheme.colorScheme.onPrimary
            message.isError -> MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.onSurface
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = textColor
                )

                val action = message.action
                if (action != null && !isUser) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ZeromileButton(
                        text = action.label,
                        onClick = { onAction(action) },
                        variant = ButtonVariant.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "chat_action_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_typing_indicator"),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Zeromile AI is checking…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    draft: String,
    interimTranscript: String,
    isListening: Boolean,
    canSend: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleListening: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = if (isListening && interimTranscript.isNotBlank()) interimTranscript else draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .weight(1f)
                .testTag("chat_input_field"),
            placeholder = {
                Text(
                    text = if (isListening) "Listening…" else "Ask about a service or a complaint",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            trailingIcon = {
                IconButton(
                    onClick = onToggleListening,
                    modifier = Modifier.testTag("chat_mic_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Speak instead",
                        tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (canSend) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .testTag("chat_send_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else Color.Gray
            )
        }
    }
}
