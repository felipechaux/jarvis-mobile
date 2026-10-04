package com.felipechaux.jarvis.core.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive

/**
 * AVAudioEngine input tap at the hardware rate (usually 48 kHz float),
 * averaged down to 16 kHz PCM16 and cut into [FRAME_SAMPLES] frames.
 */
@OptIn(ExperimentalForeignApi::class)
class IosPcmSource : PcmSource {

    override fun frames(): Flow<ShortArray> = callbackFlow {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeDefault,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionAllowBluetooth,
            error = null,
        )
        session.setActive(true, error = null)

        val engine = AVAudioEngine()
        val input = engine.inputNode
        val format = input.outputFormatForBus(0u)
        val ratio = format.sampleRate / SAMPLE_RATE
        val pending = ShortArray(FRAME_SAMPLES)
        var filled = 0
        var carry = 0.0 // fractional input position between buffers
        var acc = 0.0
        var accCount = 0

        input.installTapOnBus(0u, bufferSize = 4096u, format = format) { buffer, _ ->
            val samples = buffer?.floatChannelData?.get(0) ?: return@installTapOnBus
            val count = buffer.frameLength.toInt()
            for (i in 0 until count) {
                acc += samples[i]
                accCount++
                carry += 1.0
                if (carry >= ratio) {
                    carry -= ratio
                    val v = (acc / accCount).coerceIn(-1.0, 1.0)
                    acc = 0.0
                    accCount = 0
                    pending[filled++] = (v * Short.MAX_VALUE).toInt().toShort()
                    if (filled == FRAME_SAMPLES) {
                        trySend(pending.copyOf())
                        filled = 0
                    }
                }
            }
        }
        engine.prepare()
        if (!engine.startAndReturnError(null)) close(IllegalStateException("microphone unavailable"))

        awaitClose {
            input.removeTapOnBus(0u)
            engine.stop()
        }
    }
}
