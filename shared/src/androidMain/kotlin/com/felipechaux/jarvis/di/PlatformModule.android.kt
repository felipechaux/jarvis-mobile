package com.felipechaux.jarvis.di

import android.content.Context
import android.os.Build
import com.felipechaux.jarvis.core.audio.AndroidAudioPlayer
import com.felipechaux.jarvis.core.audio.AndroidPcmSource
import com.felipechaux.jarvis.core.audio.AudioPlayer
import com.felipechaux.jarvis.core.audio.PcmSource
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.felipechaux.jarvis.core.database.DATABASE_FILE
import com.felipechaux.jarvis.core.database.JarvisDatabase
import com.felipechaux.jarvis.core.database.buildJarvisDatabase
import com.felipechaux.jarvis.core.platform.PlatformInfo
import com.felipechaux.jarvis.core.storage.DATASTORE_FILE
import com.felipechaux.jarvis.core.storage.KeystoreTokenVault
import com.felipechaux.jarvis.core.storage.TokenVault
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Locale

actual fun platformModule(): Module = module {
    single {
        val context = get<Context>()
        PreferenceDataStoreFactory.createWithPath {
            context.filesDir.resolve(DATASTORE_FILE).absolutePath.toPath()
        }
    }
    single {
        val context = get<Context>()
        Room.databaseBuilder<JarvisDatabase>(
            context = context,
            name = context.getDatabasePath(DATABASE_FILE).absolutePath,
        ).buildJarvisDatabase()
    }
    single<TokenVault> { KeystoreTokenVault(get()) }
    single {
        PlatformInfo(
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            platform = "android",
            language = Locale.getDefault().language,
        )
    }
    single<PcmSource> { AndroidPcmSource() }
    single<AudioPlayer> { AndroidAudioPlayer(get()) }
}
