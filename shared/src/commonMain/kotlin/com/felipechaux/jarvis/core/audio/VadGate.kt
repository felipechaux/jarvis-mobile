package com.felipechaux.jarvis.core.audio

/**
 * Voice-activity gate, ported from the desktop's `lib/vad.ts`.
 *
 * Fed one mic RMS reading per tick, it decides when the user finished
 * speaking ([VadVerdict.STOP]), never spoke ([VadVerdict.CANCEL] — the
 * audio is discarded) or is still going.  Thresholds are relative to the
 * background (25th percentile of the last few seconds), so music or street
 * noise doesn't hold the mic open; in a quiet room the absolute minimums
 * dominate.
 */
enum class VadVerdict { LISTENING, STOP, CANCEL }

data class VadTunables(
    val speechRms: Float = 0.030f,
    val silenceRms: Float = 0.012f,
    val speechOverFloor: Float = 2.2f,
    val silenceOverFloor: Float = 1.6f,
    val speechOnsetTicks: Int = 3,
    val silenceHoldMs: Long = 1_200,
    val noSpeechTimeoutMs: Long = 6_000,
    val maxRecordingMs: Long = 30_000,
    val armDelayMs: Long = 250,
    val floorWindowMs: Long = 3_000,
    val minFloorSamples: Int = 4,
    val tickMs: Long = 80,
)

class VadGate(private val t: VadTunables = VadTunables()) {
    var speechStarted = false
        private set

    private val recent = ArrayDeque<Float>()
    private var loudTicks = 0
    private var silenceStart = -1L

    val floor: Float
        get() {
            if (recent.size < t.minFloorSamples) return 0f
            val sorted = recent.sorted()
            return sorted[(sorted.size * 0.25).toInt()]
        }

    private val speechThreshold get() = maxOf(t.speechRms, floor * t.speechOverFloor)
    private val silenceThreshold get() = maxOf(t.silenceRms, floor * t.silenceOverFloor)

    fun push(rms: Float, elapsedMs: Long): VadVerdict {
        if (elapsedMs > t.maxRecordingMs) return finish()
        if (elapsedMs < t.armDelayMs) return VadVerdict.LISTENING

        // Thresholds come from the background *before* this reading.
        val speechT = speechThreshold
        val silenceT = silenceThreshold

        recent.addLast(rms)
        val keep = maxOf(1L, t.floorWindowMs / t.tickMs).toInt()
        while (recent.size > keep) recent.removeFirst()

        if (rms > speechT) {
            loudTicks += 1
            if (!speechStarted && loudTicks >= t.speechOnsetTicks) speechStarted = true
            if (speechStarted) silenceStart = -1
            return VadVerdict.LISTENING
        }
        loudTicks = 0

        if (!speechStarted) {
            return if (elapsedMs > t.noSpeechTimeoutMs) VadVerdict.CANCEL else VadVerdict.LISTENING
        }

        // Between the thresholds: uncertain — hold state (hysteresis).
        if (rms >= silenceT) return VadVerdict.LISTENING

        if (silenceStart < 0) {
            silenceStart = elapsedMs
        } else if (elapsedMs - silenceStart > t.silenceHoldMs) {
            return finish()
        }
        return VadVerdict.LISTENING
    }

    private fun finish() = if (speechStarted) VadVerdict.STOP else VadVerdict.CANCEL
}
