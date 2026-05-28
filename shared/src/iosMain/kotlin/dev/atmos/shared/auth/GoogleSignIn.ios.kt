package dev.atmos.shared.auth

/**
 * Holds the Swift-side [GoogleSignInLauncher] implementation.
 *
 * From Swift (iOSApp.swift):
 *   IosGoogleSignInHolder.shared.launcher = SwiftGoogleSignInBridge()
 *
 * The bridge class must be created in Swift (using the Google Sign-In iOS SDK)
 * and assigned here before any sign-in is attempted.
 */
object IosGoogleSignInHolder {
    var launcher: GoogleSignInLauncher? = null
}

actual fun createGoogleSignInLauncher(): GoogleSignInLauncher =
    IosGoogleSignInHolder.launcher ?: NoOpGoogleSignInLauncher()
