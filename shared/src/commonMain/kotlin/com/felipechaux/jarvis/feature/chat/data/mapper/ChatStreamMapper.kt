package com.felipechaux.jarvis.feature.chat.data.mapper

import com.felipechaux.jarvis.core.network.JarvisJson
import com.felipechaux.jarvis.core.network.SseEvent
import com.felipechaux.jarvis.feature.chat.data.remote.ChatMessageDto
import com.felipechaux.jarvis.feature.chat.data.remote.ChunkDto
import com.felipechaux.jarvis.feature.chat.data.remote.ToolCallEndDto
import com.felipechaux.jarvis.feature.chat.data.remote.ToolCallStartDto
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.ChatRole
import com.felipechaux.jarvis.feature.chat.domain.model.StreamUpdate

/**
 * SSE → [StreamUpdate].  Named events come from the server's
 * `AgentStreamBridge` (`stream_bridge.py`); unnamed ones are OpenAI chunks.
 */
object ChatStreamMapper {

    fun toUpdate(event: SseEvent): StreamUpdate? = runCatching {
        when (event.event) {
            null -> JarvisJson.decodeFromString<ChunkDto>(event.data)
                .choices.firstOrNull()?.delta?.content
                ?.takeIf { it.isNotEmpty() }
                ?.let { StreamUpdate.Token(it) }
            "tool_call_start" -> JarvisJson.decodeFromString<ToolCallStartDto>(event.data)
                .let { StreamUpdate.ToolStarted(it.tool, it.arguments) }
            "tool_call_end" -> JarvisJson.decodeFromString<ToolCallEndDto>(event.data)
                .let { StreamUpdate.ToolFinished(it.tool, it.success, it.latency?.let { s -> (s * 1000).toLong() }) }
            "agent_turn_start", "inference_start", "inference_end" -> StreamUpdate.Phase(event.event)
            else -> null // tool_results and future events are ignored
        }
    }.getOrNull()

    fun toDto(message: ChatMessage): ChatMessageDto = ChatMessageDto(
        role = if (message.role == ChatRole.USER) "user" else "assistant",
        content = message.content,
    )
}
