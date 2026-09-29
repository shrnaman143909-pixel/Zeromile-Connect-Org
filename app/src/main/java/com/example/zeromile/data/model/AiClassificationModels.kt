package com.example.zeromile.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class CivicPriority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4),
    EMERGENCY("Emergency", 5)
}

@JsonClass(generateAdapter = true)
data class CivicAiRecommendation(
    @Json(name = "categoryName")
    val categoryName: String,
    @Json(name = "serviceName")
    val serviceName: String,
    @Json(name = "departmentName")
    val departmentName: String,
    @Json(name = "priority")
    val priority: String,
    @Json(name = "confidence")
    val confidence: Double,
    @Json(name = "explanation")
    val explanation: String,
    @Json(name = "matchedServiceId")
    val matchedServiceId: String? = null,
    @Json(name = "suggestedFields")
    val suggestedFields: List<String> = emptyList(),
    @Json(name = "summary")
    val summary: String? = null,
    @Json(name = "locationMentioned")
    val locationMentioned: String? = null,
    @Json(name = "needsLocation")
    val needsLocation: Boolean = true,
    @Json(name = "needsEvidence")
    val needsEvidence: Boolean = false,
    @Json(name = "originalTranscript")
    val originalTranscript: String? = null,
    @Json(name = "inputMode")
    val inputMode: String = "voice",
    @Json(name = "languageCode")
    val languageCode: String = "en-IN",
    @Json(name = "isAIGenerated")
    val isAIGenerated: Boolean = true,
    @Json(name = "sourceModel")
    val sourceModel: String = "gemini-3.5-flash"
)

/**
 * State for the pre-filled complaint form generated from AI analysis
 */
data class ComplaintFormDraft(
    val serviceName: String,
    val categoryName: String,
    val departmentName: String,
    val ward: String = "Ward 32, Dharampeth",
    val citizenTranscript: String,
    val language: VoiceLanguage,
    val priority: CivicPriority,
    val locationLandmark: String = "Dharampeth, Nagpur",
    val citizenPhone: String = "+91 98230 12345",
    val citizenName: String = "Rajesh Sharma",
    val incidentTime: String = "Today / Recent",
    val additionalNotes: String = ""
)
