package com.example.zeromile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.zeromile.data.model.FormStep
import com.example.zeromile.data.model.VoiceInputMode
import com.example.zeromile.data.model.VoiceLanguage
import com.example.zeromile.data.repository.SeedData
import com.example.zeromile.speech.AndroidSpeechManager
import com.example.zeromile.ui.components.AiAnalyzingCard
import com.example.zeromile.ui.components.ButtonVariant
import com.example.zeromile.ui.components.ComplaintFormView
import com.example.zeromile.ui.components.LanguageSelector
import com.example.zeromile.ui.components.MicrophoneButton
import com.example.zeromile.ui.components.RecommendationCard
import com.example.zeromile.ui.components.ServicePickerModal
import com.example.zeromile.ui.components.TranscriptCard
import com.example.zeromile.ui.components.VoiceConfirmationCard
import com.example.zeromile.ui.components.VoiceErrorBanner
import com.example.zeromile.ui.components.VoiceUnsupportedFallback
import com.example.zeromile.ui.components.ZeromileButton
import com.example.zeromile.ui.components.ZeromileCard
import com.example.zeromile.ui.components.ZeromilePageHeader
import com.example.zeromile.ui.components.form.ComplaintReviewView
import com.example.zeromile.ui.components.form.ComplaintSuccessView
import com.example.zeromile.ui.components.form.DynamicFormEngine
import com.example.zeromile.ui.components.form.ReadyForSubmissionCard
import com.example.zeromile.ui.viewmodel.CivicUiState
import com.example.zeromile.ui.viewmodel.CivicViewModel
import com.example.zeromile.ui.viewmodel.NavTab
import com.example.zeromile.ui.viewmodel.UiState

