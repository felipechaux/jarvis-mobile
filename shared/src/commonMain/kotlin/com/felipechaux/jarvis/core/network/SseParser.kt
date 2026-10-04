package com.felipechaux.jarvis.core.network

/** One Server-Sent Event: the optional `event:` name and its `data:` payload. */
data class SseEvent(val event: String?, val data: String)

/**
 * Incremental SSE parser fed line by line (port of the desktop's `lib/sse.ts`).
 * Returns an event for each `data:` line, null otherwise; `[DONE]` sets [done].
 */
class SseParser {
    private var currentEvent: String? = null

    var done: Boolean = false
        private set

    fun feed(line: String): SseEvent? {
        when {
            line.startsWith("event: ") -> currentEvent = line.removePrefix("event: ").trim()
            line.startsWith("data: ") -> {
                val data = line.removePrefix("data: ")
                if (data == "[DONE]") {
                    done = true
                    return null
                }
                return SseEvent(currentEvent, data).also { currentEvent = null }
            }
            line.isBlank() -> currentEvent = null
        }
        return null
    }
}
