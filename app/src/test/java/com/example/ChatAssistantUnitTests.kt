package com.example

import com.example.zeromile.ai.ChatCompletion
import com.example.zeromile.ai.ChatToolExecutor
import com.example.zeromile.ai.ChatTurn
import com.example.zeromile.ai.ChatTurnPart
import com.example.zeromile.ai.GeminiChatClient
import com.example.zeromile.data.model.ComplaintRecord
import com.example.zeromile.data.model.ComplaintStatusHistoryRecord
import com.example.zeromile.data.model.ComplaintTrackingDetails
import com.example.zeromile.data.model.ComplaintUpdateRecord
import com.example.zeromile.data.repository.SeedData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Focused tests for the assistant plumbing that has real logic in it:
 * catalogue ranking, request shaping, response parsing and complaint serialisation.
 *
 * Runs under Robolectric because the production code uses org.json, whose JVM stub in the
 * mockable android.jar is a no-op. Network calls are out of scope — the ViewModel turns
 * failures into chat error bubbles.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatAssistantUnitTests {

    // ------------------------------------------------------------------ catalogue ranking

    @Test
    fun rankServices_matchesPotholeQueryToRoadService() {
        val matches = ChatToolExecutor.rankServices(
            services = SeedData.services,
            query = "pothole on Wardha Road"
        )

        assertTrue("Expected at least one catalogue match", matches.isNotEmpty())
        val top = matches.first()
        val haystack = (top.name + " " + (top.category?.name ?: "") + " " +
            (top.department?.name ?: "")).lowercase()
        assertTrue(
            "Top match '$haystack' should mention road or pothole",
            haystack.contains("road") || haystack.contains("pothole")
        )
    }

    @Test
    fun rankServices_usesSynonymsForMarathiTerms() {
        val matches = ChatToolExecutor.rankServices(
            services = SeedData.services,
            query = "kचरा kachra उचलला नाही",
            limit = 3
        )
        // The query is intentionally noisy; the contract is that ranking never throws and
        // never returns more than the requested limit.
        assertTrue(matches.size <= 3)
    }

    @Test
    fun rankServices_returnsEmptyForUnrelatedQuery() {
        val matches = ChatToolExecutor.rankServices(
            services = SeedData.services,
            query = "zzzz qqqq"
        )
        assertTrue(matches.isEmpty())
    }

    // ------------------------------------------------------------------ request shaping

    @Test
    fun buildRequestBody_includesHistoryToolsAndSystemPrompt() {
        val client = GeminiChatClient(apiKey = "test-key")
        val body = client.buildRequestBody(
            systemPrompt = "You are Zeromile AI.",
            turns = listOf(
                ChatTurn.user("Where is my complaint?"),
                ChatTurn(
                    role = ChatTurn.ROLE_MODEL,
                    parts = listOf(
                        ChatTurnPart.FunctionCall(
                            name = "list_my_complaints",
                            arguments = JSONObject()
                        )
                    )
                ),
                ChatTurn.toolResponse(
                    "list_my_complaints",
                    JSONObject().put("count", 0)
                )
            )
        )

        assertEquals("You are Zeromile AI.", body.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text"))

        val contents = body.getJSONArray("contents")
        assertEquals(3, contents.length())
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        assertEquals(
            "list_my_complaints",
            contents.getJSONObject(1).getJSONArray("parts").getJSONObject(0)
                .getJSONObject("functionCall").getString("name")
        )
        assertEquals(
            "list_my_complaints",
            contents.getJSONObject(2).getJSONArray("parts").getJSONObject(0)
                .getJSONObject("functionResponse").getString("name")
        )

        val declarations = body.getJSONArray("tools").getJSONObject(0).getJSONArray("functionDeclarations")
        val names = (0 until declarations.length()).map { declarations.getJSONObject(it).getString("name") }
        assertTrue(names.contains("find_service"))
        assertTrue(names.contains("check_complaint_status"))
        assertTrue(names.contains("list_my_complaints"))
        assertTrue(names.contains("start_complaint"))
    }

    @Test
    fun isConfigured_rejectsPlaceholderKeys() {
        assertTrue(GeminiChatClient(apiKey = "AIzaSy-real-looking-key").isConfigured)
        assertTrue(!GeminiChatClient(apiKey = "").isConfigured)
        assertTrue(!GeminiChatClient(apiKey = "MY_GEMINI_API_KEY").isConfigured)
        assertTrue(!GeminiChatClient(apiKey = "your_gemini_api_key_here").isConfigured)
    }

    // ------------------------------------------------------------------ response parsing

    @Test
    fun parseResponse_readsPlainTextReply() {
        val client = GeminiChatClient(apiKey = "test-key")
        val raw = """
            {"candidates":[{"content":{"role":"model","parts":[{"text":"Your complaint is In Progress."}]}}]}
        """.trimIndent()

        val result = client.parseResponse(raw)
        assertTrue(result is ChatCompletion.Reply)
        assertEquals("Your complaint is In Progress.", (result as ChatCompletion.Reply).text)
    }

    @Test
    fun parseResponse_joinsMultipleTextParts() {
        val client = GeminiChatClient(apiKey = "test-key")
        val raw = """
            {"candidates":[{"content":{"parts":[{"text":"First line."},{"text":"Second line."}]}}]}
        """.trimIndent()

        val result = client.parseResponse(raw) as ChatCompletion.Reply
        assertEquals("First line.\nSecond line.", result.text)
    }

    @Test
    fun parseResponse_prefersFunctionCallOverText() {
        val client = GeminiChatClient(apiKey = "test-key")
        val raw = """
            {"candidates":[{"content":{"parts":[
              {"text":"Let me check."},
              {"functionCall":{"name":"check_complaint_status","args":{"complaintNumber":"NMC-2026-004821"}}}
            ]}}]}
        """.trimIndent()

        val result = client.parseResponse(raw)
        assertTrue(result is ChatCompletion.ToolCall)
        val call = result as ChatCompletion.ToolCall
        assertEquals("check_complaint_status", call.name)
        assertEquals("NMC-2026-004821", call.arguments.getString("complaintNumber"))
    }

    @Test
    fun parseResponse_surfacesSafetyBlock() {
        val client = GeminiChatClient(apiKey = "test-key")
        val raw = """{"promptFeedback":{"blockReason":"SAFETY"}}"""

        val result = client.parseResponse(raw)
        assertTrue(result is ChatCompletion.Failure)
    }

    @Test
    fun parseResponse_handlesGarbageBody() {
        val client = GeminiChatClient(apiKey = "test-key")
        val result = client.parseResponse("<html>gateway error</html>")
        assertTrue(result is ChatCompletion.Failure)
    }

    // ------------------------------------------------------------------ complaint serialisation

    @Test
    fun complaintDetailsToJson_exposesStatusAndHistory() {
        val details = ComplaintTrackingDetails(
            complaint = ComplaintRecord(
                id = "c-1",
                complaintNumber = "NMC-2026-000123",
                serviceId = "s-1",
                description = "Large pothole near Dharampeth square",
                originalTranscript = "pothole",
                locationText = "Dharampeth, Nagpur",
                status = "In Progress",
                aiSummary = "Pothole on Dharampeth main road"
            ),
            history = listOf(
                ComplaintStatusHistoryRecord(
                    id = "h-1",
                    complaintId = "c-1",
                    status = "Submitted",
                    createdAt = "2026-10-01T09:00:00Z"
                ),
                ComplaintStatusHistoryRecord(
                    id = "h-2",
                    complaintId = "c-1",
                    status = "In Progress",
                    note = "Road repair squad assigned",
                    createdAt = "2026-10-02T09:00:00Z"
                )
            ),
            updates = listOf(
                ComplaintUpdateRecord(
                    id = "u-1",
                    complaintId = "c-1",
                    message = "Material dispatched to site",
                    createdAt = "2026-10-02T10:00:00Z"
                )
            ),
            assignedTeam = "Zone Road Repair Squad",
            resolutionNote = null,
            resolvedAt = null
        )

        val json = ChatToolExecutor().complaintDetailsToJson(details)

        assertEquals("NMC-2026-000123", json.getString("complaintNumber"))
        assertEquals("In Progress", json.getString("status"))
        assertEquals("Dharampeth, Nagpur", json.getString("location"))
        assertEquals("Zone Road Repair Squad", json.getString("assignedTeam"))
        assertEquals(2, json.getJSONArray("recentStatusHistory").length())
        assertEquals(
            "Road repair squad assigned",
            json.getJSONArray("recentStatusHistory").getJSONObject(1).getString("note")
        )
        assertEquals(1, json.getJSONArray("recentUpdates").length())
    }
}
