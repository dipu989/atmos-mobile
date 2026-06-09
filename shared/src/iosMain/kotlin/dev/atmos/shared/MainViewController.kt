package dev.atmos.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import dev.atmos.shared.location.IosPermissionRequester
import dev.atmos.shared.location.LocalPermissionRequester
import dev.atmos.shared.ui.AtmosApp

fun MainViewController() = ComposeUIViewController {
    val permissionRequester = remember { IosPermissionRequester() }
    CompositionLocalProvider(LocalPermissionRequester provides permissionRequester) {
        AtmosApp()
    }
}
