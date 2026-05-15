package dev.atmos.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.home.previewHomeUiState
import dev.atmos.shared.ui.insights.InsightsScreen
import dev.atmos.shared.ui.onboarding.OnboardingScreen
import dev.atmos.shared.ui.profile.AppearanceMode
import dev.atmos.shared.ui.profile.ProfileScreen
import dev.atmos.shared.ui.profile.previewProfileUiState
import dev.atmos.shared.ui.theme.AtmosTheme
import dev.atmos.shared.util.currentDateLabel
import dev.atmos.shared.util.currentGreeting

private sealed class Screen {
    data object Onboarding : Screen()
    data object Home       : Screen()
    data object Profile    : Screen()
    data object Insights   : Screen()
}

@Composable
fun AtmosApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    var appearanceMode by remember { mutableStateOf(AppearanceMode.SYSTEM) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    AtmosTheme(appearanceMode = appearanceMode) {
        when (screen) {
            Screen.Onboarding -> OnboardingScreen(
                onGetStarted = { screen = Screen.Home },
                onAlreadyHaveAccount = { screen = Screen.Home },
            )

            Screen.Home -> HomeScreen(
                state = previewHomeUiState.copy(
                    greeting = currentGreeting(),
                    dateLabel = currentDateLabel(),
                ),
                onNavigateToProfile = { screen = Screen.Profile },
                onNavigateToInsights = { screen = Screen.Insights },
                onFabClick = { /* Log Activity — coming soon */ },
            )

            Screen.Profile -> ProfileScreen(
                state = previewProfileUiState.copy(
                    preferences = previewProfileUiState.preferences.copy(
                        pushNotificationsEnabled = notificationsEnabled,
                        appearanceMode = appearanceMode,
                    ),
                ),
                onBack = { screen = Screen.Home },
                onNavigateToHome = { screen = Screen.Home },
                onAppearanceChange = { mode -> appearanceMode = mode },
                onNotificationsToggle = { enabled -> notificationsEnabled = enabled },
                onSignOut = { screen = Screen.Home },
                onDeleteAccount = { screen = Screen.Home },
            )

            Screen.Insights -> InsightsScreen(
                entries = previewHomeUiState.insights,
                onNavigateToHome = { screen = Screen.Home },
                onFabClick = { /* Log Activity — coming soon */ },
            )
        }
    }
}
