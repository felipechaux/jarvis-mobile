package com.felipechaux.jarvis.feature.chat.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageDto(val role: String, val content: String)

@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
)

@Serializable
data class ServerInfoDto(val model: String = "", val agent: String? = null, val engine: String = "")

@Serializable
data class ChunkDto(val choices: List<ChunkChoiceDto> = emptyList())

@Serializable
data class ChunkChoiceDto(val delta: DeltaDto = DeltaDto())

@Serializable
data class DeltaDto(val content: String? = null)

@Serializable
data class ToolCallStartDto(val tool: String = "", val arguments: String = "")

@Serializable
data class ToolCallEndDto(val tool: String = "", val success: Boolean = true, val latency: Double? = null)
