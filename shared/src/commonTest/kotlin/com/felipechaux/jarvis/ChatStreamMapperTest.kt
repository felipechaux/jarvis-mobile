package com.felipechaux.jarvis

import com.felipechaux.jarvis.core.network.SseEvent
import com.felipechaux.jarvis.feature.chat.data.mapper.ChatStreamMapper
import com.felipechaux.jarvis.feature.chat.domain.model.StreamUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatStreamMapperTest {
    @Test
    fun openAiChunkBecomesToken() {
        val chunk = """{"id":"c1","model":"m","choices":[{"index":0,"delta":{"content":"Hola"}}]}"""
        assertEquals(StreamUpdate.Token("Hola"), ChatStreamMapper.toUpdate(SseEvent(null, chunk)))
    }

    @Test
    fun finalChunkWithoutContentIsIgnored() {
        val chunk = """{"choices":[{"delta":{},"finish_reason":"stop"}],"usage":{"total_tokens":3}}"""
        assertNull(ChatStreamMapper.toUpdate(SseEvent(null, chunk)))
    }

    @Test
    fun toolEvents() {
        assertEquals(
            StreamUpdate.ToolStarted("web_search", "{\"q\":\"x\"}"),
            ChatStreamMapper.toUpdate(SseEvent("tool_call_start", """{"tool":"web_search","arguments":"{\"q\":\"x\"}"}""")),
        )
        assertEquals(
            StreamUpdate.ToolFinished("web_search", true, 1500),
            ChatStreamMapper.toUpdate(SseEvent("tool_call_end", """{"tool":"web_search","success":true,"latency":1.5}""")),
        )
    }

    @Test
    fun unknownEventAndGarbageIgnored() {
        assertNull(ChatStreamMapper.toUpdate(SseEvent("tool_results", "{}")))
        assertNull(ChatStreamMapper.toUpdate(SseEvent(null, "not json")))
    }
}
