package dev.atmos.shared.location

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Triggers the platform's permission-request dialogs.
 * Each platform's Compose entry point (MainActivity / MainViewController) provides
 * a real implementation via [LocalPermissionRequester].
 */
interface PermissionRequester {
    /** Ask for fine + background location access. */
    fun requestLocation()
    /** Ask for post-notification permission (Android 13+ / iOS). */
    fun requestNotification()
}

/** Default no-op — used in Compose previews and tests where no real Activity exists. */
object NoOpPermissionRequester : PermissionRequester {
    override fun requestLocation() = Unit
    override fun requestNotification() = Unit
}

/**
 * Composition local carrying the active [PermissionRequester].
 * Provide a real implementation from each platform's entry point.
 * Falls back to [NoOpPermissionRequester] if nothing is provided.
 */
val LocalPermissionRequester = staticCompositionLocalOf<PermissionRequester> { NoOpPermissionRequester }
