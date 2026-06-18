package dev.atmos.shared.ui

import kotlinx.coroutines.flow.MutableStateFlow

object NotificationState {
    val pendingInsightId  = MutableStateFlow<String?>(null)
    /** Set when the user taps a "possible duplicate" push notification. */
    val pendingActivityId = MutableStateFlow<String?>(null)
    /**
     * Incremented when the Gmail OAuth deep link (`atmos://gmail/connected`) fires.
     * AtmosApp observes this to re-fetch Gmail status without requiring the user to
     * navigate away from and back to the Profile screen.
     */
    val gmailOAuthCompleted = MutableStateFlow(0)
    /**
     * Incremented when the Gmail OAuth error deep link (`atmos://gmail/error`) fires.
     * AtmosApp observes this to surface a snackbar / error state so the user is not
     * left stranded after a failed OAuth exchange.
     */
    val gmailOAuthFailed = MutableStateFlow(0)
}
