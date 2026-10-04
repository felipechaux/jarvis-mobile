package com.felipechaux.jarvis.feature.chat.domain.usecase

import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.StreamUpdate
import com.felipechaux.jarvis.feature.chat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow

class StreamReplyUseCase(private val repository: ChatRepository) {
    operator fun invoke(history: List<ChatMessage>): Flow<StreamUpdate> = repository.streamReply(history)
}
