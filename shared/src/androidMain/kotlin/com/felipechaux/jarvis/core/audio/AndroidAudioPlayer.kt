package com.felipechaux.jarvis.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

class AndroidAudioPlayer(private val context: Context) : AudioPlayer {

    private var current: MediaPlayer? = null

    override suspend fun play(audio: ByteArray) {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("jarvis-tts", null, context.cacheDir).apply { writeBytes(audio) }
        }
        try {
            withContext(Dispatchers.Main) {
                suspendCancellableCoroutine { cont ->
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build(),
                        )
                        setDataSource(file.absolutePath)
                        setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                        setOnErrorListener { _, _, _ ->
                            if (cont.isActive) cont.resume(Unit)
                            true
                        }
                        prepare()
                        start()
                    }
                    current = player
                    cont.invokeOnCancellation { player.runCatching { stop() } }
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                current?.release()
                current = null
            }
            file.delete()
        }
    }

    override fun stop() {
        current?.runCatching { stop() }
    }
}
