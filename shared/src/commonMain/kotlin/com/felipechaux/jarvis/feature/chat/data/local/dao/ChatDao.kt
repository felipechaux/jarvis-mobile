package com.felipechaux.jarvis.feature.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.felipechaux.jarvis.feature.chat.data.local.entity.ConversationEntity
import com.felipechaux.jarvis.feature.chat.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY position")
    suspend fun messages(conversationId: String): List<MessageEntity>

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessages(conversationId: String)

    @Upsert
    suspend fun upsertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    /** Replace a conversation's transcript in one transaction. */
    @Transaction
    suspend fun saveTranscript(conversation: ConversationEntity, messages: List<MessageEntity>) {
        upsertConversation(conversation)
        deleteMessages(conversation.id)
        upsertMessages(messages)
    }
}
