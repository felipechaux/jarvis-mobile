package com.felipechaux.jarvis.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile

sealed interface Recording {
    class Audio(val wav: ByteArray) : Recording
    data object NoSpeech : Recording
}

/**
 * Records one utterance: stops by itself after a pause once the user spoke
 * (or when [stop] is called), and reports [Recording.NoSpeech] when nothing
 * was said so silence is never transcribed.
 */
class VoiceRecorder(private val source: PcmSource) {

    private val _level = MutableStateFlow(0f)

    /** Smoothed 0..1 mic level for the "listening" visuals. */
    val level: StateFlow<Float> = _level.asStateFlow()

    private var stopRequested = false

    suspend fun record(tunables: VadTunables = VadTunables()): Recording {
        stopRequested = false
        val gate = VadGate(tunables)
        val chunks = ArrayList<ShortArray>()
        var elapsedMs = 0L
        var verdict = VadVerdict.LISTENING
        var smoothed = 0f
        try {
            source.frames().takeWhile { frame ->
                chunks += frame
                elapsedMs += frame.size * 1000L / SAMPLE_RATE
                val value = rms(frame)
                // Fast attack, slow release (as the desktop's mic meter).
                val target = (value / METER_FULL_SCALE_RMS).coerceAtMost(1f)
                smoothed += (target - smoothed) * if (target > smoothed) 0.5f else 0.12f
                _level.value = smoothed
                verdict = gate.push(value, elapsedMs)
                verdict == VadVerdict.LISTENING && !stopRequested
            }.collect {}
        } finally {
            _level.value = 0f
        }
        val spoke = gate.speechStarted || (stopRequested && verdict == VadVerdict.LISTENING && elapsedMs > MIN_MANUAL_MS)
        if (verdict == VadVerdict.CANCEL || !spoke) return Recording.NoSpeech
        val all = ShortArray(chunks.sumOf { it.size })
        var offset = 0
        for (c in chunks) {
            c.copyInto(all, offset)
            offset += c.size
        }
        return Recording.Audio(pcm16ToWav(all))
    }

    /** Ends the current recording now (tap on the mic while listening). */
    fun stop() {
        stopRequested = true
    }

    private companion object {
        const val METER_FULL_SCALE_RMS = 0.18f
        const val MIN_MANUAL_MS = 600L
    }
}
