package dev.atmos.shared

import dev.atmos.shared.ui.NotificationState

/**
 * Called from Swift ([iOSApp.swift] `.onOpenURL`) when the Gmail OAuth deep link
 * `atmos://gmail/connected` fires. Increments the counter so AtmosApp's
 * `LaunchedEffect(gmailOAuthCompleted)` re-runs and refreshes Gmail status.
 */
fun handleGmailOAuthCallback() {
    NotificationState.gmailOAuthCompleted.value++
}

/**
 * Called from Swift ([iOSApp.swift] `.onOpenURL`) when the Gmail OAuth error deep link
 * `atmos://gmail/error` fires. Increments the counter so AtmosApp's
 * `LaunchedEffect(gmailOAuthFailed)` can surface an error state to the user.
 */
fun handleGmailOAuthError() {
    NotificationState.gmailOAuthFailed.value++
}
