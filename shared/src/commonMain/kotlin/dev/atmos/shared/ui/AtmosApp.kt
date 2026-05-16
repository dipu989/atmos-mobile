package dev.atmos.shared.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.home.previewHomeUiState
import dev.atmos.shared.ui.insights.InsightsScreen
import dev.atmos.shared.ui.logactivity.LogActivitySheet
import dev.atmos.shared.ui.onboarding.OnboardingScreen
import dev.atmos.shared.ui.profile.AppearanceMode
import dev.atmos.shared.ui.profile.ProfileScreen
import dev.atmos.shared.ui.profile.previewProfileUiState
import dev.atmos.shared.ui.theme.AtmosTheme
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.util.currentDateLabel
import dev.atmos.shared.util.currentGreeting
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Onboarding : Screen()
    data object Home       : Screen()
    data object Profile    : Screen()
    data object Insights   : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtmosApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    var appearanceMode by remember { mutableStateOf(AppearanceMode.SYSTEM) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showLogActivity by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    AtmosTheme(appearanceMode = appearanceMode) {
        val colors = LocalAtmosColors.current

        androidx.compose.material3.Scaffold(
            containerColor = colors.background,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary,
                        actionColor = HorizonBlue,
                    )
                }
            },
        ) { _ ->
            when (screen) {
                Screen.Onboarding -> OnboardingScreen(
                    onGetStarted = { screen = Screen.Home },
                    onAlreadyHaveAccount = { screen = Screen.Home },
                )

                Screen.Home -> HomeScreen(
                    state = previewHomeUiState.copy(
                        greeting  = currentGreeting(),
                        dateLabel = currentDateLabel(),
                    ),
                    onNavigateToProfile = { screen = Screen.Profile },
                    onNavigateToInsights = { screen = Screen.Insights },
                    onFabClick = { showLogActivity = true },
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
                    onFabClick = { showLogActivity = true },
                )

                Screen.Insights -> InsightsScreen(
                    entries = previewHomeUiState.insights,
                    onNavigateToHome = { screen = Screen.Home },
                    onFabClick = { showLogActivity = true },
                )
            }

            // ── Log Activity sheet — global, shown over any screen ────────────
            if (showLogActivity) {
                LogActivitySheet(
                    onDismiss = { showLogActivity = false },
                    onTripLogged = { trip ->
                        showLogActivity = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Trip logged — ${trip.origin} → ${trip.destination} · ${
                                    if (trip.estimatedKgCO2 == 0f) "Zero emissions 🌿"
                                    else "${trip.estimatedKgCO2.toDisplayString()} kg CO₂"
                                }"
                            )
                        }
                    },
                )
            }
        }
    }
}

private fun Float.toDisplayString(): String {
    if (this == 0f) return "0"
    if (this % 1f == 0f) return toInt().toString()
    val intPart = toInt()
    return "$intPart.${((this - intPart) * 10).toInt()}"
}
