package com.felipechaux.jarvis.feature.sessions.presentation.state

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.network.RpcConnection
import com.felipechaux.jarvis.feature.sessions.domain.model.LiveSession
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionEvent

data class SessionsState(
    val connection: RpcConnection = RpcConnection.DISCONNECTED,
    val sessions: List<LiveSession> = emptyList(),
    val feed: List<SessionEvent> = emptyList(),
    val error: AppError? = null,
)

sealed interface SessionsAction {
    data class OnAnswer(val sessionName: String, val promptId: String, val key: String) : SessionsAction
    data object OnRefresh : SessionsAction
}
