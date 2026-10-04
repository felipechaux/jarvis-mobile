package com.felipechaux.jarvis

import com.felipechaux.jarvis.core.presence.Presence
import com.felipechaux.jarvis.core.presence.presenceOf
import kotlin.test.Test
import kotlin.test.assertEquals

class PresenceTest {
    @Test
    fun priorityMatchesDesktop() {
        assertEquals(Presence.LISTENING, presenceOf(recording = true, speaking = true, thinking = true))
        assertEquals(Presence.SPEAKING, presenceOf(recording = false, speaking = true, thinking = true))
        assertEquals(Presence.THINKING, presenceOf(recording = false, speaking = false, thinking = true))
        assertEquals(Presence.IDLE, presenceOf(recording = false, speaking = false, thinking = false))
    }
}
