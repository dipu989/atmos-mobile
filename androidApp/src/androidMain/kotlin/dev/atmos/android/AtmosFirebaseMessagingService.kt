package dev.atmos.android

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.russhwolf.settings.Settings
import dev.atmos.shared.network.FcmTokenStore

class AtmosFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Settings().putString("fcm_token", token)
        FcmTokenStore.update(token)
    }

    // Called only when the app is in the foreground — Firebase shows the notification
    // automatically when the app is backgrounded/killed, but we must display it ourselves here.
    override fun onMessageReceived(message: RemoteMessage) {
        val title      = message.notification?.title ?: return
        val body       = message.notification?.body ?: ""
        val type       = message.data["type"]
        val insightId  = message.data["insight_id"]
        val activityId = message.data["activity_id"]

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when (type) {
                "possible_duplicate" -> if (!activityId.isNullOrEmpty()) putExtra("activity_id", activityId)
                else                 -> if (!insightId.isNullOrEmpty())  putExtra("insight_id",  insightId)
            }
        }
        val channel = if (type == "possible_duplicate") CHANNEL_TRIPS else CHANNEL_INSIGHTS
        val notifId = (activityId ?: insightId)?.hashCode() ?: 0
        val pi = PendingIntent.getActivity(
            this,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(notifId, notification)
    }

    companion object {
        const val CHANNEL_INSIGHTS = "atmos_insights"
        const val CHANNEL_TRIPS    = "atmos_trips"
    }
}
