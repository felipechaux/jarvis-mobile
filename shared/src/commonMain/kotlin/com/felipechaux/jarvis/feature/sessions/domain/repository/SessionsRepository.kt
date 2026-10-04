package com.felipechaux.jarvis.feature.sessions.domain.repository

import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.network.RpcConnection
import com.felipechaux.jarvis.feature.sessions.domain.model.LiveSession
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SessionsRepository {
    val connection: StateFlow<RpcConnection>
    val events: Flow<SessionEvent>

    fun connect()
    fun disconnect()

    suspend fun liveSessions(): Resource<List<LiveSession>>
    suspend fun answer(sessionName: String, promptId: String, key: String): Resource<Unit>
    suspend fun sendMessage(sessionName: String, text: String): Resource<Unit>
    suspend fun registerPushToken(token: String): Resource<Unit>
}
