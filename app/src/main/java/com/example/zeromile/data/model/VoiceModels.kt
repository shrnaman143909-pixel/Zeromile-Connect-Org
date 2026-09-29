package com.example.zeromile.data.model

enum class VoiceLanguage(val displayName: String, val localeCode: String, val nativeScript: String) {
    ENGLISH("English", "en-IN", "English"),
    MARATHI("मराठी", "mr-IN", "मराठी"),
    HINDI("हिंदी", "hi-IN", "हिंदी")
}

enum class VoiceInputMode {
    VOICE,
    TEXT
}

/**
 * Structured request prepared for Phase 3 Gemini AI classification.
 */
data class CivicVoiceRequest(
    val inputMode: VoiceInputMode,
    val language: VoiceLanguage,
    val transcript: String,
    val timestamp: Long = System.currentTimeMillis()
)
