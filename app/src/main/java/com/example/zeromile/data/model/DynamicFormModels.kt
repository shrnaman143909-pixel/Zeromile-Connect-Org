package com.example.zeromile.data.model

enum class FormFieldType {
    TEXT,
    TEXT_AREA,
    CHIPS,
    DROPDOWN,
    CHECKBOX,
    LOCATION_LANDMARK,
    TIME_PERIOD,
    SEVERITY_LEVEL,
    EVIDENCE_ATTACHMENT
}

enum class FormStep {
    FILLING,
    REVIEWING,
    READY_FOR_SUBMISSION,
    SUBMITTED
}

data class FormFieldConfig(
    val id: String,
    val label: String,
    val placeholder: String = "",
    val type: FormFieldType,
    val options: List<String> = emptyList(),
    val isRequired: Boolean = true,
    val defaultValue: String = "",
    val helpText: String? = null,
    val prefillSource: String? = null, // e.g. "location", "summary", "phone", "ward", "transcript"
    val maxFiles: Int = 5,
    val maxSizeMB: Int = 10
)

data class ServiceFormConfig(
    val serviceId: String,
    val serviceName: String,
    val categoryName: String,
    val departmentName: String,
    val defaultPriority: CivicPriority = CivicPriority.HIGH,
    val description: String = "",
    val fields: List<FormFieldConfig>,
    val requiresPhotoOrEvidence: Boolean = false,
    val estimatedResolutionDays: Int = 3,
    val submissionNote: String = "Your complaint will be directly assigned to the Nagpur Municipal ward officer."
)

data class FieldResponseItem(
    val fieldId: String,
    val label: String,
    val value: String,
    val isHighlighted: Boolean = false
)

data class ComplaintReviewData(
    val referenceId: String,
    val serviceId: String = "s1111111-1111-1111-1111-111111111111",
    val serviceName: String,
    val categoryId: String? = null,
    val categoryName: String,
    val departmentId: String? = null,
    val departmentName: String,
    val priority: CivicPriority,
    val ward: String,
    val wardId: String? = "w1111111-1111-1111-1111-111111111111",
    val citizenName: String,
    val citizenPhone: String,
    val originalTranscript: String,
    val inputMode: String,
    val language: VoiceLanguage,
    val summary: String,
    val fieldResponses: List<FieldResponseItem>,
    val hasEvidenceAttached: Boolean = false,
    val evidenceType: String? = null,
    val attachedEvidence: List<StagedEvidenceItem> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Double? = null,
    val locationSource: String = "manual",
    val submissionTimestamp: Long = System.currentTimeMillis()
)
