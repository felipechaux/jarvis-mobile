package com.felipechaux.jarvis.feature.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipechaux.jarvis.core.audio.AudioPlayer
import com.felipechaux.jarvis.core.audio.Recording
import com.felipechaux.jarvis.core.audio.VoiceRecorder
import com.felipechaux.jarvis.core.model.onError
import com.felipechaux.jarvis.core.model.onSuccess
import com.felipechaux.jarvis.core.presence.presenceOf
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.ChatRole
import com.felipechaux.jarvis.feature.chat.domain.model.StreamUpdate
import com.felipechaux.jarvis.feature.chat.domain.model.ToolCall
import com.felipechaux.jarvis.feature.chat.domain.model.ToolStatus
import com.felipechaux.jarvis.feature.chat.domain.repository.ChatHistoryRepository
import com.felipechaux.jarvis.feature.chat.domain.usecase.StreamReplyUseCase
import com.felipechaux.jarvis.feature.chat.presentation.state.ChatAction
import com.felipechaux.jarvis.feature.chat.presentation.state.ChatState
import com.felipechaux.jarvis.feature.voice.domain.repository.VoiceRepository
import com.felipechaux.jarvis.feature.voice.domain.usecase.speechTextOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class ChatViewModel(
    private val streamReply: StreamReplyUseCase,
    private val history: ChatHistoryRepository,
    private val voice: VoiceRepository,
    private val recorder: VoiceRecorder,
    private val player: AudioPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState(conversationId = newId()))
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var recordJob: Job? = null
    private var speakJob: Job? = null

    init {
        viewModelScope.launch {
            history.observeConversations().collect { list -> _state.update { it.copy(conversations = list) } }
        }
        viewModelScope.launch {
            recorder.level.collect { level -> _state.update { it.copy(micLevel = level) } }
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnInputChange -> _state.update { it.copy(input = action.text) }
            ChatAction.OnSendClick -> send(_state.value.input, spokenReply = false)
            ChatAction.OnStopClick -> stopEverything()
            ChatAction.OnMicClick -> toggleMic()
            ChatAction.OnNewConversation -> openConversation(newId(), emptyList())
            is ChatAction.OnShowHistory -> _state.update { it.copy(showHistory = action.show) }
            is ChatAction.OnOpenConversation -> viewModelScope.launch {
                openConversation(action.id, history.messages(action.id))
            }
            is ChatAction.OnDeleteConversation -> viewModelScope.launch {
                history.delete(action.id)
                if (action.id == _state.value.conversationId) openConversation(newId(), emptyList())
            }
        }
    }

    // ── Text and streaming ───────────────────────────────────────────

    private fun send(rawText: String, spokenReply: Boolean) {
        val text = rawText.trim()
        if (text.isEmpty() || _state.value.isStreaming) return
        val user = ChatMessage(newId(), ChatRole.USER, text)
        val turn = _state.value.messages + user
        val replyId = newId()
        update {
            it.copy(
                messages = turn + ChatMessage(replyId, ChatRole.ASSISTANT, ""),
                input = "",
                isStreaming = true,
                error = null,
            )
        }
        streamJob = viewModelScope.launch {
            streamReply(turn).collect { apply(replyId, it) }
            finish(replyId)
            val conversationId = _state.value.conversationId
            history.save(conversationId, _state.value.messages)
            val reply = _state.value.messages.lastOrNull { it.id == replyId }?.content.orEmpty()
            if (spokenReply && reply.isNotBlank()) speak(reply)
        }
    }

    private fun apply(replyId: String, update: StreamUpdate) {
        when (update) {
            is StreamUpdate.Token -> editReply(replyId) { it.copy(content = it.content + update.text) }
            is StreamUpdate.ToolStarted -> editReply(replyId) {
                it.copy(toolCalls = it.toolCalls + ToolCall(update.tool, update.arguments))
            }
            is StreamUpdate.ToolFinished -> editReply(replyId) { reply ->
                val index = reply.toolCalls.indexOfLast { it.tool == update.tool && it.status == ToolStatus.RUNNING }
                if (index < 0) return@editReply reply
                val done = reply.toolCalls[index].copy(
                    status = if (update.success) ToolStatus.SUCCEEDED else ToolStatus.FAILED,
                    latencyMs = update.latencyMs,
                )
                reply.copy(toolCalls = reply.toolCalls.toMutableList().also { it[index] = done })
            }
            is StreamUpdate.Failed -> _state.update { it.copy(error = update.error) }
            is StreamUpdate.Phase -> Unit // presence already shows "thinking"
        }
    }

    private fun finish(replyId: String) {
        update { state ->
            // Drop an empty reply bubble (error or stopped before any token).
            val messages = state.messages.filterNot {
                it.id == replyId && it.content.isEmpty() && it.toolCalls.isEmpty()
            }
            state.copy(messages = messages, isStreaming = false)
        }
    }

    private fun editReply(replyId: String, edit: (ChatMessage) -> ChatMessage) {
        _state.update { state ->
            state.copy(messages = state.messages.map { if (it.id == replyId) edit(it) else it })
        }
    }

    private fun openConversation(id: String, messages: List<ChatMessage>) {
        stopEverything()
        update { it.copy(conversationId = id, messages = messages, showHistory = false, error = null) }
    }

    // ── Voice ────────────────────────────────────────────────────────

    private fun toggleMic() {
        if (_state.value.isRecording) {
            recorder.stop()
            return
        }
        if (_state.value.isStreaming || _state.value.isTranscribing) return
        stopSpeaking() // talking over JARVIS interrupts it, as on the desktop
        recordJob = viewModelScope.launch {
            update { it.copy(isRecording = true, error = null) }
            val recording = try {
                recorder.record()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Recording.NoSpeech
            } finally {
                update { it.copy(isRecording = false) }
            }
            if (recording !is Recording.Audio) return@launch
            update { it.copy(isTranscribing = true) }
            voice.transcribe(recording.wav)
                .onSuccess { text -> if (text.isNotBlank()) send(text, spokenReply = true) }
                .onError { error -> _state.update { it.copy(error = error) } }
            update { it.copy(isTranscribing = false) }
        }
    }

    private fun speak(reply: String) {
        val text = speechTextOf(reply)
        if (text.isBlank()) return
        speakJob = viewModelScope.launch {
            voice.synthesize(text).onSuccess { audio ->
                update { it.copy(isSpeaking = true) }
                try {
                    player.play(audio)
                } finally {
                    update { it.copy(isSpeaking = false) }
                }
            }
        }
    }

    private fun stopSpeaking() {
        speakJob?.cancel()
        player.stop()
    }

    private fun stopEverything() {
        recorder.stop()
        recordJob?.cancel()
        stopSpeaking()
        streamJob?.cancel()
        _state.value.messages.lastOrNull()?.let { if (_state.value.isStreaming) finish(it.id) }
        update { it.copy(isRecording = false, isTranscribing = false, isSpeaking = false) }
    }

    private fun update(transform: (ChatState) -> ChatState) {
        _state.update { transform(it).withPresence() }
    }

    private fun ChatState.withPresence() = copy(
        presence = presenceOf(
            recording = isRecording,
            speaking = isSpeaking,
            thinking = isStreaming || isTranscribing,
        ),
    )

    override fun onCleared() {
        recorder.stop()
        player.stop()
    }

    private fun newId(): String = Random.nextLong().toULong().toString(36)
}
