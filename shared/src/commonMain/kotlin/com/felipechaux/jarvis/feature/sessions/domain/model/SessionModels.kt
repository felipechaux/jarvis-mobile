package com.felipechaux.jarvis.feature.sessions.domain.model

enum class SessionStatus { PROMPT, WORKING, IDLE }

data class PromptOption(val key: String, val label: String)

/** A menu the session waits on (e.g. Claude asking permission to run a tool). */
data class SessionPrompt(val id: String, val question: String, val options: List<PromptOption>)

/** A live `jarvis-*` tmux session on the Mac (Claude Code / Antigravity). */
data class LiveSession(
    val name: String,
    val project: String,
    val cli: String,
    val status: SessionStatus,
    val elapsedS: Int?,
    val prompt: SessionPrompt?,
)

/** "Claude terminó en openjarvis", "Claude pide permiso…", progress, summaries. */
data class SessionEvent(val id: Int, val kind: String, val project: String, val text: String)
