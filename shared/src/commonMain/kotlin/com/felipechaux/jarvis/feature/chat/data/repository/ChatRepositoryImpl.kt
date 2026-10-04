package com.felipechaux.jarvis.feature.chat.data.repository

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.core.network.EndpointResolver
import com.felipechaux.jarvis.core.storage.CredentialStore
import com.felipechaux.jarvis.feature.chat.data.mapper.ChatStreamMapper
import com.felipechaux.jarvis.feature.chat.data.remote.ChatApi
import com.felipechaux.jarvis.feature.chat.data.remote.ChatRequestDto
import com.felipechaux.jarvis.feature.chat.data.remote.ChatStreamException
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.StreamUpdate
import com.felipechaux.jarvis.feature.chat.domain.repository.ChatRepository
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.io.IOException

/**
 * Chat goes through the same endpoint as the desktop, so the server applies
 * the agent, tools, persona, memory and history window.
 *
 * If the active address is dead before anything streamed (the phone left
 * home Wi-Fi), the reply is retried once on whichever paired address answers.
 */
class ChatRepositoryImpl(
    private val api: ChatApi,
    private val store: CredentialStore,
    private val resolver: EndpointResolver,
) : ChatRepository {

    private var model: String? = null

    override fun streamReply(history: List<ChatMessage>): Flow<StreamUpdate> = flow {
        val creds = store.current()
        if (creds == null) {
            emit(StreamUpdate.Failed(AppError.Unauthorized))
            return@flow
        }
        var emitted = false
        val collector = FlowCollector<StreamUpdate> {
            emitted = true
            emit(it)
        }
        try {
            try {
                stream(creds, history, collector)
            } catch (e: IOException) {
                if (emitted) throw e
                resolver.failover() ?: throw e
                stream(store.current() ?: throw e, history, collector)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ChatStreamException) {
            emit(StreamUpdate.Failed(e.error))
        } catch (e: HttpRequestTimeoutException) {
            emit(StreamUpdate.Failed(AppError.Unreachable))
        } catch (e: IOException) {
            emit(StreamUpdate.Failed(AppError.Unreachable))
        } catch (e: Exception) {
            emit(StreamUpdate.Failed(AppError.Unknown(e)))
        }
    }

    private suspend fun stream(
        creds: ServerCredentials,
        history: List<ChatMessage>,
        collector: FlowCollector<StreamUpdate>,
    ) {
        val model = model ?: api.serverInfo(creds).model.also { model = it }
        val request = ChatRequestDto(model = model, messages = history.map(ChatStreamMapper::toDto))
        api.stream(creds, request)
            .mapNotNull(ChatStreamMapper::toUpdate)
            .collect(collector)
    }
}
