package com.example.zeromile.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.zeromile.data.model.VoiceLanguage

/**
 * Multilingual language selector for English, मराठी, हिंदी.
 * Directly controls speech recognition locale and text preservation.
 */
@Composable
fun LanguageSelector(
    selectedLanguage: VoiceLanguage,
    onLanguageSelected: (VoiceLanguage) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("language_selector_row"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VoiceLanguage.values().forEach { lang ->
            val isSelected = selectedLanguage == lang
            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onLanguageSelected(lang) },
                label = {
                    Text(
                        text = lang.nativeScript,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("lang_chip_${lang.name.lowercase()}")
            )
        }
    }
}

/**
 * Large, accessible microphone button with subtle pulsating listening state.
 */
@Composable
fun MicrophoneButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rmsLevel: Float = 0f,
    testTag: String = "microphone_button"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val contentDesc = if (isListening) {
        stringResource(R.string.ai_tap_to_stop)
    } else {
        stringResource(R.string.ai_tap_mic_to_speak)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .testTag(testTag)
        ) {
            // Subtle ambient outer pulse ring when listening
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                )
                Box(
                    modifier = Modifier
                        .size(98.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                )
            }

            // Central prominent mic button
            Surface(
                shape = CircleShape,
                color = if (isListening) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                shadowElevation = if (isListening) 8.dp else 4.dp,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                    .semantics { contentDescription = contentDesc }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = contentDesc,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Textual listening status
        if (isListening) {
            Text(
                text = stringResource(R.string.ai_listening),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                ),
                modifier = Modifier.testTag("listening_status_text")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ai_tell_zeromile),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        } else {
            Text(
                text = stringResource(R.string.ai_tap_mic_to_speak),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("tap_to_speak_text")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ai_voice_explanation),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Live transcript card showing recognized speech, editing capability,
 * and quick actions: Edit, Clear, Record Again, Continue.
 */
@Composable
fun TranscriptCard(
    transcript: String,
    interimTranscript: String,
    isListening: Boolean,
    isEditing: Boolean,
    onStartEditing: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onClear: () -> Unit,
    onRecordAgain: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editText by remember(transcript) { mutableStateOf(transcript) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("transcript_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "Live Speech Recognition" else "Your Voice Transcript",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                if (!isListening && transcript.isNotBlank() && !isEditing) {
                    ZeromileStatusBadge(text = "Preserved", status = BadgeStatus.ACTIVE)
                }
            }

            if (isEditing) {
                // Inline editing mode
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Edit your complaint text") },
                    singleLine = false,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_transcript_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZeromileButton(
                        text = "Cancel",
                        onClick = onCancelEdit,
                        variant = ButtonVariant.TEXT,
                        testTag = "cancel_edit_button"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ZeromileButton(
                        text = "Save",
                        icon = Icons.Default.Check,
                        onClick = { onSaveEdit(editText) },
                        variant = ButtonVariant.PRIMARY,
                        testTag = "save_edit_button"
                    )
                }
            } else {
                // Display text: either final transcript or interim partial speech
                val displayText = when {
                    transcript.isNotBlank() && interimTranscript.isNotBlank() ->
                        "$transcript $interimTranscript"
                    transcript.isNotBlank() -> transcript
                    interimTranscript.isNotBlank() -> interimTranscript
                    isListening -> "Listening... Please speak now..."
                    else -> "No speech recorded yet."
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(14.dp)
                ) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (transcript.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                            lineHeight = 24.sp,
                            color = if (transcript.isNotBlank() || interimTranscript.isNotBlank()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.testTag("transcript_display_text")
                    )
                }

                // Actions toolbar
                if (!isListening && transcript.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            ZeromileButton(
                                text = stringResource(R.string.ai_edit_transcript),
                                icon = Icons.Default.Edit,
                                onClick = onStartEditing,
                                variant = ButtonVariant.TEXT,
                                testTag = "edit_transcript_button"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            ZeromileButton(
                                text = stringResource(R.string.ai_clear_transcript),
                                icon = Icons.Default.Close,
                                onClick = onClear,
                                variant = ButtonVariant.TEXT,
                                testTag = "clear_transcript_button"
                            )
                        }

                        ZeromileButton(
                            text = stringResource(R.string.ai_record_again),
                            icon = Icons.Default.Refresh,
                            onClick = onRecordAgain,
                            variant = ButtonVariant.OUTLINE,
                            testTag = "record_again_button"
                        )
                    }

                    // Primary Continue button (only enabled if text is non-empty)
                    ZeromileButton(
                        text = stringResource(R.string.ai_continue),
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onContinue,
                        enabled = transcript.trim().isNotEmpty(),
                        variant = ButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "continue_transcript_button"
                    )
                }
            }
        }
    }
}

