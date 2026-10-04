package com.felipechaux.jarvis.feature.chat.domain.model

import com.felipechaux.jarvis.core.model.AppError

enum class ChatRole { USER, ASSISTANT }

enum class ToolStatus { RUNNING, SUCCEEDED, FAILED }

data class ToolCall(
    val tool: String,
    val arguments: String,
    val status: ToolStatus = ToolStatus.RUNNING,
    val latencyMs: Long? = null,
)

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
)

/** One step of a streamed reply, decoded from the server's SSE stream. */
sealed interface StreamUpdate {
    /** Agent lifecycle (`agent_turn_start`, `inference_start`, …) for the "thinking" UI. */
    data class Phase(val name: String) : StreamUpdate
    data class ToolStarted(val tool: String, val arguments: String) : StreamUpdate
    data class ToolFinished(val tool: String, val success: Boolean, val latencyMs: Long?) : StreamUpdate
    data class Token(val text: String) : StreamUpdate
    data class Failed(val error: AppError) : StreamUpdate
}
