package com.felipechaux.jarvis.feature.sessions.data.mapper

import com.felipechaux.jarvis.feature.sessions.data.remote.LiveSessionDto
import com.felipechaux.jarvis.feature.sessions.data.remote.SessionEventDto
import com.felipechaux.jarvis.feature.sessions.domain.model.LiveSession
import com.felipechaux.jarvis.feature.sessions.domain.model.PromptOption
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionEvent
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionPrompt
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionStatus

fun LiveSessionDto.toDomain() = LiveSession(
    name = name,
    project = project.ifBlank { name },
    cli = cli,
    status = when (status) {
        "prompt" -> SessionStatus.PROMPT
        "working" -> SessionStatus.WORKING
        else -> SessionStatus.IDLE
    },
    elapsedS = elapsedS,
    prompt = prompt?.let { p ->
        SessionPrompt(
            id = p.id,
            question = p.question,
            options = p.options.map { PromptOption(it.key, it.label.ifBlank { it.title }) },
        )
    },
)

fun SessionEventDto.toDomain() = SessionEvent(id = id, kind = kind, project = project, text = text)
