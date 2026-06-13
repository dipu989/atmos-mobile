package dev.atmos.shared.ui

import kotlinx.coroutines.flow.MutableStateFlow

object NotificationState {
    val pendingInsightId = MutableStateFlow<String?>(null)
}
