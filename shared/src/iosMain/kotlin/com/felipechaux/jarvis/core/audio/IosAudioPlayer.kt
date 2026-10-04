package com.felipechaux.jarvis.core.audio

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.delay
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer : AudioPlayer {

    private var current: AVAudioPlayer? = null

    override suspend fun play(audio: ByteArray) {
        val data = audio.usePinned { NSData.create(bytes = it.addressOf(0), length = audio.size.toULong()) }
        val player = AVAudioPlayer(data = data, error = null)
        current = player
        try {
            if (!player.play()) return
            // AVAudioPlayer reports completion through a delegate; polling
            // keeps this cancellable without an Objective-C delegate class.
            while (player.playing) delay(POLL_MS)
        } finally {
            player.stop()
            current = null
        }
    }

    override fun stop() {
        current?.stop()
    }

    private companion object {
        const val POLL_MS = 100L
    }
}
