package com.felipechaux.jarvis.feature.pairing.di

import com.felipechaux.jarvis.feature.pairing.data.remote.PairingApi
import com.felipechaux.jarvis.feature.pairing.data.repository.PairingRepositoryImpl
import com.felipechaux.jarvis.feature.pairing.domain.repository.PairingRepository
import com.felipechaux.jarvis.feature.pairing.domain.usecase.PairWithServerUseCase
import com.felipechaux.jarvis.feature.pairing.presentation.viewmodel.PairingViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val pairingModule = module {
    singleOf(::PairingApi)
    singleOf(::PairingRepositoryImpl) bind PairingRepository::class
    factoryOf(::PairWithServerUseCase)
    viewModelOf(::PairingViewModel)
}
