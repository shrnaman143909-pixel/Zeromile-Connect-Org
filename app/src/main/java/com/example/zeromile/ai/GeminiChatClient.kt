package com.example.zeromile.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * One part of a conversation turn as understood by the Gemini REST API.
 */
sealed interface ChatTurnPart {
    data class Text(val text: String) : ChatTurnPart
    data class FunctionCall(val name: String, val arguments: JSONObject) : ChatTurnPart
    data class FunctionResponse(val name: String, val response: JSONObject) : ChatTurnPart
}

/**
 * A conversation turn. [role] is either "user" or "model" — the Gemini wire vocabulary.
 */
data class ChatTurn(
    val role: String,
    val parts: List<ChatTurnPart>
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_MODEL = "model"

        fun user(text: String): ChatTurn = ChatTurn(ROLE_USER, listOf(ChatTurnPart.Text(text)))
        fun model(text: String): ChatTurn = ChatTurn(ROLE_MODEL, listOf(ChatTurnPart.Text(text)))
        fun toolResponse(name: String, response: JSONObject): ChatTurn =
            ChatTurn(ROLE_USER, listOf(ChatTurnPart.FunctionResponse(name, response)))
    }
}

/**
 * Outcome of a single assistant turn.
 */
sealed interface ChatCompletion {
    data class Reply(val text: String) : ChatCompletion
    data class ToolCall(val name: String, val arguments: JSONObject) : ChatCompletion
    data class Failure(val message: String) : ChatCompletion
}

/**
 * Thin Gemini `generateContent` client for the multi-turn assistant.
 *
 * Deliberately mirrors [GeminiCivicClassifier] so the app keeps a single AI transport style:
 * raw OkHttp, org.json bodies, no extra dependencies. The difference is that this client
 * carries conversation history and declares function declarations, so the model can ask the
 * app to look up municipal services or complaint status before answering.
 *
 * The API key is read from BuildConfig (populated by the Gradle Secrets plugin from `.env`).
 * When the key is missing the client reports [isConfigured] = false and the ViewModel answers
 * with an offline, rule-based reply instead of failing silently.
 */
