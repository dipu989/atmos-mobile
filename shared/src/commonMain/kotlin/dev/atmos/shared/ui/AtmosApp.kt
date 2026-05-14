package dev.atmos.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.home.previewHomeUiState
import dev.atmos.shared.ui.profile.ProfileScreen
import dev.atmos.shared.ui.profile.previewProfileUiState
import dev.atmos.shared.ui.theme.AtmosTheme

private sealed class Screen {
    data object Home    : Screen()
    data object Profile : Screen()
}

@Composable
fun AtmosApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    AtmosTheme {
        when (screen) {
            Screen.Home -> HomeScreen(
                state = previewHomeUiState,
                onNavigateToProfile = { screen = Screen.Profile },
            )
            Screen.Profile -> ProfileScreen(
                state = previewProfileUiState,
                onBack = { screen = Screen.Home },
            )
        }
    }
}
