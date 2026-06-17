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
