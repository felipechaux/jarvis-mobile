package com.felipechaux.jarvis.feature.sessions.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipechaux.jarvis.core.model.onError
import com.felipechaux.jarvis.core.model.onSuccess
import com.felipechaux.jarvis.core.network.RpcConnection
import com.felipechaux.jarvis.feature.sessions.domain.repository.SessionsRepository
import com.felipechaux.jarvis.feature.sessions.presentation.state.SessionsAction
import com.felipechaux.jarvis.feature.sessions.presentation.state.SessionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SessionsViewModel(private val repository: SessionsRepository) : ViewModel() {

    private val _state = MutableStateFlow(SessionsState())
    val state: StateFlow<SessionsState> = _state.asStateFlow()

    init {
        repository.connect()
        viewModelScope.launch {
            repository.connection.collect { connection ->
                _state.update { it.copy(connection = connection) }
                if (connection == RpcConnection.CONNECTED) refresh()
            }
        }
        viewModelScope.launch {
            repository.events.collect { event ->
                _state.update { it.copy(feed = (listOf(event) + it.feed).take(FEED_SIZE)) }
                refresh()
            }
        }
        // Session status (working / idle / prompt) changes without events.
        viewModelScope.launch {
            while (isActive) {
                delay(POLL_MS)
                if (_state.value.connection == RpcConnection.CONNECTED) refresh()
            }
        }
    }

    fun onAction(action: SessionsAction) {
        when (action) {
            is SessionsAction.OnAnswer -> viewModelScope.launch {
                repository.answer(action.sessionName, action.promptId, action.key)
                    .onError { error -> _state.update { it.copy(error = error) } }
                refresh()
            }
            SessionsAction.OnRefresh -> viewModelScope.launch { refresh() }
        }
    }

    private suspend fun refresh() {
        repository.liveSessions()
            .onSuccess { sessions -> _state.update { it.copy(sessions = sessions, error = null) } }
            .onError { error -> _state.update { it.copy(error = error) } }
    }

    override fun onCleared() {
        repository.disconnect()
    }

    private companion object {
        const val FEED_SIZE = 50
        const val POLL_MS = 5_000L
    }
}
