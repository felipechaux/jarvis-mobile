package com.felipechaux.jarvis.feature.voice.domain.usecase

/**
 * A reply made fit to be read aloud: no code blocks, markdown, links or
 * file paths, and capped so a long answer doesn't monologue for minutes.
 */
fun speechTextOf(reply: String, maxChars: Int = 700): String {
    var text = reply
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("https?://\\S+"), "")
        .replace(Regex("[*#>|_~]+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    if (text.length > maxChars) {
        val cut = text.take(maxChars)
        val end = cut.lastIndexOfAny(charArrayOf('.', '!', '?'))
        text = if (end > maxChars / 2) cut.take(end + 1) else cut.trimEnd() + "…"
    }
    return text
}
