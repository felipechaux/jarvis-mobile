package com.felipechaux.jarvis.core.audio

import kotlinx.coroutines.flow.Flow
import kotlin.math.sqrt

/** Whisper's native rate; every platform source resamples to it. */
const val SAMPLE_RATE = 16_000

/** 80 ms frames: the VAD's tick. */
const val FRAME_SAMPLES = SAMPLE_RATE * 80 / 1000

/** Microphone as 16 kHz mono PCM16 frames of [FRAME_SAMPLES]; collecting opens the mic. */
interface PcmSource {
    fun frames(): Flow<ShortArray>
}

/** Plays a complete audio file (MP3 from edge-tts, WAV from kokoro). */
interface AudioPlayer {
    /** Suspends until playback ends; cancelling stops it. */
    suspend fun play(audio: ByteArray)
    fun stop()
}

fun rms(frame: ShortArray): Float {
    if (frame.isEmpty()) return 0f
    var sum = 0.0
    for (s in frame) {
        val v = s / 32768.0
        sum += v * v
    }
    return sqrt(sum / frame.size).toFloat()
}

/** Wraps mono PCM16 little-endian samples in a WAV container. */
fun pcm16ToWav(samples: ShortArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
    val dataBytes = samples.size * 2
    val out = ByteArray(44 + dataBytes)
    fun putStr(at: Int, s: String) = s.forEachIndexed { i, c -> out[at + i] = c.code.toByte() }
    fun putInt(at: Int, v: Int) {
        for (i in 0 until 4) out[at + i] = (v shr (8 * i)).toByte()
    }
    fun putShort(at: Int, v: Int) {
        out[at] = v.toByte()
        out[at + 1] = (v shr 8).toByte()
    }
    putStr(0, "RIFF")
    putInt(4, 36 + dataBytes)
    putStr(8, "WAVE")
    putStr(12, "fmt ")
    putInt(16, 16) // PCM chunk size
    putShort(20, 1) // PCM
    putShort(22, 1) // mono
    putInt(24, sampleRate)
    putInt(28, sampleRate * 2) // byte rate
    putShort(32, 2) // block align
    putShort(34, 16) // bits per sample
    putStr(36, "data")
    putInt(40, dataBytes)
    var o = 44
    for (s in samples) {
        out[o++] = s.toInt().toByte()
        out[o++] = (s.toInt() shr 8).toByte()
    }
    return out
}
