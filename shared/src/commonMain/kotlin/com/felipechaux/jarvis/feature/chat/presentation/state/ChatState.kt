package com.felipechaux.jarvis.feature.chat.presentation.state

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.presence.Presence
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.Conversation

data class ChatState(
    val conversationId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val showHistory: Boolean = false,
    val input: String = "",
    val isStreaming: Boolean = false,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val isSpeaking: Boolean = false,
    val micLevel: Float = 0f,
    val presence: Presence = Presence.IDLE,
    val error: AppError? = null,
)

sealed interface ChatAction {
    data class OnInputChange(val text: String) : ChatAction
    data object OnSendClick : ChatAction
    data object OnStopClick : ChatAction

    /** Mic permission already granted: start listening, or stop if listening. */
    data object OnMicClick : ChatAction
    data object OnNewConversation : ChatAction
    data class OnShowHistory(val show: Boolean) : ChatAction
    data class OnOpenConversation(val id: String) : ChatAction
    data class OnDeleteConversation(val id: String) : ChatAction
}
