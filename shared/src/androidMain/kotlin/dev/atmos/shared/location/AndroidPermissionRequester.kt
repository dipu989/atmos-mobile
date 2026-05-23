package dev.atmos.shared.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Android implementation of [PermissionRequester].
 * Injected via [LocalPermissionRequester] from MainActivity.
 *
 * Performs the mandatory two-step location permission sequence:
 *  1. REQUEST ACCESS_FINE_LOCATION (foreground)
 *  2. Only after granted → REQUEST ACCESS_BACKGROUND_LOCATION
 *
 * Android 10+ (API 29) enforces this order — requesting both simultaneously is rejected.
 */
class AndroidPermissionRequester(
    private val fineLocationLauncher: ActivityResultLauncher<String>,
    private val backgroundLocationLauncher: ActivityResultLauncher<String>,
    private val notificationLauncher: ActivityResultLauncher<String>,
) : PermissionRequester {

    private var locationCallback: ((Boolean) -> Unit)? = null
    private var notificationCallback: ((Boolean) -> Unit)? = null

    override fun requestLocation(onResult: (Boolean) -> Unit) {
        locationCallback = onResult

        val ctx = TripDetectorHolder.appContext ?: run {
            onResult(false)
            return
        }

        val fineGranted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            // Fine location already granted — check background
            requestBackgroundIfNeeded(onResult)
        } else {
            // Step 1: request fine location first
            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun requestNotification(onResult: (Boolean) -> Unit) {
        notificationCallback = onResult
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(true)  // Auto-granted on Android 12 and below
        }
    }

    // ── Callbacks from MainActivity launchers ─────────────────────────────────

    fun onFineLocationResult(granted: Boolean) {
        if (granted) {
            requestBackgroundIfNeeded(locationCallback ?: return)
        } else {
            TripDetectorState.updatePermissionState(LocationPermissionState.DENIED)
            locationCallback?.invoke(false)
            locationCallback = null
        }
    }

    fun onBackgroundLocationResult(granted: Boolean) {
        val state = if (granted) {
            LocationPermissionState.GRANTED
        } else {
            LocationPermissionState.BACKGROUND_ONLY  // foreground only
        }
        TripDetectorState.updatePermissionState(state)
        locationCallback?.invoke(granted)
        locationCallback = null
    }

    fun onNotificationResult(granted: Boolean) {
        notificationCallback?.invoke(granted)
        notificationCallback = null
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun requestBackgroundIfNeeded(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Background permission not needed below Android 10
            TripDetectorState.updatePermissionState(LocationPermissionState.GRANTED)
            onResult(true)
            return
        }

        val ctx = TripDetectorHolder.appContext ?: run { onResult(false); return }
        val backgroundGranted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (backgroundGranted) {
            TripDetectorState.updatePermissionState(LocationPermissionState.GRANTED)
            onResult(true)
        } else {
            // Step 2: request background — Android shows a separate system dialog
            locationCallback = onResult
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
}
