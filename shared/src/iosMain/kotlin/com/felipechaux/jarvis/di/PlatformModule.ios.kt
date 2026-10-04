package com.felipechaux.jarvis.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.felipechaux.jarvis.core.audio.AudioPlayer
import com.felipechaux.jarvis.core.audio.IosAudioPlayer
import com.felipechaux.jarvis.core.audio.IosPcmSource
import com.felipechaux.jarvis.core.audio.PcmSource
import com.felipechaux.jarvis.core.database.DATABASE_FILE
import com.felipechaux.jarvis.core.database.JarvisDatabase
import com.felipechaux.jarvis.core.database.buildJarvisDatabase
import com.felipechaux.jarvis.core.platform.PlatformInfo
import com.felipechaux.jarvis.core.storage.DATASTORE_FILE
import com.felipechaux.jarvis.core.storage.KeychainTokenVault
import com.felipechaux.jarvis.core.storage.TokenVault
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

@OptIn(ExperimentalForeignApi::class)
actual fun platformModule(): Module = module {
    single {
        PreferenceDataStoreFactory.createWithPath { (documentsPath() + "/" + DATASTORE_FILE).toPath() }
    }
    single {
        Room.databaseBuilder<JarvisDatabase>(name = documentsPath() + "/" + DATABASE_FILE).buildJarvisDatabase()
    }
    single<TokenVault> { KeychainTokenVault() }
    single {
        PlatformInfo(
            deviceName = UIDevice.currentDevice.name,
            platform = "ios",
            language = NSLocale.currentLocale.languageCode,
        )
    }
    single<PcmSource> { IosPcmSource() }
    single<AudioPlayer> { IosAudioPlayer() }
}

@OptIn(ExperimentalForeignApi::class)
private fun documentsPath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(requireNotNull(documents).path)
}
