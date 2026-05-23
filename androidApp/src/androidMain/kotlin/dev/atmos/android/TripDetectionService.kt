package dev.atmos.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.atmos.shared.location.TripDetectionService as TripDetectionServiceConstants

/**
 * Foreground Service that keeps the process alive during active trip detection.
 * Required by Android 9+ for background GPS access.
 *
 * Shows a persistent "Atmos is tracking your trip" notification while a trip is active.
 * Posts a "Trip detected" notification when a trip completes.
 */
class TripDetectionService : Service() {

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.getStringExtra(TripDetectionServiceConstants.EXTRA_ACTION)) {
                TripDetectionServiceConstants.ACTION_START -> {
                    startForeground(NOTIF_ID_TRACKING, buildTrackingNotification())
                }
                TripDetectionServiceConstants.ACTION_STOP -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                TripDetectionServiceConstants.ACTION_TRIP_DETECTED -> {
                    // Post a separate "tap to review" notification
                    notificationManager.notify(NOTIF_ID_TRIP_READY, buildTripReadyNotification())
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } catch (_: SecurityException) {
            // API 34+: startForeground() with type=location throws if permission not granted.
            // Shut down gracefully instead of crashing.
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trip Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shown while Atmos is tracking an active trip"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildTrackingNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Tracking your trip…")
        .setContentText("Atmos will ask you to confirm when you arrive.")
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun buildTripReadyNotification(): android.app.Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trip detected")
            .setContentText("Tap to confirm and log your carbon footprint.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    companion object {
        private const val CHANNEL_ID       = "atmos_trip_tracking"
        private const val NOTIF_ID_TRACKING = 1001
        private const val NOTIF_ID_TRIP_READY = 1002
    }
}
