package com.example.zeromile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zeromile.ai.GeminiCivicClassifier
import com.example.zeromile.data.form.ServiceFormRegistry
import com.example.zeromile.data.model.CivicAiRecommendation
import com.example.zeromile.data.model.CivicPriority
import com.example.zeromile.data.model.CivicService
import com.example.zeromile.data.model.CivicVoiceRequest
import com.example.zeromile.data.model.ComplaintFormDraft
import com.example.zeromile.data.model.ComplaintReviewData
import com.example.zeromile.data.model.EmergencyService
import com.example.zeromile.data.model.CivicUpdate
import com.example.zeromile.data.model.FieldResponseItem
import com.example.zeromile.data.model.FormFieldType
import com.example.zeromile.data.model.FormStep
import com.example.zeromile.data.model.LiveToastNotification
import com.example.zeromile.data.model.NotificationRecord
import com.example.zeromile.data.model.NotificationType
import com.example.zeromile.data.model.Profile
import com.example.zeromile.data.model.ServiceCategory
import com.example.zeromile.data.model.ServiceFormConfig
import com.example.zeromile.data.model.VoiceInputMode
import com.example.zeromile.data.model.VoiceLanguage
import com.example.zeromile.data.remote.RealtimeConnectionState
import com.example.zeromile.data.remote.RealtimeEvent
import com.example.zeromile.data.repository.CivicRepository
import com.example.zeromile.data.repository.ConnectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class NavTab {
    HOME,
    SERVICES,
    AI,
    ACTIVITY,
    PROFILE,
    EMERGENCY,
    UPDATES
}

data class CivicUiState(
    val currentTab: NavTab = NavTab.HOME,
    val categoriesState: UiState<List<ServiceCategory>> = UiState.Loading,
    val servicesState: UiState<List<CivicService>> = UiState.Loading,
    val selectedCategoryId: String? = null,
    val selectedServiceForModal: CivicService? = null,
    val profile: Profile? = null,
    val connectionStatus: ConnectionStatus? = null,
    val showAiDialog: Boolean = false,
    val userIssueDraft: String = "",
    // Phase 2 Voice Assistant State
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ENGLISH,
    val voiceInputMode: VoiceInputMode = VoiceInputMode.VOICE,
    val voiceTranscript: String = "",
    val voiceInterimTranscript: String = "",
    val isListening: Boolean = false,
    val isVoiceSupported: Boolean = true,
    val voiceError: String? = null,
    val isEditingTranscript: Boolean = false,
    val showVoiceConfirmation: Boolean = false,
    val completedVoiceRequest: CivicVoiceRequest? = null,
    val rmsLevel: Float = 0f,
    // Phase 3 AI Recommendation & Form Draft State
    val isAiAnalyzing: Boolean = false,
    val aiRecommendation: CivicAiRecommendation? = null,
    val complaintFormDraft: ComplaintFormDraft? = null,
    val showServicePicker: Boolean = false,
    // Phase 4 Dynamic Form System State
    val dynamicFormConfig: ServiceFormConfig? = null,
    val formFieldValues: Map<String, String> = emptyMap(),
    val formErrors: Map<String, String> = emptyMap(),
    val formStep: FormStep = FormStep.FILLING,
    val complaintReviewData: ComplaintReviewData? = null,
    // Phase 5 Real Complaint Submission & Persistence
    val isSubmittingComplaint: Boolean = false,
    val submittedComplaint: com.example.zeromile.data.model.ComplaintRecord? = null,
    val submissionError: String? = null,
    val userComplaintsState: UiState<List<com.example.zeromile.data.model.ComplaintRecord>> = UiState.Loading,
    // Phase 6 Citizen Complaint Tracking State
    val selectedComplaintId: String? = null,
    val complaintTrackingState: UiState<com.example.zeromile.data.model.ComplaintTrackingDetails> = UiState.Loading,
    // Phase 8 Realtime Citizen Synchronization & Notifications
    val notificationsState: UiState<List<NotificationRecord>> = UiState.Loading,
    val unreadNotificationsCount: Int = 0,
    val realtimeConnectionState: RealtimeConnectionState = RealtimeConnectionState.DISCONNECTED,
    val activeLiveToast: LiveToastNotification? = null,
    val showNotificationsSheet: Boolean = false,
    // Phase 9 Photo Evidence & Location Capture State
    val stagedEvidence: List<com.example.zeromile.data.model.StagedEvidenceItem> = emptyList(),
    val isUploadingEvidence: Boolean = false,
    val capturedLocation: com.example.zeromile.data.model.ComplaintLocationData? = null,
    val isCapturingLocation: Boolean = false,
    val locationError: String? = null,
    // Phase 10 Emergency Services, Service Discovery & Civic Updates State
    val emergencyServicesState: UiState<List<EmergencyService>> = UiState.Loading,
    val civicUpdatesState: UiState<List<CivicUpdate>> = UiState.Loading,
    val selectedWardFilterForUpdates: String? = null,
    val serviceSearchQuery: String = "",
    val activeServiceDetail: CivicService? = null,
    val activeUpdateDetail: CivicUpdate? = null
)

