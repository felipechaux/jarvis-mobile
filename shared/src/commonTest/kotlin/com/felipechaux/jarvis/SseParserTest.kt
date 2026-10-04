package com.felipechaux.jarvis

import com.felipechaux.jarvis.core.network.SseEvent
import com.felipechaux.jarvis.core.network.SseParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SseParserTest {
    @Test
    fun namedEventAppliesToNextDataOnly() {
        val parser = SseParser()
        assertNull(parser.feed("event: tool_call_start"))
        assertEquals(SseEvent("tool_call_start", "{\"tool\":\"x\"}"), parser.feed("data: {\"tool\":\"x\"}"))
        assertEquals(SseEvent(null, "{}"), parser.feed("data: {}"))
    }

    @Test
    fun blankLineResetsEventName() {
        val parser = SseParser()
        parser.feed("event: inference_start")
        parser.feed("")
        assertEquals(SseEvent(null, "x"), parser.feed("data: x"))
    }

    @Test
    fun doneSentinelStops() {
        val parser = SseParser()
        assertNull(parser.feed("data: [DONE]"))
        assertTrue(parser.done)
    }
}
