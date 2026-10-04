package com.felipechaux.jarvis.feature.sessions.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PromptOptionDto(val key: String, val label: String = "", val title: String = "")

@Serializable
data class SessionPromptDto(
    val id: String,
    val question: String = "",
    val detail: String = "",
    val options: List<PromptOptionDto> = emptyList(),
)

@Serializable
data class LiveSessionDto(
    val name: String,
    val project: String = "",
    val cli: String = "",
    val status: String = "idle",
    @SerialName("elapsed_s") val elapsedS: Int? = null,
    val prompt: SessionPromptDto? = null,
)

@Serializable
data class LiveSessionsDto(val sessions: List<LiveSessionDto> = emptyList())

@Serializable
data class SessionEventDto(val id: Int, val kind: String = "", val project: String = "", val text: String = "")

@Serializable
data class OkDto(val ok: Boolean = false, val error: String? = null)
