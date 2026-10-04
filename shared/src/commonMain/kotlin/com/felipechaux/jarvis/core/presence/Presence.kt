package com.felipechaux.jarvis.core.presence

/** What JARVIS is doing right now; drives every "alive" visual (port of usePresence.ts). */
enum class Presence { IDLE, LISTENING, THINKING, SPEAKING }

fun presenceOf(recording: Boolean, speaking: Boolean, thinking: Boolean): Presence = when {
    recording -> Presence.LISTENING
    speaking -> Presence.SPEAKING
    thinking -> Presence.THINKING
    else -> Presence.IDLE
}