@Composable
fun AiScreen(
    uiState: CivicUiState,
    viewModel: CivicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val speechManager = remember { AndroidSpeechManager(context) }
    val speechState by speechManager.state.collectAsState()
    val scrollState = rememberScrollState()

    // Sync speech manager state to ViewModel
    LaunchedEffect(speechState.isListening) {
        viewModel.setListeningState(speechState.isListening)
    }
    LaunchedEffect(speechState.interimText) {
        if (speechState.interimText.isNotBlank()) {
            viewModel.onInterimSpeech(speechState.interimText)
        }
    }
    LaunchedEffect(speechState.finalText) {
        if (speechState.finalText.isNotBlank()) {
            viewModel.onFinalSpeech(speechState.finalText)
        }
    }
    LaunchedEffect(speechState.errorMessage) {
        speechState.errorMessage?.let { viewModel.onSpeechError(it) }
    }
    LaunchedEffect(speechState.rmsLevel) {
        viewModel.setRmsLevel(speechState.rmsLevel)
    }
    LaunchedEffect(speechState.isSupported) {
        viewModel.setVoiceSupported(speechState.isSupported)
    }

    // Clean up speech manager on leave
    DisposableEffect(speechManager) {
        onDispose {
            speechManager.destroy()
        }
    }

    // Permission launcher for dangerous RECORD_AUDIO permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            speechManager.startListening(uiState.voiceLanguage)
        } else {
            viewModel.onSpeechError("Microphone access is needed for voice input.")
        }
    }

    val requestSpeech = {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (speechState.isListening) {
                speechManager.stopListening()
            } else {
                speechManager.startListening(uiState.voiceLanguage)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val knownServices = (uiState.servicesState as? UiState.Success)?.data ?: SeedData.services

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("ai_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ZeromilePageHeader(
            title = stringResource(R.string.ai_header_title),
            subtitle = stringResource(R.string.ai_voice_explanation),
            testTag = "ai_page_header"
        )

        // Phase 4 Dynamic Form System: Filling -> Reviewing -> Ready for Submission
        if (uiState.dynamicFormConfig != null && uiState.aiRecommendation != null) {
            when (uiState.formStep) {
                FormStep.FILLING -> {
                    DynamicFormEngine(
                        formConfig = uiState.dynamicFormConfig,
                        recommendation = uiState.aiRecommendation,
                        fieldValues = uiState.formFieldValues,
                        formErrors = uiState.formErrors,
                        profile = uiState.profile,
                        onFieldValueChange = { fieldId, value -> viewModel.updateFormField(fieldId, value) },
                        onBackToAnalysis = { viewModel.backToAnalysis() },
                        onReviewComplaint = { viewModel.validateAndReviewForm() },
                        stagedEvidence = uiState.stagedEvidence,
                        onAddStagedEvidence = { viewModel.addStagedEvidence(it) },
                        onRemoveStagedEvidence = { viewModel.removeStagedEvidence(it) },
                        capturedLocation = uiState.capturedLocation,
                        isCapturingLocation = uiState.isCapturingLocation,
                        locationError = uiState.locationError,
                        onCaptureLocation = { viewModel.captureCurrentLocation(context) }
                    )
                }
                FormStep.REVIEWING -> {
                    uiState.complaintReviewData?.let { reviewData ->
                        ComplaintReviewView(
                            reviewData = reviewData,
                            onEditDetails = { viewModel.editComplaintDetails() },
                            onConfirmReadyForSubmission = { viewModel.confirmReadyForSubmission() }
                        )
                    }
                }
                FormStep.READY_FOR_SUBMISSION -> {
                    uiState.complaintReviewData?.let { reviewData ->
                        ReadyForSubmissionCard(
                            reviewData = reviewData,
                            isSubmitting = uiState.isSubmittingComplaint,
                            submissionError = uiState.submissionError,
                            onSubmitComplaint = { viewModel.submitRealComplaint() },
                            onReviewAgain = { viewModel.editComplaintDetails() },
                            onDismissError = { viewModel.dismissSubmissionError() }
                        )
                    }
                }
                FormStep.SUBMITTED -> {
                    uiState.submittedComplaint?.let { complaintRecord ->
                        ComplaintSuccessView(
                            complaint = complaintRecord,
                            onTrackInActivity = {
                                viewModel.openComplaintDetail(complaintRecord.complaintNumber)
                                viewModel.setTab(NavTab.ACTIVITY)
                            },
                            onFileAnotherComplaint = {
                                viewModel.resetFormToNew()
                            }
                        )
                    }
                }
            }
        } else if (uiState.complaintFormDraft != null && uiState.aiRecommendation != null) {
            ComplaintFormView(
                formDraft = uiState.complaintFormDraft,
                recommendation = uiState.aiRecommendation,
                onBackToAnalysis = { viewModel.backToAnalysis() },
                onDraftUpdated = { viewModel.updateComplaintDraft(it) }
            )
        } else if (uiState.isAiAnalyzing) {
            // Analyzing with Gemini
            AiAnalyzingCard(transcript = uiState.voiceTranscript)
        } else if (uiState.aiRecommendation != null) {
            // Show AI Recommendation Card
            RecommendationCard(
                recommendation = uiState.aiRecommendation,
                onConfirm = { viewModel.confirmRecommendation() },
                onChooseDifferentService = { viewModel.setShowServicePicker(true) },
                onRevise = { viewModel.reviseProblem() }
            )
        } else {
            // Voice / Text Input Flow
            // Multilingual Language Selector (English, मराठी, हिंदी)
            LanguageSelector(
                selectedLanguage = uiState.voiceLanguage,
                onLanguageSelected = { lang ->
                    viewModel.setVoiceLanguage(lang)
                    if (speechState.isListening) {
                        speechManager.stopListening()
                        speechManager.startListening(lang)
                    }
                },
                enabled = !speechState.isListening
            )

            // Mode Switcher: Voice vs Type Instead
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (uiState.voiceInputMode == VoiceInputMode.VOICE) Icons.Default.Mic else Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.voiceInputMode == VoiceInputMode.VOICE) "Voice Complaint Mode" else "Text Complaint Mode",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                ZeromileButton(
                    text = if (uiState.voiceInputMode == VoiceInputMode.VOICE) stringResource(R.string.ai_type_instead) else "Use Voice",
                    icon = if (uiState.voiceInputMode == VoiceInputMode.VOICE) Icons.Default.Keyboard else Icons.Default.Mic,
                    onClick = {
                        if (speechState.isListening) {
                            speechManager.stopListening()
                        }
                        val newMode = if (uiState.voiceInputMode == VoiceInputMode.VOICE) VoiceInputMode.TEXT else VoiceInputMode.VOICE
                        viewModel.setVoiceInputMode(newMode)
                    },
                    variant = ButtonVariant.TEXT,
                    testTag = "toggle_input_mode_button"
                )
            }

            // Voice Error Banner (if any)
            uiState.voiceError?.let { error ->
                VoiceErrorBanner(
                    errorMessage = error,
                    onRetry = {
                        viewModel.onSpeechError("")
                        requestSpeech()
                    },
                    onTypeInstead = {
                        viewModel.setVoiceInputMode(VoiceInputMode.TEXT)
                    }
                )
            }

            // Voice Input Unsupported on device
            if (!uiState.isVoiceSupported && uiState.voiceInputMode == VoiceInputMode.VOICE) {
                VoiceUnsupportedFallback(
                    onTypeInstead = {
                        viewModel.setVoiceInputMode(VoiceInputMode.TEXT)
                    }
                )
            }

            if (uiState.voiceInputMode == VoiceInputMode.VOICE && uiState.isVoiceSupported) {
                // Voice Input UI Card
                ZeromileCard(testTag = "voice_input_card") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MicrophoneButton(
                            isListening = speechState.isListening,
                            onClick = requestSpeech,
                            rmsLevel = speechState.rmsLevel,
                            testTag = "ai_microphone_button"
                        )

                        // Multilingual hint
                        Text(
                            text = when (uiState.voiceLanguage) {
                                VoiceLanguage.MARATHI -> "उदा: \"माझ्या परिसरात रोज रात्री मोठ्या आवाजात DJ वाजतो.\""
                                VoiceLanguage.HINDI -> "उदा: \"धरमपेठ में सड़क पर गहरा गड्ढा है, तुरंत मरम्मत कराएं।\""
                                VoiceLanguage.ENGLISH -> "e.g. \"Streetlights are not working on VIP Road since 3 days.\""
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                        )
                    }
                }

                // Live Transcript Card (or editing mode)
                if (speechState.isListening || uiState.voiceTranscript.isNotBlank() || uiState.voiceInterimTranscript.isNotBlank()) {
                    TranscriptCard(
                        transcript = uiState.voiceTranscript,
                        interimTranscript = uiState.voiceInterimTranscript,
                        isListening = speechState.isListening,
                        isEditing = uiState.isEditingTranscript,
                        onStartEditing = { viewModel.toggleEditingTranscript(true) },
                        onSaveEdit = { newText ->
                            viewModel.editTranscript(newText)
                            viewModel.toggleEditingTranscript(false)
                        },
                        onCancelEdit = { viewModel.toggleEditingTranscript(false) },
                        onClear = {
                            speechManager.resetState()
                            viewModel.clearTranscript()
                        },
                        onRecordAgain = {
                            speechManager.resetState()
                            viewModel.recordAgain()
                            requestSpeech()
                        },
                        onContinue = {
                            viewModel.continueWithTranscript()
                        }
                    )
                }

                // Confirmation Card: "Did we get that right?"
                if (uiState.showVoiceConfirmation && uiState.voiceTranscript.isNotBlank() && !speechState.isListening) {
                    VoiceConfirmationCard(
                        transcript = uiState.voiceTranscript,
                        language = uiState.voiceLanguage,
                        onRecordAgain = {
                            speechManager.resetState()
                            viewModel.recordAgain()
                            requestSpeech()
                        },
                        onEdit = { viewModel.toggleEditingTranscript(true) },
                        onContinue = { viewModel.continueWithTranscript() },
                        isConfirmed = uiState.completedVoiceRequest != null
                    )
                }
            } else {
                // Text Input Fallback UI
                ZeromileCard(testTag = "text_input_fallback_card") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(R.string.ai_type_complaint_instead),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Write your civic issue in ${uiState.voiceLanguage.nativeScript}. It will be packaged with your selected language for AI classification.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        OutlinedTextField(
                            value = uiState.voiceTranscript,
                            onValueChange = { viewModel.editTranscript(it) },
                            label = { Text("Describe civic problem...") },
                            placeholder = {
                                Text(
                                    when (uiState.voiceLanguage) {
                                        VoiceLanguage.MARATHI -> "उदा: रस्त्यावर कचरा साचला आहे..."
                                        VoiceLanguage.HINDI -> "उदा: सड़क पर कचरा फैला हुआ है..."
                                        VoiceLanguage.ENGLISH -> "e.g. Broken water pipe near Sitabuldi..."
                                    }
                                )
                            },
                            singleLine = false,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_complaint_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ZeromileButton(
                                text = stringResource(R.string.ai_clear_transcript),
                                icon = Icons.Default.Close,
                                onClick = { viewModel.clearTranscript() },
                                variant = ButtonVariant.TEXT,
                                testTag = "text_clear_button"
                            )

                            ZeromileButton(
                                text = stringResource(R.string.ai_continue),
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                onClick = { viewModel.continueWithTranscript() },
                                enabled = uiState.voiceTranscript.trim().isNotEmpty(),
                                variant = ButtonVariant.PRIMARY,
                                testTag = "text_continue_button"
                            )
                        }
                    }
                }
            }
        }

        // Service Picker Modal (when citizen selects "Different Service")
        if (uiState.showServicePicker) {
            ServicePickerModal(
                services = knownServices,
                onSelectService = { service -> viewModel.chooseManualService(service) },
                onDismissRequest = { viewModel.setShowServicePicker(false) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
