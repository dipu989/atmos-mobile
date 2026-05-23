package dev.atmos.shared.location

import android.content.Context

// ── Context holder ────────────────────────────────────────────────────────────

/**
 * Stores the application context before Compose starts.
 * Called from MainActivity.onCreate() before setContent {}.
 */
object TripDetectorHolder {
    internal var appContext: Context? = null
    private var instance: AndroidTripDetector? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): AndroidTripDetector {
        val ctx = requireNotNull(appContext) {
            "TripDetectorHolder.init(context) must be called before get()"
        }
        return instance ?: AndroidTripDetector(ctx).also { instance = it }
    }
}

// ── actual factory ────────────────────────────────────────────────────────────

actual fun createTripDetector(): TripDetector = TripDetectorHolder.get()

// ── Android stub — full state machine wired in task 2.3 ──────────────────────

class AndroidTripDetector(private val context: Context) : TripDetector {
    override fun startMonitoring() = Unit
    override fun stopMonitoring() = Unit
    override fun requestPermissions() = Unit
    override fun manualEndAndSave() = Unit
    override fun discardSession() = Unit
    override fun handleTransition(event: VehicleActivity) = Unit
}
