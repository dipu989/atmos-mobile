package dev.atmos.android

import com.google.firebase.messaging.FirebaseMessagingService
import com.russhwolf.settings.Settings
import dev.atmos.shared.network.FcmTokenStore

class AtmosFirebaseMessagingService : FirebaseMessagingService() {

    // Called by Firebase when a push token is created or rotated.
    // Persists the token to Settings (survives process death) and updates FcmTokenStore
    // so AtmosApp's LaunchedEffect immediately re-registers with the backend — even while
    // the user is actively signed in without a full cold-start/re-login cycle.
    override fun onNewToken(token: String) {
        Settings().putString("fcm_token", token)
        FcmTokenStore.update(token)
    }
}
