package com.felipechaux.jarvis.feature.chat.domain.repository

import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.StreamUpdate
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    /** Stream JARVIS's reply to [history] (the last message is the user's). */
    fun streamReply(history: List<ChatMessage>): Flow<StreamUpdate>
}
