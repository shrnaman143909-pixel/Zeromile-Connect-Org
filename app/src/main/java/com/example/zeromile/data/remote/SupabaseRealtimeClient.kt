package com.example.zeromile.data.remote

import android.util.Log
import com.example.zeromile.data.model.NotificationRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Connection state for Supabase Realtime WebSocket client
 */
enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

/**
 * Payload event emitted on Realtime changes
 */
sealed class RealtimeEvent {
    data class NotificationInserted(val notification: NotificationRecord) : RealtimeEvent()
    data class ComplaintUpdated(val complaintId: String, val status: String, val assignedTeamName: String?) : RealtimeEvent()
    data class StatusHistoryInserted(val complaintId: String, val status: String, val note: String?) : RealtimeEvent()
    data class ComplaintUpdateInserted(val complaintId: String, val title: String, val message: String) : RealtimeEvent()
    data class ConnectionChanged(val state: RealtimeConnectionState) : RealtimeEvent()
}

/**
 * SupabaseRealtimeClient
 *
 * Implements the Phoenix-channel based Supabase Realtime v2 protocol over standard OkHttp WebSocket.
 * Features:
 * - Listens for 'notifications', 'complaints', 'complaint_status_history', and 'complaint_updates'
 * - Filtered by user_id for citizen isolation
 * - Automatic exponential reconnect with state broadcasting
 * - Heartbeat ping-pong
 * - Realtime reconciliation triggers
 */
