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
import dev.atmos.shared.ui.activities.ActivitiesScreen
import dev.atmos.shared.ui.auth.ForgotPasswordScreen
import dev.atmos.shared.ui.auth.LoginScreen
import dev.atmos.shared.ui.auth.SignUpScreen
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.home.PendingTripEntry
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.previewAllActivities
import dev.atmos.shared.ui.home.previewHomeUiState
import dev.atmos.shared.ui.insights.InsightsScreen
import dev.atmos.shared.ui.logactivity.LogActivityPrefill
import dev.atmos.shared.ui.logactivity.LogActivitySheet
import dev.atmos.shared.ui.onboarding.OnboardingScreen
import dev.atmos.shared.ui.profile.AppearanceMode
import dev.atmos.shared.ui.profile.ProfileScreen
import dev.atmos.shared.ui.profile.previewProfileUiState
import dev.atmos.shared.ui.tripdetail.TripDetailScreen
import dev.atmos.shared.ui.theme.AtmosTheme
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import dev.atmos.shared.util.currentDateLabel
import dev.atmos.shared.util.currentGreeting
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Onboarding     : Screen()
    data object Login          : Screen()
    data object SignUp         : Screen()
    data object ForgotPassword : Screen()
    data object Home           : Screen()
    data object Activities     : Screen()
    data object TripDetail     : Screen()
    data object Profile        : Screen()
    data object Insights       : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtmosApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Onboarding) }
    var appearanceMode by remember { mutableStateOf(AppearanceMode.SYSTEM) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showLogActivity by remember { mutableStateOf(false) }
    var tripToEdit     by remember { mutableStateOf<PendingTripEntry?>(null) }
    var selectedTrip   by remember { mutableStateOf<RecentActivityEntry?>(null) }

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
                    onGetStarted         = { screen = Screen.SignUp },
                    onAlreadyHaveAccount = { screen = Screen.Login },
                )

                Screen.Login -> LoginScreen(
                    onSignIn           = { screen = Screen.Home },
                    onNavigateToSignUp = { screen = Screen.SignUp },
                    onForgotPassword   = { screen = Screen.ForgotPassword },
                )

                Screen.SignUp -> SignUpScreen(
                    onCreateAccount    = { screen = Screen.Home },
                    onNavigateToSignIn = { screen = Screen.Login },
                )

                Screen.ForgotPassword -> ForgotPasswordScreen(
                    onBack        = { screen = Screen.Login },
                    onBackToSignIn = { screen = Screen.Login },
                )

                Screen.Home -> HomeScreen(
                    state = previewHomeUiState.copy(
                        greeting       = currentGreeting(),
                        dateLabel      = currentDateLabel(),
                        // ── EMPTY STATE SIMULATION ──────────────────────────────
                        // Uncomment the two lines below to simulate empty state
//                        recentActivity = emptyList(),
//                        pendingTrip    = null,
                        // ────────────────────────────────────────────────────────
                    ),
                    onNavigateToProfile    = { screen = Screen.Profile },
                    onNavigateToActivities = { screen = Screen.Activities },
                    onFabClick           = { tripToEdit = null; showLogActivity = true },
                    onEditPendingTrip    = { trip -> tripToEdit = trip; showLogActivity = true },
                    onTripClick          = { entry -> selectedTrip = entry; screen = Screen.TripDetail },
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

                Screen.TripDetail -> selectedTrip?.let { entry ->
                    TripDetailScreen(
                        entry          = entry,
                        dailyGoalKgCO2 = previewHomeUiState.todayImpact.dailyGoalKgCO2,
                        onBack         = { screen = Screen.Home },
                        onEdit         = {
                            tripToEdit = PendingTripEntry(
                                mode           = entry.mode,
                                origin         = entry.origin,
                                destination    = entry.destination,
                                distanceKm     = entry.distanceKm,
                                durationMin    = entry.durationMin,
                                estimatedKgCO2 = entry.kgCO2,
                            )
                            showLogActivity = true
                        },
                        onDelete = { screen = Screen.Home },
                    )
                }

                Screen.Activities -> ActivitiesScreen(
                    // ── EMPTY STATE SIMULATION ──────────────────────────────────
                    // Swap emptyList() for previewAllActivities to restore normal data.
                    groupedEntries = emptyList(),
                    // ────────────────────────────────────────────────────────────
                    onNavigateToHome = { screen = Screen.Home },
                    onTripClick = { entry -> selectedTrip = entry; screen = Screen.TripDetail },
                    onFabClick = { showLogActivity = true },
                )

                Screen.Insights -> InsightsScreen(
                    entries = previewHomeUiState.insights,
                    onBack = { screen = Screen.Home },
                )
            }

            // ── Log Activity sheet — global, shown over any screen ────────────
            if (showLogActivity) {
                LogActivitySheet(
                    prefill   = tripToEdit?.let { t ->
                        LogActivityPrefill(origin = t.origin, destination = t.destination, mode = t.mode)
                    },
                    onDismiss = { showLogActivity = false; tripToEdit = null },
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
