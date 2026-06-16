package dev.atmos.shared.ui

import kotlinx.coroutines.flow.MutableStateFlow

object NotificationState {
    val pendingInsightId  = MutableStateFlow<String?>(null)
    /** Set when the user taps a "possible duplicate" push notification. */
    val pendingActivityId = MutableStateFlow<String?>(null)
}
