package com.felipechaux.jarvis.feature.chat.data.repository

import com.felipechaux.jarvis.core.storage.CredentialStore
import com.felipechaux.jarvis.feature.chat.data.local.dao.ChatDao
import com.felipechaux.jarvis.feature.chat.data.local.entity.ConversationEntity
import com.felipechaux.jarvis.feature.chat.data.mapper.ChatStreamMapper
import com.felipechaux.jarvis.feature.chat.data.mapper.titleFor
import com.felipechaux.jarvis.feature.chat.data.mapper.toDomain
import com.felipechaux.jarvis.feature.chat.data.mapper.toEntity
import com.felipechaux.jarvis.feature.chat.data.remote.ChatHistoryApi
import com.felipechaux.jarvis.feature.chat.data.remote.IndexedConversationDto
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.Conversation
import com.felipechaux.jarvis.feature.chat.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ChatHistoryRepositoryImpl(
    private val dao: ChatDao,
    private val api: ChatHistoryApi,
    private val store: CredentialStore,
) : ChatHistoryRepository {

    override fun observeConversations(): Flow<List<Conversation>> =
        dao.observeConversations().map { list -> list.map { it.toDomain() } }

    override suspend fun messages(conversationId: String): List<ChatMessage> =
        dao.messages(conversationId).map { it.toDomain() }

    @OptIn(ExperimentalTime::class)
    override suspend fun save(conversationId: String, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        val conversation = ConversationEntity(
            id = conversationId,
            title = titleFor(messages),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
        dao.saveTranscript(conversation, messages.mapIndexed { i, m -> m.toEntity(conversationId, i) })
        val creds = store.current() ?: return
        try {
            api.index(
                creds,
                IndexedConversationDto(
                    id = "mobile-$conversationId",
                    title = conversation.title,
                    updatedAt = conversation.updatedAt,
                    messages = messages.map(ChatStreamMapper::toDto),
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline: the next save of this conversation re-indexes it whole.
        }
    }

    override suspend fun delete(conversationId: String) = dao.deleteConversation(conversationId)
}
