package com.felipechaux.jarvis.core.network

import com.felipechaux.jarvis.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

enum class RpcConnection { DISCONNECTED, CONNECTING, CONNECTED, UNAUTHORIZED, INCOMPATIBLE }

/** A server-pushed message: `{"event": name, "data": {...}}`. */
data class RpcEvent(val name: String, val data: JsonObject)

class RpcException(val code: String, override val message: String) : Exception(message)

/**
 * Client for the server's `/v1/mobile/ws` RPC channel.
 *
 * Keeps one connection alive while [start]ed, reconnecting with backoff,
 * and correlates responses to requests by `id`.  Server events are exposed
 * on [events]; after a reconnect, callers catch up with `sessions.events`.
 */
class MobileRpcClient(
    private val client: HttpClient,
    private val credentials: CredentialStore,
    private val resolver: EndpointResolver,
    private val scope: CoroutineScope,
) {
    private val _connection = MutableStateFlow(RpcConnection.DISCONNECTED)
    val connection: StateFlow<RpcConnection> = _connection.asStateFlow()

    private val _events = MutableSharedFlow<RpcEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<RpcEvent> = _events.asSharedFlow()

    private val pending = mutableMapOf<Int, CompletableDeferred<JsonElement>>()
    private val lock = Mutex()
    private var nextId = 1
    private var session: DefaultClientWebSocketSession? = null
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runForever() }
    }

    fun stop() {
        job?.cancel()
        job = null
        _connection.value = RpcConnection.DISCONNECTED
    }

    suspend fun call(method: String, params: JsonObject = JsonObject(emptyMap())): JsonElement {
        val ws = session ?: throw RpcException("offline", "not connected")
        val deferred = CompletableDeferred<JsonElement>()
        val id = lock.withLock {
            val id = nextId++
            pending[id] = deferred
            id
        }
        val request = buildJsonObject {
            put("id", id)
            put("method", method)
            put("params", params)
        }
        ws.send(Frame.Text(request.toString()))
        return try {
            withTimeout(15_000) { deferred.await() }
        } finally {
            lock.withLock { pending.remove(id) }
        }
    }

    private suspend fun runForever() {
        var backoffMs = 1_000L
        while (true) {
            val creds = credentials.current()
            if (creds == null) {
                _connection.value = RpcConnection.DISCONNECTED
                return
            }
            _connection.value = RpcConnection.CONNECTING
            try {
                val ws = client.webSocketSession(wsUrl(creds.baseUrl)) {
                    header(HttpHeaders.Authorization, "Bearer ${creds.token}")
                }
                session = ws
                backoffMs = 1_000L
                scope.launch { handshake() }
                for (frame in ws.incoming) {
                    if (frame is Frame.Text) dispatch(frame.readText())
                }
                // The server closes with 1008 when the token is no longer valid.
                if (ws.closeReason.await()?.code == POLICY_VIOLATION) {
                    _connection.value = RpcConnection.UNAUTHORIZED
                    return
                }
            } catch (e: CancellationException) {
                session?.close()
                throw e
            } catch (_: Exception) {
                // Unreachable or dropped; retry below.
            } finally {
                session = null
                failPending()
            }
            if (_connection.value == RpcConnection.INCOMPATIBLE) return
            _connection.value = RpcConnection.DISCONNECTED
            // The network may have changed (left home Wi-Fi): switch to
            // whichever paired address answers before retrying.
            resolver.failover()
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
        }
    }

    private suspend fun handshake() {
        try {
            call("hello", buildJsonObject { put("protocol", PROTOCOL_VERSION) })
            _connection.value = RpcConnection.CONNECTED
        } catch (e: RpcException) {
            if (e.code == "incompatible_protocol") {
                _connection.value = RpcConnection.INCOMPATIBLE
                session?.close()
            }
        } catch (_: Exception) {
            // The connection dropped mid-handshake; runForever reconnects.
        }
    }

    private suspend fun dispatch(text: String) {
        val message = runCatching { JarvisJson.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val event = message["event"]?.jsonPrimitive?.contentOrNull
        if (event != null) {
            val data = message["data"] as? JsonObject ?: JsonObject(emptyMap())
            _events.emit(RpcEvent(event, data))
            return
        }
        val id = message["id"]?.jsonPrimitive?.intOrNull ?: return
        val deferred = lock.withLock { pending.remove(id) } ?: return
        val error = message["error"] as? JsonObject
        if (error != null) {
            deferred.completeExceptionally(
                RpcException(
                    code = error["code"]?.jsonPrimitive?.contentOrNull ?: "error",
                    message = error["message"]?.jsonPrimitive?.contentOrNull ?: "",
                ),
            )
        } else {
            deferred.complete(message["result"] ?: JsonNull)
        }
    }

    private suspend fun failPending() {
        lock.withLock {
            pending.values.forEach { it.completeExceptionally(RpcException("offline", "connection lost")) }
            pending.clear()
        }
    }

    private companion object {
        const val POLICY_VIOLATION: Short = 1008
    }
}

/** `http://host:8000` → `ws://host:8000/v1/mobile/ws` (https → wss). */
fun wsUrl(baseUrl: String): String {
    val base = baseUrl.trimEnd('/')
    val ws = when {
        base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
        base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
        else -> base
    }
    return "$ws/v1/mobile/ws"
}
