package com.example.zeromile.ai

import com.example.zeromile.data.model.ChatAction
import com.example.zeromile.data.model.ChatActionKind
import com.example.zeromile.data.model.CivicService
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.ComplaintTrackingDetails
import com.example.zeromile.data.repository.CivicRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Result of one tool call. [response] is fed back to Gemini as a functionResponse part;
 * [forcedAction] is an optional UI affordance the ViewModel attaches to the following bubble.
 */
data class ChatToolResult(
    val name: String,
    val response: JSONObject,
    val forcedAction: ChatAction? = null
)

/**
 * Executes the assistant's function calls against the existing data layer.
 *
 * Everything here is read-only except `start_complaint`, which performs no write at all —
 * it only hands the conversation over to the existing intake form. That keeps the chat
 * surface incapable of mutating municipal records on its own.
 */
class ChatToolExecutor(
    private val repository: CivicRepository = CivicRepository()
) {
    suspend fun execute(
        name: String,
        arguments: JSONObject,
        userId: String
    ): ChatToolResult = when (name) {
        "find_service" -> findService(arguments.optString("query", ""))
        "check_complaint_status" -> checkComplaintStatus(arguments.optString("complaintNumber", ""))
        "list_my_complaints" -> listMyComplaints(userId)
        "start_complaint" -> startComplaint(arguments.optString("description", ""))
        else -> ChatToolResult(
            name = name,
            response = JSONObject().put("error", "Unknown tool: $name")
        )
    }

    private suspend fun findService(query: String): ChatToolResult {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return ChatToolResult(
                "find_service",
                JSONObject().put("error", "A search query is required.")
            )
        }

        val services = repository.getServices().getOrElse { emptyList() }
        if (services.isEmpty()) {
            return ChatToolResult(
                "find_service",
                JSONObject().put("found", false).put("message", "The service catalogue is unavailable.")
            )
        }

        val matches = rankServices(services, trimmed, limit = 5)
        if (matches.isEmpty()) {
            return ChatToolResult(
                "find_service",
                JSONObject().put("found", false)
                    .put("message", "No matching municipal service was found for that issue.")
            )
        }

        val array = JSONArray()
        matches.forEach { service ->
            array.put(
                JSONObject().apply {
                    put("name", service.name)
                    put("category", service.category?.name ?: "")
                    put("department", service.department?.name ?: "")
                    put("summary", service.description ?: service.processDescription ?: "")
                    put("type", service.serviceType)
                    put("needsLocation", service.requiresLocation)
                    put("acceptsEvidence", service.supportsEvidence)
                }
            )
        }
        return ChatToolResult(
            "find_service",
            JSONObject().apply {
                put("found", true)
                put("query", trimmed)
                put("matches", array)
            }
        )
    }

    private suspend fun checkComplaintStatus(complaintNumber: String): ChatToolResult {
        val reference = complaintNumber.trim()
        if (reference.isBlank()) {
            return ChatToolResult(
                "check_complaint_status",
                JSONObject().put("error", "A complaint number is required.")
            )
        }

        val details = repository.getComplaintTrackingDetails(reference).getOrElse { null }
        if (details == null) {
            return ChatToolResult(
                "check_complaint_status",
                JSONObject().apply {
                    put("found", false)
                    put("complaintNumber", reference)
                    put("message", "No complaint found with that number for this citizen.")
                }
            )
        }

        return ChatToolResult("check_complaint_status", complaintDetailsToJson(details))
    }

    private suspend fun listMyComplaints(userId: String): ChatToolResult {
        val complaints = repository.getComplaints(userId).getOrElse { emptyList() }
        if (complaints.isEmpty()) {
            return ChatToolResult(
                "list_my_complaints",
                JSONObject().put("count", 0).put("complaints", JSONArray())
            )
        }

        val recent = complaints
            .sortedByDescending { it.createdAt ?: "" }
            .take(5)

        val array = JSONArray()
        recent.forEach { complaint ->
            array.put(
                JSONObject().apply {
                    put("complaintNumber", complaint.complaintNumber)
                    put("status", complaint.status)
                    put("priority", complaint.priority)
                    put("summary", complaint.aiSummary ?: complaint.description)
                    put("service", complaint.service?.name ?: "")
                    put("createdAt", complaint.createdAt ?: "")
                }
            )
        }
        return ChatToolResult(
            "list_my_complaints",
            JSONObject().apply {
                put("count", complaints.size)
                put("complaints", array)
            }
        )
    }

    private fun startComplaint(description: String): ChatToolResult {
        val text = description.trim()
        if (text.isBlank()) {
            return ChatToolResult(
                "start_complaint",
                JSONObject().put("error", "A complaint description is required.")
            )
        }
        return ChatToolResult(
            name = "start_complaint",
            response = JSONObject().apply {
                put("status", "intake_ready")
                put("message", "The citizen can now continue in the official complaint form with this description pre-filled.")
                put("description", text)
            },
            forcedAction = ChatAction(
                kind = ChatActionKind.START_COMPLAINT,
                label = "Continue in complaint form",
                payload = text
            )
        )
    }

    internal fun complaintDetailsToJson(details: ComplaintTrackingDetails): JSONObject {
        val complaint: ComplaintRecord = details.complaint
        val history = JSONArray()
        details.history.takeLast(5).forEach { entry ->
            history.put(
                JSONObject().apply {
                    put("status", entry.status)
                    put("note", entry.note ?: "")
                    put("at", entry.createdAt)
                }
            )
        }
        val updates = JSONArray()
        details.updates.takeLast(3).forEach { update ->
            updates.put(
                JSONObject().apply {
                    put("message", update.message)
                    put("by", update.officialName ?: "Nagpur Municipal Corporation")
                    put("at", update.createdAt)
                }
            )
        }

        return JSONObject().apply {
            put("found", true)
            put("complaintNumber", complaint.complaintNumber)
            put("status", complaint.status)
            put("priority", complaint.priority)
            put("summary", complaint.aiSummary ?: complaint.description)
            put("service", complaint.service?.name ?: "")
            put("department", complaint.department?.name ?: "")
            put("location", complaint.locationText)
            put("createdAt", complaint.createdAt ?: "")
            put("assignedTeam", details.assignedTeam ?: "")
            put("resolutionNote", details.resolutionNote ?: "")
            put("resolvedAt", details.resolvedAt ?: "")
            put("recentStatusHistory", history)
            put("recentUpdates", updates)
        }
    }

    companion object {
        /**
         * Light synonym expansion so the model can pass colloquial English, Marathi or Hindi
         * phrasings and still land on the right catalogue entry.
         */
        private val SYNONYMS: Map<String, List<String>> = mapOf(
            "pothole" to listOf("road", "pothole", "repair", "asphalt", "crater", "khadda", "gaddha"),
            "garbage" to listOf("garbage", "waste", "trash", "dump", "kachra", "sweeping", "sanitation"),
            "water" to listOf("water", "leak", "pipeline", "supply", "paani", "tanker", "drain"),
            "light" to listOf("streetlight", "light", "lamp", "electrical", "dark"),
            "noise" to listOf("noise", "loud", "speaker", "sound", "pollution", "awaaz"),
            "drain" to listOf("drain", "sewer", "gutter", "nala", "overflow", "drainage"),
            "tree" to listOf("tree", "branch", "fallen", "green"),
            "stray" to listOf("stray", "dog", "cattle", "animal"),
            "encroach" to listOf("encroachment", "hawker", "illegal", "vendor"),
            "permit" to listOf("certificate", "permit", "licence", "license", "registration", "birth", "death")
        )

        /**
         * Scores catalogue entries against a free-text query and returns the best [limit] matches.
         * Exposed for unit tests — the ranking is the only non-obvious logic in this class.
         */
        internal fun rankServices(
            services: List<CivicService>,
            query: String,
            limit: Int = 5
        ): List<CivicService> {
            val normalized = query.lowercase()
            val tokens = normalized
                .split(Regex("[^\\p{L}\\p{N}]+"))
                .filter { it.length >= 3 }
                .distinct()

            val expanded = tokens.toMutableSet()
            tokens.forEach { token ->
                SYNONYMS.entries.forEach { (key, values) ->
                    if (values.any { it == token } || key == token) {
                        expanded.add(key)
                        expanded.addAll(values)
                    }
                }
            }

            return services
                .map { service -> service to scoreService(service, normalized, expanded) }
                .filter { it.second > 0 }
                .sortedWith(
                    compareByDescending<Pair<CivicService, Int>> { it.second }
                        .thenByDescending { it.first.popularScore }
                        .thenBy { it.first.name }
                )
                .take(limit)
                .map { it.first }
        }

        private fun scoreService(
            service: CivicService,
            normalizedQuery: String,
            terms: Set<String>
        ): Int {
            val name = service.name.lowercase()
            val category = service.category?.name?.lowercase().orEmpty()
            val department = service.department?.name?.lowercase().orEmpty()
            val description = (service.description ?: service.processDescription.orEmpty()).lowercase()

            var score = 0
            if (name.contains(normalizedQuery)) score += 6

            terms.forEach { term ->
                if (name.contains(term)) score += 4
                if (category.contains(term)) score += 3
                if (department.contains(term)) score += 3
                if (description.contains(term)) score += 1
            }
            return score
        }
    }
}
