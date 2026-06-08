package dev.atmos.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import dev.atmos.shared.location.LocalPermissionRequester
import dev.atmos.shared.location.NoOpPermissionRequester
import dev.atmos.shared.ui.AtmosApp

fun MainViewController() = ComposeUIViewController {
    // TODO: replace NoOp with a real IosPermissionRequester that calls
    //       CLLocationManager.requestAlwaysAuthorization() and
    //       UNUserNotificationCenter.requestAuthorization(options:)
    CompositionLocalProvider(LocalPermissionRequester provides NoOpPermissionRequester) {
        AtmosApp()
    }
}
