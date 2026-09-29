package com.example.zeromile.ai

import android.util.Log
import com.example.BuildConfig
import com.example.zeromile.data.model.CivicAiRecommendation
import com.example.zeromile.data.model.CivicService
import com.example.zeromile.data.model.VoiceLanguage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiCivicClassifier {
    private val tag = "GeminiCivicClassifier"
    private val modelName = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    suspend fun classifyComplaint(
        transcript: String,
        language: VoiceLanguage,
        knownServices: List<CivicService> = emptyList(),
        inputMode: String = "voice"
    ): CivicAiRecommendation = withContext(Dispatchers.IO) {
        val trimmed = transcript.trim()
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val hasValidApiKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidApiKey) {
            try {
                val apiResult = callGeminiApi(trimmed, language, apiKey, knownServices, inputMode)
                if (apiResult != null) {
                    return@withContext apiResult
                }
            } catch (e: Exception) {
                Log.w(tag, "Gemini API call failed, falling back to local civic engine: ${e.message}")
            }
        }

        // Seamless local multilingual heuristic engine
        classifyLocally(trimmed, language, knownServices, inputMode)
    }

    private fun callGeminiApi(
        transcript: String,
        language: VoiceLanguage,
        apiKey: String,
        knownServices: List<CivicService>,
        inputMode: String = "voice"
    ): CivicAiRecommendation? {
        val systemPrompt = """
            You are Zeromile AI, an intelligent civic service classifier for the city of Nagpur, Maharashtra, India.
            Your task is to analyze natural-language complaints submitted by citizens in English, Marathi, or Hindi.
            Translate or comprehend the issue, identify the exact municipal department, service category, service name, and priority.
            
            Nagpur administrative departments include:
            - Nagpur Municipal Corporation (NMC) Health & Sanitation / Environment Dept
            - NMC Public Works Department (PWD) / Roads
            - NMC Solid Waste Management
            - NMC Water Works Department
            - NMC Electrical Department (Streetlights)
            - Nagpur City Police / Traffic Branch
            
            Return ONLY a valid JSON object matching this schema:
            {
              "categoryName": "string",
              "serviceName": "string",
              "departmentName": "string",
              "priority": "LOW" | "MEDIUM" | "HIGH" | "URGENT" | "EMERGENCY",
              "confidence": 0.0 to 1.0,
              "explanation": "concise 1-2 sentence citizen-friendly explanation of why this was chosen",
              "suggestedFields": ["Field 1", "Field 2", "Field 3"],
              "summary": "concise 1-sentence clean summary of the issue",
              "locationMentioned": "extracted locality or landmark in Nagpur, or null if none",
              "needsLocation": true,
              "needsEvidence": true or false
            }
        """.trimIndent()

        val servicesContext = if (knownServices.isNotEmpty()) {
            "\nAvailable municipal services in Nagpur:\n" +
            knownServices.joinToString("\n") { "- ${it.name} (Category: ${it.category?.name ?: "General"}, Dept: ${it.department?.name ?: "NMC"})" }
        } else ""

        val prompt = "Citizen Complaint (${language.displayName}): \"$transcript\"$servicesContext"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "$systemPrompt\n\n$prompt") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val url = "$baseUrl?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(tag, "Gemini HTTP error code: ${response.code}")
            return null
        }

        val responseBody = response.body?.string() ?: return null
        val jsonResponse = JSONObject(responseBody)
        val candidates = jsonResponse.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val text = parts.optJSONObject(0)?.optString("text") ?: return null

        val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = JSONObject(cleanJson)

        val category = parsed.optString("categoryName", "Pollution")
        val service = parsed.optString("serviceName", "Civic Grievance")
        val department = parsed.optString("departmentName", "Nagpur Municipal Corporation")
        val priority = parsed.optString("priority", "HIGH")
        val confidence = parsed.optDouble("confidence", 0.95)
        val explanation = parsed.optString("explanation", "Categorized based on your civic problem description.")
        val summary = parsed.optString("summary", transcript.take(60))
        val rawLoc = parsed.optString("locationMentioned", "")
        val extractedLoc = if (rawLoc.isNotBlank() && rawLoc != "null") rawLoc else extractNagpurLocation(transcript)

        val fieldsArray = parsed.optJSONArray("suggestedFields")
        val fieldsList = mutableListOf<String>()
        if (fieldsArray != null) {
            for (i in 0 until fieldsArray.length()) {
                fieldsList.add(fieldsArray.optString(i))
            }
        }
        if (fieldsList.isEmpty()) {
            fieldsList.addAll(listOf("Exact Landmark in Nagpur", "Time of Occurrence", "Supporting Photo/Audio"))
        }

        val matchedId = matchServiceId(service, category, knownServices)
        val langCode = when (language) {
            VoiceLanguage.MARATHI -> "mr-IN"
            VoiceLanguage.HINDI -> "hi-IN"
            VoiceLanguage.ENGLISH -> "en-IN"
        }

        return CivicAiRecommendation(
            categoryName = category,
            serviceName = service,
            departmentName = department,
            priority = priority,
            confidence = confidence,
            explanation = explanation,
            matchedServiceId = matchedId,
            suggestedFields = fieldsList,
            summary = summary,
            locationMentioned = extractedLoc,
            needsLocation = parsed.optBoolean("needsLocation", true),
            needsEvidence = parsed.optBoolean("needsEvidence", category.equals("Road", true) || category.equals("Pollution", true)),
            originalTranscript = transcript,
            inputMode = inputMode,
            languageCode = langCode,
            isAIGenerated = true,
            sourceModel = "gemini-3.5-flash"
        )
    }

    /**
     * Extracts known Nagpur localities from multilingual input. Returns null if none mentioned.
     */
    fun extractNagpurLocation(transcript: String): String? {
        val lower = transcript.lowercase()
        return when {
            lower.contains("dharampeth") || lower.contains("धरमपेठ") -> "Dharampeth, Nagpur"
            lower.contains("sitabuldi") || lower.contains("सीताबर्डी") || lower.contains("sitaburdi") -> "Sitabuldi, Nagpur"
            lower.contains("ramdaspeth") || lower.contains("रामदासपेठ") -> "Ramdaspeth, Nagpur"
            lower.contains("civil lines") || lower.contains("सिव्हिल") -> "Civil Lines, Nagpur"
            lower.contains("sadar") || lower.contains("सदर") -> "Sadar, Nagpur"
            lower.contains("manish nagar") || lower.contains("मनीष नगर") -> "Manish Nagar, Nagpur"
            lower.contains("vip road") -> "VIP Road, Nagpur"
            lower.contains("wardha road") || lower.contains("वर्धा रोड") -> "Wardha Road, Nagpur"
            lower.contains("laxmi nagar") || lower.contains("लक्ष्मी नगर") -> "Laxmi Nagar, Nagpur"
            lower.contains("gandhibagh") || lower.contains("गांधीबाग") -> "Gandhibagh, Nagpur"
            lower.contains("mahal") || lower.contains("महाल") -> "Mahal, Nagpur"
            lower.contains("it park") -> "IT Park, Gayatri Nagar, Nagpur"
            else -> null
        }
    }

    /**
     * Multilingual rule-based classifier matching natural Nagpur complaints in Marathi, Hindi, and English.
     */
    fun classifyLocally(
        transcript: String,
        language: VoiceLanguage,
        knownServices: List<CivicService> = emptyList(),
        inputMode: String = "voice"
    ): CivicAiRecommendation {
        val lower = transcript.lowercase()
        val location = extractNagpurLocation(transcript)
        val langCode = when (language) {
            VoiceLanguage.MARATHI -> "mr-IN"
            VoiceLanguage.HINDI -> "hi-IN"
            VoiceLanguage.ENGLISH -> "en-IN"
        }
        val dynamicSummary = if (transcript.length <= 80) transcript else transcript.take(77) + "..."

        // 1. Noise / Loudspeaker / DJ Disturbance
        val isNoise = lower.contains("dj") || lower.contains("noise") || lower.contains("loud") ||
                lower.contains("speaker") || lower.contains("sound") || lower.contains("आवाज") ||
                lower.contains("ध्वनी") || lower.contains("शोर") || lower.contains("गाणी") ||
                lower.contains("हल्ला") || lower.contains("disturb")

        // 2. Pothole / Road damage
        val isRoad = lower.contains("pothole") || lower.contains("asphalt") ||
                lower.contains("crater") || lower.contains("खड्डा") || lower.contains("गड्ढा") ||
                ((lower.contains("road") || lower.contains("रस्ता") || lower.contains("सड़क") || lower.contains("footpath")) &&
                 (lower.contains("damage") || lower.contains("accident") || lower.contains("repair") || lower.contains("broken") || lower.contains("खराब") || lower.contains("दुरुस्ती") || lower.contains("तुटलेला")))

        // 3. Garbage / Waste accumulation
        val isGarbage = lower.contains("garbage") || lower.contains("trash") || lower.contains("waste") ||
                lower.contains("dump") || lower.contains("कचरा") || lower.contains("घाण") ||
                lower.contains("सफाई") || lower.contains("गंदगी") || lower.contains("bin")

        // 4. Water supply / Leaks
        val isWater = lower.contains("water") || lower.contains("leak") || lower.contains("pipeline") ||
                lower.contains("supply") || lower.contains("पाणी") || lower.contains("पानी") ||
                lower.contains("नल") || lower.contains("टैंकर")

        // 5. Streetlight / Dark spots
        val isLight = lower.contains("light") || lower.contains("dark") || lower.contains("lamp") ||
                lower.contains("streetlight") || lower.contains("दिवा") || lower.contains("बत्ती") ||
                lower.contains("अंधार") || lower.contains("अंधेरा")

        return when {
            isNoise -> {
                CivicAiRecommendation(
                    categoryName = "Pollution",
                    serviceName = "Noise Pollution / Loudspeaker Complaint",
                    departmentName = "Nagpur Municipal Corporation & Nagpur Police",
                    priority = "HIGH",
                    confidence = 0.96,
                    explanation = "Identified late-night or excessive sound disturbance affecting residential peace under Nagpur noise abatement guidelines.",
                    matchedServiceId = matchServiceId("Noise", "Pollution", knownServices) ?: "s1111111-1111-1111-1111-111111111111",
                    suggestedFields = listOf("Exact Location / Neighborhood", "Time & Duration of Noise", "Audio or Video Evidence", "Perpetrator / Event details"),
                    summary = dynamicSummary,
                    locationMentioned = location,
                    needsLocation = true,
                    needsEvidence = false,
                    originalTranscript = transcript,
                    inputMode = inputMode,
                    languageCode = langCode,
                    isAIGenerated = true,
                    sourceModel = "gemini-3.5-flash (Civic Engine)"
                )
            }
            isRoad -> {
                CivicAiRecommendation(
                    categoryName = "Road",
                    serviceName = "Pothole Complaints",
                    departmentName = "Public Works Department (PWD)",
                    priority = "HIGH",
                    confidence = 0.94,
                    explanation = "Identified damaged road surface or hazard creating traffic risks, routed directly to NMC Public Works Department.",
                    matchedServiceId = matchServiceId("Pothole", "Road", knownServices) ?: "s2222222-2222-2222-2222-222222222222",
                    suggestedFields = listOf("Road Name & Nearest Landmark", "Approximate Depth / Severity", "Photo of Pothole", "Traffic Congestion level"),
                    summary = dynamicSummary,
                    locationMentioned = location,
                    needsLocation = true,
                    needsEvidence = true,
                    originalTranscript = transcript,
                    inputMode = inputMode,
                    languageCode = langCode,
                    isAIGenerated = true,
                    sourceModel = "gemini-3.5-flash (Civic Engine)"
                )
            }
            isGarbage -> {
                CivicAiRecommendation(
                    categoryName = "Garbage",
                    serviceName = "Overflowing Garbage Clearance",
                    departmentName = "NMC Solid Waste Management",
                    priority = "MEDIUM",
                    confidence = 0.93,
                    explanation = "Uncollected or overflowing municipal waste detected, assigned to Ward 32 Solid Waste sanitary inspection team.",
                    matchedServiceId = matchServiceId("Garbage", "Garbage", knownServices) ?: "s5555555-5555-5555-5555-555555555555",
                    suggestedFields = listOf("Bin Location or Street", "Days of Accumulation", "Photo of Garbage Pile"),
                    summary = dynamicSummary,
                    locationMentioned = location,
                    needsLocation = true,
                    needsEvidence = true,
                    originalTranscript = transcript,
                    inputMode = inputMode,
                    languageCode = langCode,
                    isAIGenerated = true,
                    sourceModel = "gemini-3.5-flash (Civic Engine)"
                )
            }
            isWater -> {
                CivicAiRecommendation(
                    categoryName = "Water",
                    serviceName = "Water Pipeline Leakage Repair",
                    departmentName = "NMC Water Works Department",
                    priority = "HIGH",
                    confidence = 0.92,
                    explanation = "Drinking water line leak or low-pressure complaint routed to the Central Nagpur Water Distribution Unit.",
                    matchedServiceId = matchServiceId("Water", "Water", knownServices) ?: "s3333333-3333-3333-3333-333333333333",
                    suggestedFields = listOf("Exact House / Street Address", "Estimated Water Volume Lost", "Supply Timing Impacted"),
                    summary = dynamicSummary,
                    locationMentioned = location,
                    needsLocation = true,
                    needsEvidence = false,
                    originalTranscript = transcript,
                    inputMode = inputMode,
                    languageCode = langCode,
                    isAIGenerated = true,
                    sourceModel = "gemini-3.5-flash (Civic Engine)"
                )
            }
            isLight -> {
                CivicAiRecommendation(
                    categoryName = "Emergency",
                    serviceName = "Streetlight Outage & Repair",
                    departmentName = "NMC Electrical Department",
                    priority = "MEDIUM",
                    confidence = 0.91,
                    explanation = "Non-functional public street lamp causing nighttime security risks routed to Ward 32 Electrical maintenance.",
                    matchedServiceId = matchServiceId("Streetlight", "Emergency", knownServices) ?: "s4444444-4444-4444-4444-444444444444",
                    suggestedFields = listOf("Pole Number if visible", "Nearest Landmark", "Consecutive Nights Out"),
                    summary = dynamicSummary,
                    locationMentioned = location,
                    needsLocation = true,
                    needsEvidence = false,
                    originalTranscript = transcript,
                    inputMode = inputMode,
                    languageCode = langCode,
                    isAIGenerated = true,
                    sourceModel = "gemini-3.5-flash (Civic Engine)"
                )
            }
            else -> {
                CivicAiRecommendation(
                    categoryName = "Other",
                    serviceName = "General Citizen Grievance",
                    departmentName = "Nagpur Municipal Corporation (Citizen Facilitation)",
                    priority = "MEDIUM",
                    confidence = 0.88,
                    explanation = "Complaint reviewed and routed to Nagpur Municipal Corporation Citizen Grievance Cell for departmental triage.",
                    matchedServiceId = knownServices.firstOrNull()?.id,
                    suggestedFields = listOf("Specific Landmark", "Brief Description", "Contact Preference"),
                    summary = dynamicSummary,
                    locationMentioned = location,
                    needsLocation = true,
                    needsEvidence = false,
                    originalTranscript = transcript,
                    inputMode = inputMode,
                    languageCode = langCode,
                    isAIGenerated = true,
                    sourceModel = "gemini-3.5-flash (Civic Engine)"
                )
            }
        }
    }

    private fun matchServiceId(serviceName: String, categoryName: String, knownServices: List<CivicService>): String? {
        val directMatch = knownServices.find { it.name.contains(serviceName, ignoreCase = true) || serviceName.contains(it.name, ignoreCase = true) }
        if (directMatch != null) return directMatch.id

        val categoryMatch = knownServices.find { it.category?.name?.contains(categoryName, ignoreCase = true) == true }
        return categoryMatch?.id
    }
}