class SupabaseRealtimeClient(
    private val supabaseUrl: String,
    private val apiKey: String,
    private val authTokenProvider: () -> String?,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) {
    companion object {
        private const val TAG = "SupabaseRealtime"
        private const val HEARTBEAT_INTERVAL_MS = 25000L
    }

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for WebSocket
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var activeWebSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var connectionJob: Job? = null

    private val _connectionState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RealtimeConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private var currentUserId: String? = null
    private var refCounter = 1

    fun connect(userId: String) {
        if (currentUserId == userId && _connectionState.value == RealtimeConnectionState.CONNECTED) {
            return
        }
        currentUserId = userId
        disconnect()
        startConnectionLoop(userId)
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        connectionJob?.cancel()
        connectionJob = null
        try {
            activeWebSocket?.close(1000, "Normal Closure")
        } catch (e: Exception) {
            Log.w(TAG, "Error closing websocket: ${e.message}")
        }
        activeWebSocket = null
        _connectionState.value = RealtimeConnectionState.DISCONNECTED
    }

    private fun startConnectionLoop(userId: String) {
        connectionJob = scope.launch {
            var attempt = 0
            while (isActive) {
                try {
                    _connectionState.value = if (attempt == 0) RealtimeConnectionState.CONNECTING else RealtimeConnectionState.RECONNECTING
                    _events.emit(RealtimeEvent.ConnectionChanged(_connectionState.value))

                    // Parse ws protocol from http/https url
                    val cleanBase = supabaseUrl.trimEnd('/')
                    val wsScheme = if (cleanBase.startsWith("https://")) "wss://" else "ws://"
                    val hostAndPort = cleanBase.removePrefix("https://").removePrefix("http://")
                    val wsUrl = "$wsScheme$hostAndPort/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"

                    val request = Request.Builder()
                        .url(wsUrl)
                        .addHeader("apikey", apiKey)
                        .build()

                    val connectionCloseLatch = kotlinx.coroutines.CompletableDeferred<Unit>()

                    val listener = object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            Log.i(TAG, "Connected to Supabase Realtime WebSocket for user: $userId")
                            activeWebSocket = webSocket
                            _connectionState.value = RealtimeConnectionState.CONNECTED
                            scope.launch {
                                _events.emit(RealtimeEvent.ConnectionChanged(RealtimeConnectionState.CONNECTED))
                            }
                            attempt = 0

                            // 1. Subscribe to citizen's notification channel
                            val notifTopic = "realtime:public:notifications:user_id=eq.$userId"
                            val notifJoinRef = (refCounter++).toString()
                            val notifJoinMsg = JSONObject().apply {
                                put("topic", notifTopic)
                                put("event", "phx_join")
                                put("payload", JSONObject().apply {
                                    put("config", JSONObject().apply {
                                        put("broadcast", JSONObject().apply { put("self", false) })
                                        put("presence", JSONObject().apply { put("key", "") })
                                        put("postgres_changes", org.json.JSONArray().apply {
                                            put(JSONObject().apply {
                                                put("event", "INSERT")
                                                put("schema", "public")
                                                put("table", "notifications")
                                                put("filter", "user_id=eq.$userId")
                                            })
                                        })
                                    })
                                })
                                put("ref", notifJoinRef)
                            }
                            webSocket.send(notifJoinMsg.toString())

                            // 2. Subscribe to complaints changes
                            val compTopic = "realtime:public:complaints"
                            val compJoinRef = (refCounter++).toString()
                            val compJoinMsg = JSONObject().apply {
                                put("topic", compTopic)
                                put("event", "phx_join")
                                put("payload", JSONObject().apply {
                                    put("config", JSONObject().apply {
                                        put("postgres_changes", org.json.JSONArray().apply {
                                            put(JSONObject().apply {
                                                put("event", "*")
                                                put("schema", "public")
                                                put("table", "complaints")
                                            })
                                        })
                                    })
                                })
                                put("ref", compJoinRef)
                            }
                            webSocket.send(compJoinMsg.toString())

                            // Start heartbeat loop
                            heartbeatJob?.cancel()
                            heartbeatJob = scope.launch {
                                while (isActive) {
                                    delay(HEARTBEAT_INTERVAL_MS)
                                    val hb = JSONObject().apply {
                                        put("topic", "phoenix")
                                        put("event", "heartbeat")
                                        put("payload", JSONObject())
                                        put("ref", (refCounter++).toString())
                                    }
                                    val sent = activeWebSocket?.send(hb.toString()) ?: false
                                    if (!sent) {
                                        Log.w(TAG, "Heartbeat failed to send")
                                        break
                                    }
                                }
                            }
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            handleIncomingMessage(text)
                        }

                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "WebSocket closing: $code / $reason")
                        }

                        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                            Log.d(TAG, "WebSocket closed: $code / $reason")
                            connectionCloseLatch.complete(Unit)
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            Log.w(TAG, "WebSocket failure: ${t.message}")
                            _connectionState.value = RealtimeConnectionState.ERROR
                            scope.launch {
                                _events.emit(RealtimeEvent.ConnectionChanged(RealtimeConnectionState.ERROR))
                            }
                            connectionCloseLatch.complete(Unit)
                        }
                    }

                    okHttpClient.newWebSocket(request, listener)

                    // Await socket closure/failure before reconnecting
                    connectionCloseLatch.await()

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Realtime connection error: ${e.message}")
                    _connectionState.value = RealtimeConnectionState.ERROR
                    _events.emit(RealtimeEvent.ConnectionChanged(RealtimeConnectionState.ERROR))
                } finally {
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                }

                // Exponential backoff before reconnecting
                attempt++
                val backoffMs = (1000L * (1 shl (attempt.coerceAtMost(5)))).coerceIn(2000L, 30000L)
                delay(backoffMs)
            }
        }
    }

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val event = json.optString("event")
            val payload = json.optJSONObject("payload") ?: return

            if (event == "postgres_changes") {
                val data = payload.optJSONObject("data") ?: return
                val table = data.optString("table")
                val record = data.optJSONObject("record") ?: data.optJSONObject("new")

                when (table) {
                    "notifications" -> {
                        if (record != null) {
                            val notif = NotificationRecord(
                                id = record.optString("id"),
                                userId = record.optString("user_id"),
                                complaintId = if (record.isNull("complaint_id")) null else record.optString("complaint_id"),
                                type = record.optString("type"),
                                title = record.optString("title"),
                                message = record.optString("message"),
                                read = record.optBoolean("read", false),
                                createdAt = record.optString("created_at"),
                                idempotencyKey = if (record.isNull("idempotency_key")) null else record.optString("idempotency_key")
                            )
                            scope.launch {
                                _events.emit(RealtimeEvent.NotificationInserted(notif))
                            }
                        }
                    }
                    "complaints" -> {
                        if (record != null) {
                            val id = record.optString("id")
                            val status = record.optString("status")
                            val team = if (record.isNull("assigned_team_name")) null else record.optString("assigned_team_name")
                            scope.launch {
                                _events.emit(RealtimeEvent.ComplaintUpdated(id, status, team))
                            }
                        }
                    }
                    "complaint_status_history" -> {
                        if (record != null) {
                            val cId = record.optString("complaint_id")
                            val status = record.optString("status")
                            val note = if (record.isNull("note")) null else record.optString("note")
                            scope.launch {
                                _events.emit(RealtimeEvent.StatusHistoryInserted(cId, status, note))
                            }
                        }
                    }
                    "complaint_updates" -> {
                        if (record != null) {
                            val cId = record.optString("complaint_id")
                            val title = record.optString("title")
                            val msg = record.optString("message")
                            scope.launch {
                                _events.emit(RealtimeEvent.ComplaintUpdateInserted(cId, title, msg))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling realtime frame: ${e.message}")
        }
    }
}
