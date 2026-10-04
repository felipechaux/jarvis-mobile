package com.felipechaux.jarvis.feature.chat.domain.repository

import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.Conversation
import kotlinx.coroutines.flow.Flow

interface ChatHistoryRepository {
    fun observeConversations(): Flow<List<Conversation>>
    suspend fun messages(conversationId: String): List<ChatMessage>

    /** Store the transcript locally and (best effort) index it on the Mac
     *  so JARVIS remembers it, as the desktop app does. */
    suspend fun save(conversationId: String, messages: List<ChatMessage>)

    suspend fun delete(conversationId: String)
}
