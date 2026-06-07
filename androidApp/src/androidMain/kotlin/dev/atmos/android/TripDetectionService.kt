package dev.atmos.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import dev.atmos.shared.location.TripDetectionServiceConsts

/**
 * Foreground Service that keeps the process alive during active trip detection.
 * Required by Android 9+ for background GPS access.
 *
 * Lifecycle:
 *  - ACTION_START        → call startForeground() with trip-tracking notification
 *  - ACTION_UPDATE       → refresh notification text (mode/distance/phase changed)
 *  - ACTION_STOP         → stopForeground + stopSelf
 *  - ACTION_TRIP_DETECTED → post "tap to review" notification (system auto-end path)
 *  - ACTION_TRIP_SAVED   → post "trip saved" notification (user-initiated end path)
 *
 * The tracking notification features:
 *  - Live chronometer counting up from session start
 *  - Content text: "X.X km so far" (Active) or "Trip ending…" (LegEnding)
 *  - Two action buttons: "End Trip" and "Discard" → fire to [TripActionReceiver]
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
                    // Use current time as chronometer base — will be corrected on first ACTION_UPDATE
                    startForeground(NOTIF_ID_TRACKING, buildTrackingNotification())
                }
                TripDetectionServiceConsts.ACTION_UPDATE -> {
                    val mode     = intent.getStringExtra(TripDetectionServiceConsts.EXTRA_MODE) ?: ""
                    val distKm   = intent.getFloatExtra(TripDetectionServiceConsts.EXTRA_DIST_KM, 0f)
                    val startMs  = intent.getLongExtra(
                        TripDetectionServiceConsts.EXTRA_START_TIME_MS,
                        System.currentTimeMillis(),
                    )
                    val phase    = intent.getStringExtra(TripDetectionServiceConsts.EXTRA_PHASE)
                        ?: TripDetectionServiceConsts.PHASE_ACTIVE
                    notificationManager.notify(
                        NOTIF_ID_TRACKING,
                        buildTrackingNotification(mode, distKm, startMs, phase),
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

    /**
     * The live foreground notification shown throughout an active session.
     *
     * - Title   : mode display label ("Driving", "Walking"), or "Detecting trip…" before confirmation
     * - Content : "X.X km so far" when active, "Trip ending…" during the 60 s grace window
     * - Timer   : chronometer counting up from [startMs] (session wall-clock start)
     * - Actions : "End Trip" and "Discard" — both routed through [TripActionReceiver]
     */
    private fun buildTrackingNotification(
        mode: String = "",
        distKm: Float = 0f,
        startMs: Long = System.currentTimeMillis(),
        phase: String = TripDetectionServiceConsts.PHASE_ACTIVE,
    ): android.app.Notification {
        val title = if (mode.isBlank()) "Detecting trip…" else modeDisplayLabel(mode)
        val content = when {
            phase == TripDetectionServiceConsts.PHASE_LEG_ENDING -> "Trip ending…"
            distKm > 0f -> "%.1f km so far".format(distKm)
            else        -> "Tracking your route"
        }
        // Convert wall-clock start time to elapsed-realtime base (what Chronometer needs)
        val chronometerBase = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startMs)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setUsesChronometer(true)
            .setWhen(chronometerBase)
            .setShowWhen(true)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(buildEndTripAction())
            .addAction(buildDiscardAction())
            .build()
    }

    private fun buildEndTripAction(): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(
            this,
            REQ_CODE_END_TRIP,
            Intent(this, TripActionReceiver::class.java)
                .setAction(TripDetectionServiceConsts.ACTION_END_TRIP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(0, "End Trip", pi)
    }

    private fun buildDiscardAction(): NotificationCompat.Action {
        val pi = PendingIntent.getBroadcast(
            this,
            REQ_CODE_DISCARD,
            Intent(this, TripActionReceiver::class.java)
                .setAction(TripDetectionServiceConsts.ACTION_DISCARD),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(0, "Discard", pi)
    }

    private fun buildTripReadyNotification(): android.app.Notification {
        val tap = PendingIntent.getActivity(
            this, REQ_CODE_OPEN_APP,
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
            this, REQ_CODE_OPEN_APP,
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun modeDisplayLabel(mode: String) = when (mode) {
        "DRIVING"        -> "Driving"
        "WALKING"        -> "Walking"
        "CAB"            -> "Cab"
        "TWO_WHEELER"    -> "Two-wheeler"
        "AUTO_RICKSHAW"  -> "Auto"
        "PUBLIC_TRANSIT" -> "Transit"
        "BUS"            -> "Bus"
        "METRO"          -> "Metro"
        "TRAIN"          -> "Train"
        "CYCLING"        -> "Cycling"
        "FLIGHT"         -> "Flight"
        else             -> "Trip"
    }

    companion object {
        private const val CHANNEL_ID           = "atmos_trip_tracking"
        private const val NOTIF_ID_TRACKING    = 1001
        private const val NOTIF_ID_TRIP_READY  = 1002
        private const val REQ_CODE_END_TRIP    = 2001
        private const val REQ_CODE_DISCARD     = 2002
        private const val REQ_CODE_OPEN_APP    = 2003
    }
}
