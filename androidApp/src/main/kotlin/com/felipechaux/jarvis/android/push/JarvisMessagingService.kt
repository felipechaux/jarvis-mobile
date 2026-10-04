package com.felipechaux.jarvis.android.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.felipechaux.jarvis.android.MainActivity
import com.felipechaux.jarvis.core.push.PushRegistrar
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext

/** FCM entry point: forwards new tokens to the Mac, shows banners in foreground. */
class JarvisMessagingService : FirebaseMessagingService() {

    private val registrar: PushRegistrar by inject()

    override fun onNewToken(token: String) {
        registrar.onNewToken(PROVIDER, token)
    }

    // In background FCM shows the notification itself; in foreground it lands here.
    override fun onMessageReceived(message: RemoteMessage) {
        val note = message.notification ?: return
        showNotification(this, note.title.orEmpty(), note.body.orEmpty())
    }

    companion object {
        private const val PROVIDER = "fcm"
        const val CHANNEL_ID = "sessions"

        /**
         * Create the channel and register the current token.  No-op without
         * `google-services.json` (Firebase not configured): push stays off and
         * the app still gets live events over the WebSocket.
         */
        fun setUp(context: Context) {
            createChannel(context)
            if (FirebaseApp.getApps(context).isEmpty()) return
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                GlobalContext.get().get<PushRegistrar>().onNewToken(PROVIDER, token)
            }
        }

        private fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Sesiones de JARVIS", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        private fun showNotification(context: Context, title: String, body: String) {
            val intent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()
            runCatching { NotificationManagerCompat.from(context).notify(body.hashCode(), notification) }
        }
    }
}
