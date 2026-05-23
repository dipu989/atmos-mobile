package dev.atmos.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.atmos.shared.location.TripDetectionServiceConsts

/**
 * Foreground Service that keeps the process alive during active trip detection.
 * Required by Android 9+ for background GPS access.
 *
 * Lifecycle:
 *  - ACTION_START       → call startForeground() with trip-tracking notification
 *  - ACTION_UPDATE      → refresh notification text (mode/distance changed)
 *  - ACTION_STOP        → stopForeground + stopSelf
 *  - ACTION_TRIP_DETECTED → post "tap to review" notification (system auto-end path)
 *  - ACTION_TRIP_SAVED  → post "trip saved" notification (user-initiated end path)
 *
 * Note: Notification redesign (live timer, action buttons) is task 3.1.
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
            when (intent?.getStringExtra(TripDetectionServiceConsts.EXTRA_ACTION)) {
                TripDetectionServiceConsts.ACTION_START -> {
                    startForeground(NOTIF_ID_TRACKING, buildTrackingNotification())
                }
                TripDetectionServiceConsts.ACTION_UPDATE -> {
                    // Refresh notification when mode or distance changes
                    val mode    = intent.getStringExtra(TripDetectionServiceConsts.EXTRA_MODE) ?: ""
                    val distKm  = intent.getFloatExtra(TripDetectionServiceConsts.EXTRA_DIST_KM, 0f)
                    notificationManager.notify(
                        NOTIF_ID_TRACKING,
                        buildTrackingNotification(mode, distKm),
                    )
                }
                TripDetectionServiceConsts.ACTION_STOP -> {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                TripDetectionServiceConsts.ACTION_TRIP_DETECTED -> {
                    notificationManager.notify(NOTIF_ID_TRIP_READY, buildTripReadyNotification())
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                TripDetectionServiceConsts.ACTION_TRIP_SAVED -> {
                    val distKm = intent.getFloatExtra(TripDetectionServiceConsts.EXTRA_DIST_KM, 0f)
                    notificationManager.notify(NOTIF_ID_TRIP_READY, buildTripSavedNotification(distKm))
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        } catch (_: SecurityException) {
            // API 34+: startForeground() with foregroundServiceType=location throws if
            // ACCESS_FINE_LOCATION isn't granted. Shut down gracefully instead of crashing.
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

    private fun buildTrackingNotification(
        mode: String = "",
        distKm: Float = 0f,
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(if (mode.isBlank()) "Tracking your trip…" else "Tracking · $mode")
        .setContentText(
            if (distKm > 0f) "%.1f km recorded so far".format(distKm)
            else "Atmos will ask you to confirm when you arrive."
        )
        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
        .setOngoing(true)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun buildTripReadyNotification(): android.app.Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trip detected")
            .setContentText("Tap to confirm and log your carbon footprint.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun buildTripSavedNotification(distKm: Float): android.app.Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Trip saved ✓")
            .setContentText("%.1f km recorded. Tap to view.".format(distKm))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    companion object {
        private const val CHANNEL_ID          = "atmos_trip_tracking"
        private const val NOTIF_ID_TRACKING   = 1001
        private const val NOTIF_ID_TRIP_READY = 1002
    }
}
