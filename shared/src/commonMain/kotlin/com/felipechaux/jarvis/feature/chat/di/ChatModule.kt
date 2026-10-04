package com.felipechaux.jarvis.feature.chat.di

import com.felipechaux.jarvis.core.database.JarvisDatabase
import com.felipechaux.jarvis.feature.chat.data.remote.ChatApi
import com.felipechaux.jarvis.feature.chat.data.remote.ChatHistoryApi
import com.felipechaux.jarvis.feature.chat.data.repository.ChatHistoryRepositoryImpl
import com.felipechaux.jarvis.feature.chat.domain.repository.ChatHistoryRepository
import com.felipechaux.jarvis.feature.chat.data.repository.ChatRepositoryImpl
import com.felipechaux.jarvis.feature.chat.domain.repository.ChatRepository
import com.felipechaux.jarvis.feature.chat.domain.usecase.StreamReplyUseCase
import com.felipechaux.jarvis.feature.chat.presentation.viewmodel.ChatViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val chatModule = module {
    singleOf(::ChatApi)
    singleOf(::ChatHistoryApi)
    single { get<JarvisDatabase>().chatDao() }
    singleOf(::ChatHistoryRepositoryImpl) bind ChatHistoryRepository::class
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    factoryOf(::StreamReplyUseCase)
    viewModelOf(::ChatViewModel)
}
