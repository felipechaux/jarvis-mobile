package com.felipechaux.jarvis.di

import com.felipechaux.jarvis.core.network.EndpointResolver
import com.felipechaux.jarvis.core.network.MobileRpcClient
import com.felipechaux.jarvis.core.network.createHttpClient
import com.felipechaux.jarvis.core.push.PushRegistrar
import com.felipechaux.jarvis.core.storage.CredentialStore
import com.felipechaux.jarvis.core.storage.DataStoreCredentialStore
import com.felipechaux.jarvis.feature.chat.di.chatModule
import com.felipechaux.jarvis.feature.pairing.di.pairingModule
import com.felipechaux.jarvis.feature.sessions.di.sessionsModule
import com.felipechaux.jarvis.feature.voice.di.voiceModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Provides the platform DataStore and [com.felipechaux.jarvis.core.platform.PlatformInfo]. */
expect fun platformModule(): Module

val coreModule = module {
    single { createHttpClient() }
    single<CredentialStore> { DataStoreCredentialStore(get(), get()) }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { EndpointResolver(get(), get()) }
    single { MobileRpcClient(get(), get(), get(), get()) }
    single { PushRegistrar(get(), get(), get(), get()) }
}

fun appModules(): List<Module> = listOf(platformModule(), coreModule, pairingModule, chatModule, voiceModule, sessionsModule)

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    val koin = startKoin {
        appDeclaration()
        modules(appModules())
    }.koin
    koin.get<PushRegistrar>().start()
}
