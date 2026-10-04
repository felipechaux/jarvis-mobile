package com.felipechaux.jarvis.feature.voice.di

import com.felipechaux.jarvis.core.audio.VoiceRecorder
import com.felipechaux.jarvis.feature.voice.data.remote.SpeechApi
import com.felipechaux.jarvis.feature.voice.data.repository.VoiceRepositoryImpl
import com.felipechaux.jarvis.feature.voice.domain.repository.VoiceRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val voiceModule = module {
    singleOf(::SpeechApi)
    singleOf(::VoiceRepositoryImpl) bind VoiceRepository::class
    factoryOf(::VoiceRecorder)
}
