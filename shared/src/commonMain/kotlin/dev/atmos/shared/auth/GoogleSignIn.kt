package dev.atmos.shared.auth

// ── Callback ──────────────────────────────────────────────────────────────────

/**
 * Receives the result of a Google Sign-In flow.
 *
 * Exactly one of [idToken] or [error] will be non-null.
 *
 * This is a regular interface (not `fun interface`) so it exports cleanly to
 * ObjC/Swift as a protocol that the Swift bridge can conform to.
 */
interface GoogleSignInCallback {
    fun onResult(idToken: String?, error: String?)
}

// ── Launcher ──────────────────────────────────────────────────────────────────

/**
 * Platform-specific Google Sign-In launcher.
 *
 * Android actual: AndroidX Credential Manager (no webview — uses bottom sheet).
 * iOS actual:     Native GIDSignIn via a Swift bridge injected at app start.
 */
interface GoogleSignInLauncher {
    fun launch(callback: GoogleSignInCallback)
}

/**
 * Platform factory.
 * Returns [NoOpGoogleSignInLauncher] if the platform holder has not been initialised yet.
 */
expect fun createGoogleSignInLauncher(): GoogleSignInLauncher

// ── No-op fallback ────────────────────────────────────────────────────────────

internal class NoOpGoogleSignInLauncher : GoogleSignInLauncher {
    override fun launch(callback: GoogleSignInCallback) {
        callback.onResult(null, "Google Sign-In is not configured on this platform")
    }
}
