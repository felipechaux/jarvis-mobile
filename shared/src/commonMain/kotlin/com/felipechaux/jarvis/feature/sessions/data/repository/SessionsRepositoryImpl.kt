package com.felipechaux.jarvis.feature.sessions.data.repository

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.network.JarvisJson
import com.felipechaux.jarvis.core.network.MobileRpcClient
import com.felipechaux.jarvis.core.network.RpcConnection
import com.felipechaux.jarvis.core.network.RpcException
import com.felipechaux.jarvis.feature.sessions.data.mapper.toDomain
import com.felipechaux.jarvis.feature.sessions.data.remote.LiveSessionsDto
import com.felipechaux.jarvis.feature.sessions.data.remote.OkDto
import com.felipechaux.jarvis.feature.sessions.data.remote.SessionEventDto
import com.felipechaux.jarvis.feature.sessions.domain.model.LiveSession
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionEvent
import com.felipechaux.jarvis.feature.sessions.domain.repository.SessionsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

class SessionsRepositoryImpl(private val rpc: MobileRpcClient) : SessionsRepository {

    override val connection: StateFlow<RpcConnection> = rpc.connection

    override val events: Flow<SessionEvent> = rpc.events
        .filter { it.name == "session_event" }
        .mapNotNull { runCatching { JarvisJson.decodeFromJsonElement<SessionEventDto>(it.data).toDomain() }.getOrNull() }

    override fun connect() = rpc.start()

    override fun disconnect() = rpc.stop()

    override suspend fun liveSessions(): Resource<List<LiveSession>> =
        rpcCall("sessions.live") { JarvisJson.decodeFromJsonElement<LiveSessionsDto>(it).sessions.map { s -> s.toDomain() } }

    override suspend fun answer(sessionName: String, promptId: String, key: String): Resource<Unit> =
        okCall(
            "sessions.answer",
            buildJsonObject {
                put("name", sessionName)
                put("prompt_id", promptId)
                put("key", key)
            },
        )

    override suspend fun sendMessage(sessionName: String, text: String): Resource<Unit> =
        okCall(
            "sessions.message",
            buildJsonObject {
                put("name", sessionName)
                put("text", text)
            },
        )

    override suspend fun registerPushToken(token: String): Resource<Unit> =
        rpcCall("push.register", buildJsonObject {
            put("provider", "fcm")
            put("token", token)
        }) { }

    private suspend fun okCall(method: String, params: JsonObject): Resource<Unit> =
        when (val result = rpcCall(method, params) { JarvisJson.decodeFromJsonElement<OkDto>(it) }) {
            is Resource.Error -> result
            is Resource.Success ->
                if (result.data.ok) Resource.Success(Unit)
                else Resource.Error(AppError.Server(200, result.data.error.orEmpty()))
        }

    private suspend fun <T> rpcCall(
        method: String,
        params: JsonObject = JsonObject(emptyMap()),
        decode: (JsonElement) -> T,
    ): Resource<T> = try {
        Resource.Success(decode(rpc.call(method, params)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: RpcException) {
        Resource.Error(
            when (e.code) {
                "offline" -> AppError.Unreachable
                "incompatible_protocol" -> AppError.IncompatibleProtocol(e.message)
                else -> AppError.Server(0, e.message)
            },
        )
    } catch (e: Exception) {
        Resource.Error(AppError.Unknown(e))
    }
}