/**
 * Confirmation card shown after voice recognition completes:
 * "Did we get that right?"
 * Actions: [Record Again] [Edit] [Continue]
 */
@Composable
fun VoiceConfirmationCard(
    transcript: String,
    language: VoiceLanguage,
    onRecordAgain: () -> Unit,
    onEdit: () -> Unit,
    onContinue: () -> Unit,
    isConfirmed: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_confirmation_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ai_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                ZeromileStatusBadge(
                    text = language.nativeScript,
                    status = BadgeStatus.INFO,
                    testTag = "confirmation_lang_badge"
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(14.dp)
            ) {
                Text(
                    text = transcript,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier.testTag("confirmed_transcript_text")
                )
            }

            if (isConfirmed) {
                // Readiness indicator for Phase 3
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Transcript Confirmed",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = stringResource(R.string.ai_speech_ready),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ZeromileButton(
                    text = stringResource(R.string.ai_record_again),
                    icon = Icons.Default.Refresh,
                    onClick = onRecordAgain,
                    variant = ButtonVariant.OUTLINE,
                    modifier = Modifier.weight(1f),
                    testTag = "confirm_record_again_button"
                )
                ZeromileButton(
                    text = stringResource(R.string.ai_edit_transcript),
                    icon = Icons.Default.Edit,
                    onClick = onEdit,
                    variant = ButtonVariant.OUTLINE,
                    modifier = Modifier.weight(1f),
                    testTag = "confirm_edit_button"
                )
            }

            if (!isConfirmed) {
                ZeromileButton(
                    text = stringResource(R.string.ai_continue),
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = onContinue,
                    enabled = transcript.trim().isNotEmpty(),
                    variant = ButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "confirm_continue_button"
                )
            }
        }
    }
}

/**
 * Error state banner for microphone permission, speech timeout, or recognition failure.
 */
@Composable
fun VoiceErrorBanner(
    errorMessage: String,
    onRetry: () -> Unit,
    onTypeInstead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_error_banner")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.testTag("voice_error_text")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                ZeromileButton(
                    text = stringResource(R.string.ai_type_instead),
                    icon = Icons.Default.Keyboard,
                    onClick = onTypeInstead,
                    variant = ButtonVariant.TEXT,
                    testTag = "error_type_instead_button"
                )
                Spacer(modifier = Modifier.width(8.dp))
                ZeromileButton(
                    text = stringResource(R.string.btn_try_again),
                    icon = Icons.Default.Refresh,
                    onClick = onRetry,
                    variant = ButtonVariant.PRIMARY,
                    testTag = "error_try_again_button"
                )
            }
        }
    }
}

/**
 * Fallback shown when device/browser does not support speech recognition.
 */
@Composable
fun VoiceUnsupportedFallback(
    onTypeInstead: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("voice_unsupported_fallback")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MicOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = stringResource(R.string.ai_voice_unsupported),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )
            ZeromileButton(
                text = stringResource(R.string.ai_type_complaint_instead),
                icon = Icons.Default.Keyboard,
                onClick = onTypeInstead,
                variant = ButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                testTag = "unsupported_type_instead_button"
            )
        }
    }
}
