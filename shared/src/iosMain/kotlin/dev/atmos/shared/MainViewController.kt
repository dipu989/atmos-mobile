package dev.atmos.shared

import androidx.compose.ui.window.ComposeUIViewController
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.theme.AtmosTheme

fun MainViewController() = ComposeUIViewController {
    AtmosTheme {
        HomeScreen()
    }
}
