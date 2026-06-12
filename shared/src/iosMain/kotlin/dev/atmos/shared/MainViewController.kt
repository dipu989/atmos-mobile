package dev.atmos.shared

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import dev.atmos.shared.location.IosPermissionRequester
import dev.atmos.shared.location.LocalPermissionRequester
import dev.atmos.shared.ui.AtmosApp
import dev.atmos.shared.ui.common.LocalShareLauncher

fun MainViewController() = ComposeUIViewController {
    val permissionRequester = remember { IosPermissionRequester() }
    val shareLauncher       = remember { IosShareLauncher() }
    CompositionLocalProvider(
        LocalPermissionRequester provides permissionRequester,
        LocalShareLauncher       provides shareLauncher,
    ) {
        AtmosApp()
    }
}
