package dev.atmos.shared.location

import androidx.compose.runtime.compositionLocalOf

/**
 * Platform bridge for runtime permission requests.
 *
 * Android: implemented by AndroidPermissionRequester in androidApp (holds ActivityResultLauncher).
 * iOS: CLLocationManager.requestAlwaysAuthorization() is called directly from TripDetector.ios.kt;
 *      the NoOpPermissionRequester is used as the CompositionLocal value.
 * Previews: NoOpPermissionRequester is the default — no dialogs appear.
 */
interface PermissionRequester {
    /**
     * Request location permission.
     * Android: triggers the mandatory two-step sequence (foreground first, then background).
     * iOS: no-op (CLLocationManager handles it internally via TripDetector.requestPermissions()).
     * [onResult] receives `true` only when background ("always") access is granted.
     */
    fun requestLocation(onResult: (granted: Boolean) -> Unit)

    /**
     * Request notification permission (Android 13+ / iOS).
     * [onResult] receives the user's choice — trip detection doesn't depend on this.
     */
    fun requestNotification(onResult: (granted: Boolean) -> Unit)
}

/** Safe no-op default used in iOS and Compose previews. */
class NoOpPermissionRequester : PermissionRequester {
    override fun requestLocation(onResult: (Boolean) -> Unit) = Unit
    override fun requestNotification(onResult: (Boolean) -> Unit) = Unit
}

/**
 * CompositionLocal that provides a [PermissionRequester] to the Compose tree.
 * Injected from MainActivity on Android; defaults to [NoOpPermissionRequester] everywhere else.
 */
val LocalPermissionRequester = compositionLocalOf<PermissionRequester> {
    NoOpPermissionRequester()
}
