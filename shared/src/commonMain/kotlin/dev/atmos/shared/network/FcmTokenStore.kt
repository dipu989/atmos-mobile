package dev.atmos.shared.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// In-memory FCM token holder. Updated by Android-side code (MainActivity, AtmosFirebaseMessagingService)
// and observed by AtmosApp to trigger device registration whenever the token changes.
// The token is also persisted to Settings so it survives process death and can be seeded on cold start.
object FcmTokenStore {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    fun update(newToken: String?) {
        _token.value = newToken
    }
}
