package com.felipechaux.jarvis.feature.chat.data.mapper

import com.felipechaux.jarvis.core.network.JarvisJson
import com.felipechaux.jarvis.feature.chat.data.local.entity.ConversationEntity
import com.felipechaux.jarvis.feature.chat.data.local.entity.MessageEntity
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.ChatRole
import com.felipechaux.jarvis.feature.chat.domain.model.Conversation
import com.felipechaux.jarvis.feature.chat.domain.model.ToolCall
import com.felipechaux.jarvis.feature.chat.domain.model.ToolStatus
import kotlinx.serialization.Serializable

@Serializable
private data class StoredToolCall(val tool: String, val arguments: String, val status: String, val latencyMs: Long? = null)

private const val TITLE_CHARS = 60

fun ChatMessage.toEntity(conversationId: String, position: Int) = MessageEntity(
    id = id,
    conversationId = conversationId,
    position = position,
    role = role.name,
    content = content,
    toolCallsJson = JarvisJson.encodeToString(
        toolCalls.map { StoredToolCall(it.tool, it.arguments, it.status.name, it.latencyMs) },
    ),
)

fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    role = runCatching { ChatRole.valueOf(role) }.getOrDefault(ChatRole.ASSISTANT),
    content = content,
    toolCalls = runCatching { JarvisJson.decodeFromString<List<StoredToolCall>>(toolCallsJson) }
        .getOrDefault(emptyList())
        .map {
            ToolCall(
                tool = it.tool,
                arguments = it.arguments,
                status = runCatching { ToolStatus.valueOf(it.status) }.getOrDefault(ToolStatus.SUCCEEDED),
                latencyMs = it.latencyMs,
            )
        },
)

fun ConversationEntity.toDomain() = Conversation(id = id, title = title, updatedAtMs = updatedAt)

/** First user message, trimmed — the same title the desktop uses. */
fun titleFor(messages: List<ChatMessage>): String {
    val first = messages.firstOrNull { it.role == ChatRole.USER }?.content?.trim().orEmpty()
    val oneLine = first.replace(Regex("\\s+"), " ")
    return if (oneLine.length <= TITLE_CHARS) oneLine else oneLine.take(TITLE_CHARS - 1).trimEnd() + "…"
}
