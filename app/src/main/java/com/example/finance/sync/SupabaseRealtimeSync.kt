package com.example.finance.sync

import android.util.Log
import com.example.finance.auth.SupabaseAuthRepository
import com.example.finance.supabase.SupabaseConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Live Supabase Postgres Changes transport. Room mutation belongs to the sync coordinator. */
class SupabaseRealtimeSync(
    private val auth: SupabaseAuthRepository,
    private val onDatabaseChange: suspend (table: String, event: String, record: JSONObject, oldRecord: JSONObject) -> Unit,
    private val onStateChanged: (State) -> Unit = {}
) {
    enum class State { STOPPED, CONNECTING, SUBSCRIBED, DISCONNECTED, ERROR }

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val refCounter = AtomicLong(0L)
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    @Volatile private var stopped = true
    @Volatile private var joinRef = "1"

    fun start() {
        if (!stopped) return
        stopped = false
        connect()
    }

    fun stop() {
        stopped = true
        reconnectJob?.cancel()
        heartbeatJob?.cancel()
        socket?.close(1000, "client stop")
        socket = null
        onStateChanged(State.STOPPED)
    }

    private fun connect() {
        if (stopped) return
        scope.launch {
            if (!auth.restoreSession()) {
                onStateChanged(State.ERROR)
                scheduleReconnect()
                return@launch
            }
            val token = auth.currentAccessToken()
            if (token.isNullOrBlank()) {
                onStateChanged(State.ERROR)
                scheduleReconnect()
                return@launch
            }
            val url = SupabaseConnection.url.trimEnd('/')
                .replaceFirst("https://", "wss://")
                .replaceFirst("http://", "ws://") +
                "/realtime/v1/websocket?apikey=${SupabaseConnection.publishableKey}&vsn=1.0.0"
            onStateChanged(State.CONNECTING)
            socket = client.newWebSocket(Request.Builder().url(url).build(), Listener(token))
        }
    }

    private fun join(ws: WebSocket, accessToken: String) {
        joinRef = nextRef()
        val payload = JSONObject()
            .put("access_token", accessToken)
            .put("config", JSONObject()
                .put("private", false)
                .put("broadcast", JSONObject().put("ack", false).put("self", false))
                .put("presence", JSONObject().put("enabled", false))
                .put("postgres_changes", JSONArray().put(
                    JSONObject().put("event", "*").put("schema", "public").put("table", "*")
                )))
        ws.send(JSONObject()
            .put("event", "phx_join")
            .put("topic", "realtime:bye-pro-finance")
            .put("payload", payload)
            .put("ref", joinRef)
            .put("join_ref", joinRef)
            .toString())
    }

    private fun heartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && !stopped) {
                delay(20_000L)
                ws.send(JSONObject()
                    .put("event", "heartbeat")
                    .put("topic", "phoenix")
                    .put("payload", JSONObject())
                    .put("ref", nextRef())
                    .toString())
            }
        }
    }

    private fun handleMessage(text: String) {
        val message = runCatching { JSONObject(text) }.getOrNull() ?: return
        when (message.optString("event")) {
            "phx_reply" -> if (message.optString("ref") == joinRef &&
                message.optJSONObject("payload")?.optString("status") == "ok") {
                onStateChanged(State.SUBSCRIBED)
            }
            "postgres_changes" -> {
                val data = message.optJSONObject("payload")?.optJSONObject("data") ?: return
                val table = data.optString("table")
                if (table.isBlank()) return
                val event = data.optString("type", "*")
                val record = data.optJSONObject("record") ?: JSONObject()
                val oldRecord = data.optJSONObject("old_record") ?: JSONObject()
                scope.launch {
                    runCatching { onDatabaseChange(table, event, record, oldRecord) }
                        .onFailure { Log.e(TAG, "Realtime consumer failed: $table/$event", it) }
                }
            }
            "phx_error", "phx_close" -> {
                onStateChanged(State.DISCONNECTED)
                scheduleReconnect()
            }
        }
    }

    private fun scheduleReconnect() {
        if (stopped || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(2_000L)
            if (!stopped) connect()
        }
    }

    private fun nextRef(): String = refCounter.incrementAndGet().toString()

    private inner class Listener(private val accessToken: String) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            join(webSocket, accessToken)
            heartbeat(webSocket)
        }

        override fun onMessage(webSocket: WebSocket, text: String) = handleMessage(text)

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            heartbeatJob?.cancel()
            socket = null
            onStateChanged(State.ERROR)
            Log.w(TAG, "Supabase Realtime connection failed", t)
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            heartbeatJob?.cancel()
            socket = null
            if (!stopped) {
                onStateChanged(State.DISCONNECTED)
                scheduleReconnect()
            }
        }
    }

    companion object { private const val TAG = "SupabaseRealtimeSync" }
}
