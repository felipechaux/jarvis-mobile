package com.felipechaux.jarvis

import com.felipechaux.jarvis.core.audio.VadGate
import com.felipechaux.jarvis.core.audio.VadVerdict
import com.felipechaux.jarvis.core.audio.pcm16ToWav
import com.felipechaux.jarvis.feature.chat.data.mapper.titleFor
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.ChatRole
import com.felipechaux.jarvis.feature.voice.domain.usecase.speechTextOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VoiceTest {
    /** Feed [levels] at 80 ms ticks; returns the first non-LISTENING verdict. */
    private fun run(levels: List<Float>): VadVerdict {
        val gate = VadGate()
        levels.forEachIndexed { i, rms ->
            val verdict = gate.push(rms, (i + 1) * 80L)
            if (verdict != VadVerdict.LISTENING) return verdict
        }
        return VadVerdict.LISTENING
    }

    @Test
    fun stopsAfterSpeechThenSilence() {
        val quiet = List(5) { 0.005f }
        val speech = List(15) { 0.1f }
        val silence = List(25) { 0.005f }
        assertEquals(VadVerdict.STOP, run(quiet + speech + silence))
    }

    @Test
    fun cancelsWhenNobodySpeaks() {
        assertEquals(VadVerdict.CANCEL, run(List(100) { 0.005f }))
    }

    @Test
    fun steadyMusicDoesNotHoldTheMicOpen() {
        // Loud constant background: once the floor tracks it, the music
        // reads as "background" and the recording ends within ~2 s instead
        // of running to the 30 s cap (the desktop bug this gate fixed).
        val gate = VadGate()
        var endedAtMs = -1L
        for (i in 1..400) {
            if (gate.push(0.06f, i * 80L) != VadVerdict.LISTENING) {
                endedAtMs = i * 80L
                break
            }
        }
        assertTrue(endedAtMs in 1..2_500, "ended at $endedAtMs ms")
    }

    @Test
    fun singleSpikeIsNotSpeech() {
        val levels = List(5) { 0.005f } + 0.2f + List(90) { 0.005f }
        assertEquals(VadVerdict.CANCEL, run(levels))
    }

    @Test
    fun wavHeader() {
        val wav = pcm16ToWav(shortArrayOf(1, -1, 300))
        assertEquals("RIFF", wav.decodeToString(0, 4))
        assertEquals("WAVE", wav.decodeToString(8, 12))
        assertEquals("data", wav.decodeToString(36, 40))
        assertEquals(44 + 6, wav.size)
        assertEquals(6, wav[40].toInt()) // data size, little endian
    }

    @Test
    fun speechTextDropsMarkdownAndCode() {
        val text = speechTextOf("**Listo.** Mira `main.kt`:\n```kotlin\nfun x() {}\n```\nMás en [docs](https://x.dev).")
        assertFalse(text.contains("```"))
        assertFalse(text.contains("*"))
        assertFalse(text.contains("https"))
        assertTrue(text.startsWith("Listo. Mira main.kt"))
        assertTrue(text.endsWith("Más en docs."))
    }

    @Test
    fun longSpeechCutsAtSentence() {
        val long = "Primera frase. ".repeat(100)
        val text = speechTextOf(long, maxChars = 100)
        assertTrue(text.length <= 100)
        assertTrue(text.endsWith("."))
    }

    @Test
    fun conversationTitleFromFirstUserMessage() {
        val messages = listOf(
            ChatMessage("1", ChatRole.USER, "  ¿Qué tengo\nhoy en el calendario?  "),
            ChatMessage("2", ChatRole.ASSISTANT, "Nada."),
        )
        assertEquals("¿Qué tengo hoy en el calendario?", titleFor(messages))
        assertTrue(titleFor(listOf(ChatMessage("1", ChatRole.USER, "a".repeat(200)))).length <= 60)
    }
}
