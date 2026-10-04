package com.felipechaux.jarvis

import androidx.compose.ui.window.ComposeUIViewController
import com.felipechaux.jarvis.core.push.PushRegistrar
import com.felipechaux.jarvis.di.initKoin
import org.koin.mp.KoinPlatform

/** Entry point for the iOS app: `MainViewControllerKt.MainViewController()` from SwiftUI. */
fun MainViewController() = ComposeUIViewController { App() }

/** Call once from the iOS app's init, before showing [MainViewController]. */
fun doInitKoin() = initKoin()

/** The app delegate hands over the APNs device token (hex) it received. */
fun onApnsToken(token: String) {
    KoinPlatform.getKoin().get<PushRegistrar>().onNewToken("apns", token)
}
