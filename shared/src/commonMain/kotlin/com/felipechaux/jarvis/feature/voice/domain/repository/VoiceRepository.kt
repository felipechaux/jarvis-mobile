package com.felipechaux.jarvis.feature.voice.domain.repository

import com.felipechaux.jarvis.core.model.Resource

interface VoiceRepository {
    /** Whisper on the Mac (`/v1/speech/transcribe`); "" when nothing usable was heard. */
    suspend fun transcribe(wav: ByteArray): Resource<String>

    /** JARVIS's voice (`/v1/speech/synthesize`, edge-tts or kokoro) as a playable file. */
    suspend fun synthesize(text: String): Resource<ByteArray>
}
