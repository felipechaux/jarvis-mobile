package com.felipechaux.jarvis.feature.sessions.di

import com.felipechaux.jarvis.feature.sessions.data.repository.SessionsRepositoryImpl
import com.felipechaux.jarvis.feature.sessions.domain.repository.SessionsRepository
import com.felipechaux.jarvis.feature.sessions.presentation.viewmodel.SessionsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val sessionsModule = module {
    singleOf(::SessionsRepositoryImpl) bind SessionsRepository::class
    viewModelOf(::SessionsViewModel)
}
