package com.felipechaux.jarvis.android

import android.app.Application
import com.felipechaux.jarvis.android.push.JarvisMessagingService
import com.felipechaux.jarvis.di.initKoin
import org.koin.android.ext.koin.androidContext

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@JarvisApplication)
        }
        JarvisMessagingService.setUp(this)
    }
}
