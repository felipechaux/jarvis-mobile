package com.felipechaux.jarvis.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

/** AudioRecord at 16 kHz mono PCM16 (VOICE_RECOGNITION: no AGC/noise games). */
class AndroidPcmSource : PcmSource {

    // RECORD_AUDIO is requested by the UI before recording starts.
    @SuppressLint("MissingPermission")
    override fun frames(): Flow<ShortArray> = flow {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, FRAME_SAMPLES * 4),
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "microphone unavailable" }
        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val frame = ShortArray(FRAME_SAMPLES)
                var read = 0
                while (read < FRAME_SAMPLES) {
                    val n = recorder.read(frame, read, FRAME_SAMPLES - read)
                    if (n <= 0) error("microphone read failed ($n)")
                    read += n
                }
                emit(frame)
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)
}
