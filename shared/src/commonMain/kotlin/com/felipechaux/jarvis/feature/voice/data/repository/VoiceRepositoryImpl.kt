package com.felipechaux.jarvis.feature.voice.data.repository

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.model.map
import com.felipechaux.jarvis.core.platform.PlatformInfo
import com.felipechaux.jarvis.core.storage.CredentialStore
import com.felipechaux.jarvis.feature.voice.data.remote.SpeechApi
import com.felipechaux.jarvis.feature.voice.domain.repository.VoiceRepository

class VoiceRepositoryImpl(
    private val api: SpeechApi,
    private val store: CredentialStore,
    private val platform: PlatformInfo,
) : VoiceRepository {

    override suspend fun transcribe(wav: ByteArray): Resource<String> {
        val creds = store.current() ?: return Resource.Error(AppError.Unauthorized)
        return api.transcribe(creds, wav, platform.language).map { it.text.trim() }
    }

    override suspend fun synthesize(text: String): Resource<ByteArray> {
        val creds = store.current() ?: return Resource.Error(AppError.Unauthorized)
        return api.synthesize(creds, text)
    }
}