class CivicViewModel(
    private val repository: CivicRepository = CivicRepository(),
    private val classifier: GeminiCivicClassifier = GeminiCivicClassifier()
) : ViewModel() {

    private val activeCitizenUserId: String = "citizen-" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)

    val currentUserId: String
        get() = _uiState.value.profile?.userId ?: activeCitizenUserId

    private val _uiState = MutableStateFlow(CivicUiState())
    val uiState: StateFlow<CivicUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeRealtimeNotifications()
    }

    fun setTab(tab: NavTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    // Phase 2 Voice Assistant Methods
    fun setVoiceLanguage(language: VoiceLanguage) {
        _uiState.update { it.copy(voiceLanguage = language) }
    }

    fun setVoiceInputMode(mode: VoiceInputMode) {
        _uiState.update { it.copy(voiceInputMode = mode, voiceError = null) }
    }

    fun setListeningState(isListening: Boolean) {
        _uiState.update { it.copy(isListening = isListening) }
    }

    fun setRmsLevel(level: Float) {
        _uiState.update { it.copy(rmsLevel = level) }
    }

    fun onInterimSpeech(interim: String) {
        _uiState.update { it.copy(voiceInterimTranscript = interim) }
    }

    fun onFinalSpeech(finalText: String) {
        val trimmed = finalText.trim()
        if (trimmed.isNotBlank()) {
            _uiState.update { prev ->
                val combined = if (prev.voiceTranscript.isBlank()) {
                    trimmed
                } else {
                    "${prev.voiceTranscript} $trimmed"
                }
                prev.copy(
                    voiceTranscript = combined,
                    voiceInterimTranscript = "",
                    isListening = false,
                    voiceError = null,
                    showVoiceConfirmation = true
                )
            }
        } else {
            _uiState.update { 
                it.copy(
                    isListening = false, 
                    voiceInterimTranscript = "",
                    voiceError = if (it.voiceTranscript.isBlank()) "We didn't hear anything. Try speaking again." else null
                ) 
            }
        }
    }

    fun onSpeechError(error: String) {
        _uiState.update { 
            it.copy(
                isListening = false,
                voiceInterimTranscript = "",
                voiceError = error
            ) 
        }
    }

    fun setVoiceSupported(supported: Boolean) {
        _uiState.update { it.copy(isVoiceSupported = supported) }
    }

    fun editTranscript(newText: String) {
        _uiState.update { it.copy(voiceTranscript = newText) }
    }

    fun clearTranscript() {
        _uiState.update { 
            it.copy(
                voiceTranscript = "",
                voiceInterimTranscript = "",
                showVoiceConfirmation = false,
                voiceError = null
            ) 
        }
    }

    fun recordAgain() {
        _uiState.update { 
            it.copy(
                voiceTranscript = "",
                voiceInterimTranscript = "",
                showVoiceConfirmation = false,
                voiceError = null,
                voiceInputMode = VoiceInputMode.VOICE
            ) 
        }
    }

    fun toggleEditingTranscript(isEditing: Boolean) {
        _uiState.update { it.copy(isEditingTranscript = isEditing) }
    }

    fun continueWithTranscript(): Boolean {
        val currentTranscript = _uiState.value.voiceTranscript.trim()
        if (currentTranscript.isBlank()) {
            return false
        }
        val request = CivicVoiceRequest(
            inputMode = _uiState.value.voiceInputMode,
            language = _uiState.value.voiceLanguage,
            transcript = currentTranscript
        )
        _uiState.update { 
            it.copy(
                completedVoiceRequest = request,
                showVoiceConfirmation = true,
                isEditingTranscript = false
            ) 
        }
        analyzeWithGemini()
        return true
    }

    // Phase 3 AI Classification and Service Routing
    fun analyzeWithGemini() {
        val currentTranscript = _uiState.value.voiceTranscript.trim()
        if (currentTranscript.isBlank()) return

        val lang = _uiState.value.voiceLanguage
        _uiState.update { 
            it.copy(
                isAiAnalyzing = true, 
                aiRecommendation = null,
                complaintFormDraft = null
            ) 
        }

        viewModelScope.launch {
            val knownServices = (_uiState.value.servicesState as? UiState.Success)?.data 
                ?: com.example.zeromile.data.repository.SeedData.services

            val recommendation = classifier.classifyComplaint(
                transcript = currentTranscript,
                language = lang,
                knownServices = knownServices
            )

            _uiState.update { 
                it.copy(
                    isAiAnalyzing = false,
                    aiRecommendation = recommendation
                ) 
            }
        }
    }

    fun confirmRecommendation() {
        val recommendation = _uiState.value.aiRecommendation ?: return
        val transcript = _uiState.value.voiceTranscript
        val lang = _uiState.value.voiceLanguage
        val profile = _uiState.value.profile

        val priorityEnum = when (recommendation.priority.uppercase()) {
            "EMERGENCY" -> CivicPriority.EMERGENCY
            "URGENT" -> CivicPriority.URGENT
            "HIGH" -> CivicPriority.HIGH
            "LOW" -> CivicPriority.LOW
            else -> CivicPriority.MEDIUM
        }

        // Phase 4: Dynamic Form Engine Configuration
        val config = ServiceFormRegistry.getFormConfig(recommendation)
        
        // Initialize dynamic field values with intelligent pre-filling
        val initialValues = mutableMapOf<String, String>()
        config.fields.forEach { field ->
            val prefilled = when (field.prefillSource) {
                "location" -> recommendation.locationMentioned ?: "Dharampeth, Nagpur"
                "summary" -> recommendation.summary ?: ""
                "phone" -> profile?.phone ?: "+91 98230 12345"
                "ward" -> "Ward 32, Dharampeth"
                "transcript" -> transcript
                else -> field.defaultValue
            }
            initialValues[field.id] = prefilled.ifBlank { field.defaultValue }
        }

        val draft = ComplaintFormDraft(
            serviceName = config.serviceName,
            categoryName = config.categoryName,
            departmentName = config.departmentName,
            ward = "Ward 32, Dharampeth",
            citizenTranscript = transcript,
            language = lang,
            priority = priorityEnum,
            locationLandmark = initialValues["location"] ?: "Dharampeth, Nagpur",
            citizenPhone = profile?.phone ?: "+91 98230 12345",
            citizenName = profile?.fullName ?: "Rajesh Sharma",
            incidentTime = initialValues["time_period"] ?: "Today"
        )

        _uiState.update { 
            it.copy(
                complaintFormDraft = draft,
                dynamicFormConfig = config,
                formFieldValues = initialValues,
                formErrors = emptyMap(),
                formStep = FormStep.FILLING,
                complaintReviewData = null
            ) 
        }
    }

    fun chooseManualService(service: CivicService) {
        val currentRec = _uiState.value.aiRecommendation
        val transcript = _uiState.value.voiceTranscript
        val extractedLocation = if (transcript.isNotBlank()) {
            classifier.classifyLocally(transcript, _uiState.value.voiceLanguage).locationMentioned
        } else null

        val updated = currentRec?.copy(
            serviceName = service.name,
            categoryName = service.category?.name ?: currentRec.categoryName,
            departmentName = service.department?.name ?: currentRec.departmentName,
            matchedServiceId = service.id
        ) ?: CivicAiRecommendation(
            categoryName = service.category?.name ?: "General",
            serviceName = service.name,
            departmentName = service.department?.name ?: "Nagpur Municipal Corporation",
            priority = "HIGH",
            confidence = 1.0,
            locationMentioned = extractedLocation,
            explanation = "Manually selected service by citizen.",
            matchedServiceId = service.id
        )

        // Also update dynamicFormConfig if active
        val newConfig = ServiceFormRegistry.getFormConfig(updated)
        val updatedValues = mutableMapOf<String, String>()
        newConfig.fields.forEach { field ->
            val prefilled = when (field.prefillSource) {
                "location" -> updated.locationMentioned ?: "Dharampeth, Nagpur"
                "summary" -> updated.summary ?: ""
                else -> field.defaultValue
            }
            updatedValues[field.id] = prefilled.ifBlank { field.defaultValue }
        }

        _uiState.update { 
            it.copy(
                aiRecommendation = updated,
                showServicePicker = false,
                dynamicFormConfig = if (it.dynamicFormConfig != null) newConfig else null,
                formFieldValues = if (it.dynamicFormConfig != null) updatedValues else it.formFieldValues,
                formErrors = emptyMap()
            ) 
        }

        // If draft exists, also update draft
        _uiState.value.complaintFormDraft?.let { draft ->
            _uiState.update { 
                it.copy(
                    complaintFormDraft = draft.copy(
                        serviceName = service.name,
                        categoryName = service.category?.name ?: draft.categoryName,
                        departmentName = service.department?.name ?: draft.departmentName
                    )
                ) 
            }
        }
    }

    fun updateFormField(fieldId: String, value: String) {
        val currentValues = _uiState.value.formFieldValues.toMutableMap()
        currentValues[fieldId] = value
        
        // Remove error for this field if populated
        val currentErrors = _uiState.value.formErrors.toMutableMap()
        if (value.isNotBlank()) {
            currentErrors.remove(fieldId)
        }

        _uiState.update { 
            it.copy(
                formFieldValues = currentValues,
                formErrors = currentErrors
            ) 
        }
    }

    fun validateAndReviewForm(): Boolean {
        val config = _uiState.value.dynamicFormConfig ?: return false
        val values = _uiState.value.formFieldValues
        val errors = mutableMapOf<String, String>()

        config.fields.forEach { field ->
            if (field.isRequired) {
                if (field.type == FormFieldType.EVIDENCE_ATTACHMENT && _uiState.value.stagedEvidence.isNotEmpty()) {
                    // Staged evidence photos satisfy required evidence attachment
                } else {
                    val value = values[field.id]?.trim() ?: ""
                    if (value.isBlank() || value == "none") {
                        errors[field.id] = "This field is required"
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(formErrors = errors) }
            return false
        }

        // Construct Review Data
        val rec = _uiState.value.aiRecommendation
        val profile = _uiState.value.profile
        val transcript = _uiState.value.voiceTranscript
        val lang = _uiState.value.voiceLanguage

        val priorityEnum = when (rec?.priority?.uppercase()) {
            "EMERGENCY" -> CivicPriority.EMERGENCY
            "URGENT" -> CivicPriority.URGENT
            "HIGH" -> CivicPriority.HIGH
            "LOW" -> CivicPriority.LOW
            else -> CivicPriority.MEDIUM
        }

        val fieldResponses = config.fields.map { field ->
            val rawValue = values[field.id] ?: field.defaultValue
            val formatted = when (field.type) {
                FormFieldType.CHECKBOX -> if (rawValue.toBooleanStrictOrNull() == true) "Yes (Confirmed)" else "No"
                FormFieldType.EVIDENCE_ATTACHMENT -> if (rawValue.isNotBlank() && rawValue != "none") rawValue else "None Attached"
                else -> rawValue
            }
            FieldResponseItem(
                fieldId = field.id,
                label = field.label,
                value = formatted,
                isHighlighted = field.id == "location" || field.type == FormFieldType.CHIPS
            )
        }

        val evidenceField = values["evidence"] ?: values["photo_evidence"]
        val stagedList = _uiState.value.stagedEvidence
        val hasEvidence = stagedList.isNotEmpty() || (evidenceField != null && evidenceField.isNotBlank() && evidenceField != "none")
        val evidenceLabel = if (stagedList.isNotEmpty()) "${stagedList.size} photo(s) attached" else evidenceField

        val capturedLoc = _uiState.value.capturedLocation
        val lat = capturedLoc?.latitude ?: 21.1436
        val lng = capturedLoc?.longitude ?: 79.0688
        val accuracy = capturedLoc?.accuracyMeters
        val source = capturedLoc?.source ?: "manual"

        // Generate realistic reference ID
        val randomSuffix = (1000..9999).random()
        val refId = "NGP-2026-DP-$randomSuffix"

        val review = ComplaintReviewData(
            referenceId = refId,
            serviceId = config.serviceId,
            serviceName = config.serviceName,
            categoryId = rec?.categoryName?.let { cat ->
                com.example.zeromile.data.repository.SeedData.categories.find { it.name.equals(cat, ignoreCase = true) }?.id
            },
            categoryName = config.categoryName,
            departmentId = rec?.departmentName?.let { dept ->
                com.example.zeromile.data.repository.SeedData.departments.find { it.name.equals(dept, ignoreCase = true) }?.id
            },
            departmentName = config.departmentName,
            priority = priorityEnum,
            ward = capturedLoc?.wardName ?: "Ward 32, Dharampeth",
            wardId = "w1111111-1111-1111-1111-111111111111",
            citizenName = profile?.fullName ?: "Rajesh Sharma",
            citizenPhone = profile?.phone ?: "+91 98230 12345",
            originalTranscript = transcript,
            inputMode = rec?.inputMode ?: "voice",
            language = lang,
            summary = rec?.summary ?: transcript.take(60),
            fieldResponses = fieldResponses,
            hasEvidenceAttached = hasEvidence,
            evidenceType = if (hasEvidence) evidenceLabel else null,
            attachedEvidence = stagedList,
            latitude = lat,
            longitude = lng,
            locationAccuracyMeters = accuracy,
            locationSource = source
        )

        _uiState.update { 
            it.copy(
                formErrors = emptyMap(),
                complaintReviewData = review,
                formStep = FormStep.REVIEWING
            ) 
        }
        return true
    }

    fun editComplaintDetails() {
        _uiState.update { it.copy(formStep = FormStep.FILLING) }
    }

    fun confirmReadyForSubmission() {
        _uiState.update { it.copy(formStep = FormStep.READY_FOR_SUBMISSION) }
    }

    // Phase 9: Photo Evidence Staging & Management
    fun addStagedEvidence(item: com.example.zeromile.data.model.StagedEvidenceItem) {
        _uiState.update { prev ->
            if (prev.stagedEvidence.size >= 5) prev
            else prev.copy(stagedEvidence = prev.stagedEvidence + item)
        }
    }

    fun removeStagedEvidence(id: String) {
        _uiState.update { prev ->
            prev.copy(stagedEvidence = prev.stagedEvidence.filterNot { it.id == id })
        }
    }

    fun clearStagedEvidence() {
        _uiState.update { it.copy(stagedEvidence = emptyList()) }
    }

    // Phase 9: GPS & Ward Location Capture
    fun captureCurrentLocation(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCapturingLocation = true, locationError = null) }
            val result = com.example.zeromile.data.location.LocationHelper.getCurrentLocation(context)
            result.onSuccess { locData ->
                _uiState.update { prev ->
                    val updatedFields = prev.formFieldValues.toMutableMap()
                    // Auto populate landmark or location field
                    prev.dynamicFormConfig?.fields?.find { 
                        it.type == FormFieldType.LOCATION_LANDMARK || it.id.contains("location", ignoreCase = true) 
                    }?.let { locField ->
                        updatedFields[locField.id] = locData.locationText
                    }
                    prev.copy(
                        capturedLocation = locData,
                        isCapturingLocation = false,
                        formFieldValues = updatedFields,
                        locationError = null
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isCapturingLocation = false, locationError = err.message) }
            }
        }
    }

    fun setManualLocation(locationText: String) {
        _uiState.update { prev ->
            val ward = com.example.zeromile.data.location.LocationHelper.inferNagpurWard(locationText)
            val updated = com.example.zeromile.data.model.ComplaintLocationData(
                latitude = ward?.lat ?: 21.1436,
                longitude = ward?.lng ?: 79.0688,
                accuracyMeters = null,
                source = "manual",
                locationText = locationText,
                wardName = ward?.wardName,
                wardNumber = ward?.wardNumber
            )
            prev.copy(capturedLocation = updated)
        }
    }

    fun uploadAdditionalEvidence(complaintId: String, fileName: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingEvidence = true) }
            val userId = currentUserId
            repository.uploadEvidence(
                complaintId = complaintId,
                userId = userId,
                fileName = fileName,
                mimeType = mimeType,
                bytes = bytes
            )
            _uiState.update { it.copy(isUploadingEvidence = false) }
            // Reload complaint details with updated evidence
            loadComplaintTracking(complaintId)
        }
    }

    fun deleteEvidence(complaintId: String, evidenceId: String, storagePath: String) {
        viewModelScope.launch {
            repository.deleteEvidence(evidenceId, storagePath)
            loadComplaintTracking(complaintId)
        }
    }

    /**
     * Phase 5 & 9 Real Complaint Submission to Supabase.
     * Prevents duplicate clicks, constructs database-validated CreateComplaintPayload
     * including GPS coordinates and location metadata, submits via CivicRepository,
     * uploads any staged photo evidence, and navigates to the official Success Screen.
     */
    fun submitRealComplaint() {
        val currentState = _uiState.value
        val reviewData = currentState.complaintReviewData ?: return
        if (currentState.isSubmittingComplaint) return // Prevent double submission

        _uiState.update { it.copy(isSubmittingComplaint = true, submissionError = null) }

        viewModelScope.launch {
            // Find location from review fields or fallback
            val locationText = reviewData.fieldResponses.find { 
                it.fieldId.contains("location", ignoreCase = true) || it.label.contains("Location", ignoreCase = true) || it.label.contains("Road", ignoreCase = true)
            }?.value?.ifBlank { "Ward 32, Dharampeth, Nagpur" } ?: (currentState.capturedLocation?.locationText ?: "Ward 32, Dharampeth, Nagpur")

            // Construct detailed description combining structured responses
            val formattedDescription = buildString {
                appendLine(reviewData.summary)
                appendLine()
                appendLine("--- Form Details ---")
                reviewData.fieldResponses.forEach { item ->
                    appendLine("${item.label}: ${item.value}")
                }
                if (reviewData.hasEvidenceAttached) {
                    appendLine("Evidence: ${reviewData.evidenceType ?: "Attached"}")
                }
            }

            val payload = com.example.zeromile.data.model.CreateComplaintPayload(
                userId = currentUserId,
                serviceId = reviewData.serviceId,
                categoryId = reviewData.categoryId,
                departmentId = reviewData.departmentId,
                description = formattedDescription.trim(),
                originalTranscript = reviewData.originalTranscript.ifBlank { reviewData.summary },
                inputMode = reviewData.inputMode,
                language = reviewData.language.localeCode,
                locationText = locationText,
                wardId = reviewData.wardId ?: "w1111111-1111-1111-1111-111111111111",
                latitude = reviewData.latitude,
                longitude = reviewData.longitude,
                locationAccuracyMeters = reviewData.locationAccuracyMeters,
                locationSource = reviewData.locationSource,
                priority = when (reviewData.priority) {
                    com.example.zeromile.data.model.CivicPriority.EMERGENCY -> "Emergency"
                    com.example.zeromile.data.model.CivicPriority.URGENT -> "Urgent"
                    com.example.zeromile.data.model.CivicPriority.HIGH -> "High"
                    com.example.zeromile.data.model.CivicPriority.LOW -> "Low"
                    else -> "Medium"
                },
                aiConfidence = currentState.aiRecommendation?.confidence ?: 0.90,
                aiSummary = currentState.aiRecommendation?.summary ?: reviewData.summary,
                aiReason = currentState.aiRecommendation?.explanation ?: "Verified by Nagpur citizen via structured intake.",
                status = "Submitted"
            )

            val result = repository.submitComplaint(payload)
            result.onSuccess { complaintRecord ->
                // Phase 9: Upload any staged evidence photos to Supabase Storage
                val staged = currentState.stagedEvidence
                if (staged.isNotEmpty()) {
                    val userId = currentUserId
                    repository.uploadStagedEvidenceList(
                        complaintId = complaintRecord.id,
                        userId = userId,
                        stagedItems = staged
                    )
                }

                _uiState.update { 
                    it.copy(
                        isSubmittingComplaint = false,
                        submittedComplaint = complaintRecord,
                        formStep = FormStep.SUBMITTED,
                        submissionError = null,
                        stagedEvidence = emptyList() // clear staged items after upload
                    ) 
                }
                // Refresh Activity complaints in background
                loadComplaints()
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        isSubmittingComplaint = false,
                        submissionError = error.localizedMessage ?: "Failed to submit grievance to Municipal Corporation. Please try again."
                    ) 
                }
            }
        }
    }

    fun dismissSubmissionError() {
        _uiState.update { it.copy(submissionError = null) }
    }

    fun resetFormToNew() {
        _uiState.update { 
            it.copy(
                aiRecommendation = null,
                complaintFormDraft = null,
                dynamicFormConfig = null,
                formFieldValues = emptyMap(),
                formErrors = emptyMap(),
                formStep = FormStep.FILLING,
                complaintReviewData = null,
                isSubmittingComplaint = false,
                submittedComplaint = null,
                submissionError = null,
                voiceTranscript = "",
                voiceInterimTranscript = "",
                showVoiceConfirmation = false,
                completedVoiceRequest = null,
                stagedEvidence = emptyList(),
                capturedLocation = null,
                locationError = null
            ) 
        }
    }

    fun updateComplaintDraft(draft: ComplaintFormDraft) {
        _uiState.update { it.copy(complaintFormDraft = draft) }
    }

    fun backToAnalysis() {
        _uiState.update { 
            it.copy(
                complaintFormDraft = null,
                dynamicFormConfig = null,
                complaintReviewData = null,
                formStep = FormStep.FILLING
            ) 
        }
    }

    fun reviseProblem() {
        _uiState.update { 
            it.copy(
                aiRecommendation = null,
                complaintFormDraft = null,
                dynamicFormConfig = null,
                complaintReviewData = null,
                formStep = FormStep.FILLING,
                showVoiceConfirmation = false,
                completedVoiceRequest = null
            ) 
        }
    }

    fun setShowServicePicker(show: Boolean) {
        _uiState.update { it.copy(showServicePicker = show) }
    }

    fun openVoiceAssistant(initialPrompt: String = "") {
        _uiState.update { 
            it.copy(
                currentTab = NavTab.AI,
                voiceTranscript = initialPrompt,
                voiceInterimTranscript = "",
                showVoiceConfirmation = initialPrompt.isNotBlank(),
                voiceError = null,
                showAiDialog = false,
                aiRecommendation = null,
                complaintFormDraft = null
            ) 
        }
        if (initialPrompt.isNotBlank()) {
            analyzeWithGemini()
        }
    }

    fun loadInitialData() {
        _uiState.update { 
            it.copy(
                categoriesState = UiState.Loading,
                servicesState = UiState.Loading,
                userComplaintsState = UiState.Loading,
                notificationsState = UiState.Loading,
                emergencyServicesState = UiState.Loading,
                civicUpdatesState = UiState.Loading,
                connectionStatus = repository.getConnectionStatus()
            ) 
        }

        viewModelScope.launch {
            loadCategories()
            loadServices()
            loadProfile()
            loadComplaints()
            loadNotifications()
            loadEmergencyServicesInternal()
            loadCivicUpdatesInternal(_uiState.value.selectedWardFilterForUpdates)
        }
    }

    fun loadComplaints() {
        viewModelScope.launch {
            val result = repository.getComplaints(currentUserId)
            result.onSuccess { complaints ->
                _uiState.update { 
                    it.copy(
                        userComplaintsState = UiState.Success(complaints),
                        connectionStatus = repository.getConnectionStatus()
                    ) 
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        userComplaintsState = UiState.Error(error.localizedMessage ?: "Failed to load complaints"),
                        connectionStatus = repository.getConnectionStatus()
                    ) 
                }
            }
        }
    }

    /**
     * Open complaint details and initiate tracking fetch for status history & official updates.
     */
    fun openComplaintDetail(complaintIdOrNumber: String) {
        _uiState.update { 
            it.copy(
                selectedComplaintId = complaintIdOrNumber,
                complaintTrackingState = UiState.Loading
            ) 
        }
        loadComplaintTracking(complaintIdOrNumber)
    }

    /**
     * Open complaint tracking and switch current tab to ACTIVITY (used by notifications & live toasts).
     */
    fun openComplaintTracking(complaintIdOrNumber: String) {
        _uiState.update {
            it.copy(
                currentTab = NavTab.ACTIVITY,
                selectedComplaintId = complaintIdOrNumber,
                complaintTrackingState = UiState.Loading
            )
        }
        loadComplaintTracking(complaintIdOrNumber)
    }

    fun closeComplaintDetail() {
        _uiState.update { 
            it.copy(
                selectedComplaintId = null,
                complaintTrackingState = UiState.Loading
            ) 
        }
    }

    fun refreshComplaintDetail() {
        val currentId = _uiState.value.selectedComplaintId
        if (currentId != null) {
            _uiState.update { it.copy(complaintTrackingState = UiState.Loading) }
            loadComplaintTracking(currentId)
        }
    }

    private fun loadComplaintTracking(complaintIdOrNumber: String) {
        viewModelScope.launch {
            val result = repository.getComplaintTrackingDetails(complaintIdOrNumber)
            result.onSuccess { details ->
                if (details != null) {
                    _uiState.update { 
                        it.copy(
                            complaintTrackingState = UiState.Success(details),
                            connectionStatus = repository.getConnectionStatus()
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            complaintTrackingState = UiState.Error("Complaint not found."),
                            connectionStatus = repository.getConnectionStatus()
                        ) 
                    }
                }
            }.onFailure { error ->
                _uiState.update { 
                    it.copy(
                        complaintTrackingState = UiState.Error(error.localizedMessage ?: "We couldn't load this complaint."),
                        connectionStatus = repository.getConnectionStatus()
                    ) 
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId, servicesState = UiState.Loading) }
        viewModelScope.launch {
            loadServices(categoryId)
        }
    }

    fun openServiceDetail(service: CivicService?) {
        _uiState.update { it.copy(selectedServiceForModal = service) }
    }

    fun openAiModal(initialPrompt: String = "") {
        _uiState.update { it.copy(showAiDialog = true, userIssueDraft = initialPrompt) }
    }

    fun closeAiModal() {
        _uiState.update { it.copy(showAiDialog = false) }
    }

    fun updateIssueDraft(text: String) {
        _uiState.update { it.copy(userIssueDraft = text) }
    }

    fun retryLoading() {
        loadInitialData()
    }

    private suspend fun loadCategories() {
        val result = repository.getServiceCategories()
        result.onSuccess { categories ->
            _uiState.update { 
                it.copy(
                    categoriesState = UiState.Success(categories),
                    connectionStatus = repository.getConnectionStatus()
                ) 
            }
        }.onFailure { error ->
            _uiState.update { 
                it.copy(
                    categoriesState = UiState.Error(error.localizedMessage ?: "Failed to load service categories"),
                    connectionStatus = repository.getConnectionStatus()
                ) 
            }
        }
    }

    private suspend fun loadServices(categoryId: String? = null) {
        val result = repository.getServices(categoryId)
        result.onSuccess { services ->
            _uiState.update { 
                it.copy(
                    servicesState = UiState.Success(services),
                    connectionStatus = repository.getConnectionStatus()
                ) 
            }
        }.onFailure { error ->
            _uiState.update { 
                it.copy(
                    servicesState = UiState.Error(error.localizedMessage ?: "Failed to load civic services"),
                    connectionStatus = repository.getConnectionStatus()
                ) 
            }
        }
    }

    private suspend fun loadProfile() {
        val result = repository.getProfile(currentUserId)
        result.onSuccess { profile ->
            _uiState.update { it.copy(profile = profile) }
        }
    }

    // Phase 10: Emergency Services, Discovery & Civic Updates
    fun openEmergencyPage() {
        _uiState.update { it.copy(currentTab = NavTab.EMERGENCY) }
        loadEmergencyServices()
    }

    fun openCivicUpdatesPage(wardId: String? = null) {
        _uiState.update { 
            it.copy(
                currentTab = NavTab.UPDATES,
                selectedWardFilterForUpdates = wardId
            ) 
        }
        loadCivicUpdates(wardId)
    }

    fun setServiceSearchQuery(query: String) {
        _uiState.update { it.copy(serviceSearchQuery = query) }
    }

    fun openServiceDetailPage(service: CivicService?) {
        _uiState.update { it.copy(activeServiceDetail = service) }
    }

    fun closeServiceDetailPage() {
        _uiState.update { it.copy(activeServiceDetail = null) }
    }

    fun openUpdateDetailPage(update: CivicUpdate?) {
        _uiState.update { it.copy(activeUpdateDetail = update) }
    }

    fun closeUpdateDetailPage() {
        _uiState.update { it.copy(activeUpdateDetail = null) }
    }

    fun setWardFilterForUpdates(wardId: String?) {
        _uiState.update { it.copy(selectedWardFilterForUpdates = wardId) }
        loadCivicUpdates(wardId)
    }

    fun loadEmergencyServices() {
        viewModelScope.launch {
            loadEmergencyServicesInternal()
        }
    }

    private suspend fun loadEmergencyServicesInternal() {
        val result = repository.getEmergencyServices()
        result.onSuccess { list ->
            _uiState.update { it.copy(emergencyServicesState = UiState.Success(list)) }
        }.onFailure { error ->
            _uiState.update { it.copy(emergencyServicesState = UiState.Error(error.localizedMessage ?: "Failed to load emergency services")) }
        }
    }

    fun loadCivicUpdates(wardId: String? = null) {
        viewModelScope.launch {
            loadCivicUpdatesInternal(wardId)
        }
    }

    private suspend fun loadCivicUpdatesInternal(wardId: String? = null) {
        val result = repository.getCivicUpdates(wardId)
        result.onSuccess { list ->
            _uiState.update { it.copy(civicUpdatesState = UiState.Success(list)) }
        }.onFailure { error ->
            _uiState.update { it.copy(civicUpdatesState = UiState.Error(error.localizedMessage ?: "Failed to load civic updates")) }
        }
    }

    // Phase 8: Realtime Citizen Notifications & Live Alerts
    fun loadNotifications() {
        viewModelScope.launch {
            val result = repository.getNotifications(currentUserId)
            result.onSuccess { notifs ->
                val unread = notifs.count { !it.read }
                _uiState.update {
                    it.copy(
                        notificationsState = UiState.Success(notifs),
                        unreadNotificationsCount = unread
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        notificationsState = UiState.Error(err.localizedMessage ?: "Failed to load notifications")
                    )
                }
            }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
            loadNotifications()
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(currentUserId)
            loadNotifications()
        }
    }

    fun setShowNotificationsSheet(show: Boolean) {
        _uiState.update { it.copy(showNotificationsSheet = show) }
        if (show) {
            loadNotifications()
        }
    }

    fun dismissLiveToast() {
        _uiState.update { it.copy(activeLiveToast = null) }
    }

    fun onNotificationClicked(notification: NotificationRecord) {
        dismissLiveToast()
        _uiState.update { it.copy(showNotificationsSheet = false) }
        markNotificationRead(notification.id)
        if (!notification.complaintId.isNullOrBlank()) {
            openComplaintTracking(notification.complaintId)
        }
    }

    private fun observeRealtimeNotifications() {
        viewModelScope.launch {
            repository.realtimeClient.connect(currentUserId)

            launch {
                repository.realtimeClient.connectionState.collect { state ->
                    _uiState.update { it.copy(realtimeConnectionState = state) }
                }
            }

            launch {
                repository.realtimeClient.events.collect { event ->
                    when (event) {
                        is RealtimeEvent.NotificationInserted -> {
                            repository.addNotificationFromEvent(event.notification)
                            loadNotifications()
                            // Show live banner/toast
                            val toast = LiveToastNotification(
                                id = event.notification.id,
                                title = event.notification.title,
                                message = event.notification.message,
                                complaintId = event.notification.complaintId,
                                type = NotificationType.fromString(event.notification.type)
                            )
                            _uiState.update { it.copy(activeLiveToast = toast) }
                        }
                        is RealtimeEvent.ComplaintUpdated -> {
                            loadComplaints()
                            if (_uiState.value.selectedComplaintId == event.complaintId) {
                                openComplaintTracking(event.complaintId)
                            }
                        }
                        is RealtimeEvent.StatusHistoryInserted -> {
                            if (_uiState.value.selectedComplaintId == event.complaintId) {
                                openComplaintTracking(event.complaintId)
                            }
                        }
                        is RealtimeEvent.ComplaintUpdateInserted -> {
                            if (_uiState.value.selectedComplaintId == event.complaintId) {
                                openComplaintTracking(event.complaintId)
                            }
                        }
                        is RealtimeEvent.ConnectionChanged -> {
                            _uiState.update { it.copy(realtimeConnectionState = event.state) }
                        }
                    }
                }
            }

            launch {
                repository.notificationsFlow.collect { list ->
                    val unread = list.count { !it.read }
                    _uiState.update {
                        it.copy(
                            notificationsState = UiState.Success(list),
                            unreadNotificationsCount = unread
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.realtimeClient.disconnect()
    }
}