class GeminiChatClient(
    private val apiKey: String = runCatching { BuildConfig.GEMINI_API_KEY }.getOrDefault(""),
    private val modelName: String = DEFAULT_MODEL,
    private val httpClient: OkHttpClient = defaultHttpClient()
) {
    private val tag = "GeminiChatClient"

    val isConfigured: Boolean
        get() = apiKey.isNotBlank() &&
            apiKey != "MY_GEMINI_API_KEY" &&
            !apiKey.startsWith("your_")

    suspend fun complete(
        systemPrompt: String,
        turns: List<ChatTurn>
    ): ChatCompletion = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext ChatCompletion.Failure(
                "The assistant is not configured with a Gemini API key."
            )
        }
        if (turns.isEmpty()) {
            return@withContext ChatCompletion.Failure("Nothing to send.")
        }

        val requestBody = buildRequestBody(systemPrompt, turns)
        val url = "$BASE_URL/models/$modelName:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(tag, "Gemini chat HTTP ${response.code}: ${body.take(400)}")
                    return@withContext ChatCompletion.Failure(
                        "The assistant could not reach Gemini right now (HTTP ${response.code})."
                    )
                }
                parseResponse(body)
            }
        } catch (e: Exception) {
            Log.e(tag, "Gemini chat call failed", e)
            ChatCompletion.Failure(
                e.localizedMessage ?: "The assistant could not reach Gemini right now."
            )
        }
    }

    internal fun buildRequestBody(systemPrompt: String, turns: List<ChatTurn>): JSONObject {
        val contents = JSONArray()
        turns.forEach { turn ->
            contents.put(
                JSONObject().apply {
                    put("role", turn.role)
                    put(
                        "parts",
                        JSONArray().apply {
                            turn.parts.forEach { part -> put(partToJson(part)) }
                        }
                    )
                }
            )
        }

        return JSONObject().apply {
            if (systemPrompt.isNotBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt))
                    )
                )
            }
            put("contents", contents)
            put(
                "tools",
                JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations()))
            )
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 1024)
                }
            )
        }
    }

    internal fun parseResponse(rawBody: String): ChatCompletion {
        val json = try {
            JSONObject(rawBody)
        } catch (e: Exception) {
            return ChatCompletion.Failure("The assistant returned an unreadable response.")
        }

        val blockReason = json.optJSONObject("promptFeedback")?.optString("blockReason", "")
        if (!blockReason.isNullOrBlank()) {
            return ChatCompletion.Failure(
                "That request was blocked by the safety filter. Try rephrasing it."
            )
        }

        val candidates = json.optJSONArray("candidates")
        val first = candidates?.optJSONObject(0)
        if (first == null) {
            return ChatCompletion.Failure("The assistant did not return a response.")
        }

        val parts = first.optJSONObject("content")?.optJSONArray("parts")
            ?: return ChatCompletion.Failure("The assistant returned an empty message.")

        // A function call anywhere in the parts wins: the model wants data before it answers.
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val functionCall = part.optJSONObject("functionCall") ?: continue
            val name = functionCall.optString("name", "")
            if (name.isNotBlank()) {
                return ChatCompletion.ToolCall(
                    name = name,
                    arguments = functionCall.optJSONObject("args") ?: JSONObject()
                )
            }
        }

        val builder = StringBuilder()
        for (i in 0 until parts.length()) {
            val text = parts.optJSONObject(i)?.optString("text", "").orEmpty()
            if (text.isNotBlank()) {
                if (builder.isNotEmpty()) builder.append("\n")
                builder.append(text.trim())
            }
        }

        val answer = builder.toString().trim()
        return if (answer.isEmpty()) {
            ChatCompletion.Failure("The assistant returned an empty message.")
        } else {
            ChatCompletion.Reply(answer)
        }
    }

    private fun partToJson(part: ChatTurnPart): JSONObject = when (part) {
        is ChatTurnPart.Text -> JSONObject().put("text", part.text)
        is ChatTurnPart.FunctionCall -> JSONObject().put(
            "functionCall",
            JSONObject().apply {
                put("name", part.name)
                put("args", part.arguments)
            }
        )
        is ChatTurnPart.FunctionResponse -> JSONObject().put(
            "functionResponse",
            JSONObject().apply {
                put("name", part.name)
                put("response", part.response)
            }
        )
    }

    /**
     * Tools the model may call. Keep this list in sync with [ChatToolExecutor].
     */
    private fun functionDeclarations(): JSONArray {
        fun property(type: String, description: String): JSONObject = JSONObject().apply {
            put("type", type)
            put("description", description)
        }

        fun parameters(props: JSONObject, required: List<String> = emptyList()): JSONObject =
            JSONObject().apply {
                put("type", "OBJECT")
                put("properties", props)
                if (required.isNotEmpty()) {
                    put("required", JSONArray().apply { required.forEach { put(it) } })
                }
            }

        return JSONArray().apply {
            put(
                JSONObject().apply {
                    put("name", "find_service")
                    put(
                        "description",
                        "Search the Nagpur Municipal Corporation service catalogue. Use for questions " +
                            "about which department handles an issue (potholes, garbage, water leaks, " +
                            "streetlights, noise), what a service covers, or what information a " +
                            "complaint needs."
                    )
                    put(
                        "parameters",
                        parameters(
                            props = JSONObject().put(
                                "query",
                                property(
                                    "STRING",
                                    "Free-text issue or service, e.g. 'pothole on Wardha Road' or 'garbage collection'."
                                )
                            ),
                            required = listOf("query")
                        )
                    )
                }
            )
            put(
                JSONObject().apply {
                    put("name", "check_complaint_status")
                    put(
                        "description",
                        "Look up one civic complaint by its complaint number (for example " +
                            "NMC-2026-004821) and return its current status, department, assigned " +
                            "team and latest official update."
                    )
                    put(
                        "parameters",
                        parameters(
                            props = JSONObject().put(
                                "complaintNumber",
                                property("STRING", "The complaint number exactly as shown in the app.")
                            ),
                            required = listOf("complaintNumber")
                        )
                    )
                }
            )
            put(
                JSONObject().apply {
                    put("name", "list_my_complaints")
                    put(
                        "description",
                        "List the citizen's most recent complaints with their status, so you can " +
                            "answer questions like 'what is happening with my complaints' or find " +
                            "a complaint number."
                    )
                    put("parameters", parameters(JSONObject()))
                }
            )
            put(
                JSONObject().apply {
                    put("name", "start_complaint")
                    put(
                        "description",
                        "Hand the citizen over to the official complaint intake form with a " +
                            "pre-filled description. Use only when the citizen clearly wants to " +
                            "report a new civic issue and you already know what the issue is."
                    )
                    put(
                        "parameters",
                        parameters(
                            props = JSONObject().put(
                                "description",
                                property(
                                    "STRING",
                                    "One or two sentence complaint description in the citizen's language."
                                )
                            ),
                            required = listOf("description")
                        )
                    )
                }
            )
        }
    }

    companion object {
        /**
         * Kept aligned with docs/ARCHITECTURE.md. If the project moves to a newer Gemini
         * model, this single constant is the switch.
         */
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
