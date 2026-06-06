package dev.atmos.shared.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import dev.atmos.shared.auth.AppTokenStore
import dev.atmos.shared.location.TripDetectorState
import dev.atmos.shared.location.createTripDetector
import dev.atmos.shared.auth.AuthState
import dev.atmos.shared.auth.AuthUser
import dev.atmos.shared.auth.GoogleSignInCallback
import dev.atmos.shared.auth.createGoogleSignInLauncher
import dev.atmos.shared.network.AuthService
import dev.atmos.shared.ui.activities.ActivitiesScreen
import dev.atmos.shared.ui.auth.ForgotPasswordScreen
import dev.atmos.shared.ui.home.InsightEntry
import dev.atmos.shared.ui.insightdetail.InsightDetailScreen
import dev.atmos.shared.ui.auth.LoginScreen
import dev.atmos.shared.ui.auth.SignUpScreen
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.home.PendingTripEntry
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
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

private sealed class Screen {
    data object Onboarding     : Screen()
    data object Login          : Screen()
    data object SignUp         : Screen()
    data object ForgotPassword : Screen()
    data object Home           : Screen()
    data object Activities     : Screen()
    data object TripDetail     : Screen()
    data object InsightDetail  : Screen()
    data object Profile        : Screen()
    data object Insights       : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtmosApp() {

    // ── Auth — determine initial screen from persisted token ─────────────────
    val tokenStore = remember { AppTokenStore.instance }
    val authService = remember { AuthService() }
    val googleSignInLauncher = remember { createGoogleSignInLauncher() }

    var screen by remember {
        // Skip onboarding/auth if the user is already signed in
        mutableStateOf<Screen>(
            if (tokenStore.isLoggedIn) Screen.Home else Screen.Onboarding
        )
    }

    // Restore AuthState from persisted tokens on first composition
    LaunchedEffect(Unit) {
        if (tokenStore.isLoggedIn) {
            AuthState.onSignedIn(
                AuthUser(
                    id          = tokenStore.getUserId() ?: "",
                    email       = tokenStore.getUserEmail() ?: "",
                    displayName = tokenStore.getDisplayName() ?: "",
                    avatarUrl   = tokenStore.getAvatarUrl() ?: "",
                )
            )
        }
    }

    // ── Google Sign-In state ─────────────────────────────────────────────────
    var googleSignInLoading by remember { mutableStateOf(false) }
    var googleSignInError   by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    /**
     * Launches the platform Google Sign-In flow, then exchanges the ID token
     * with the Atmos backend. On success, persists tokens and navigates to Home.
     */
    fun handleGoogleSignIn() {
        if (googleSignInLoading) return
        googleSignInLoading = true
        googleSignInError   = null

        googleSignInLauncher.launch(object : GoogleSignInCallback {
            override fun onResult(idToken: String?, error: String?, cancelled: Boolean) {
                if (cancelled) {
                    // User dismissed the picker — reset loading, show nothing
                    googleSignInLoading = false
                    return
                }
                if (idToken == null) {
                    // Platform-level error (e.g. no Google account on device)
                    googleSignInLoading = false
                    googleSignInError = error ?: "Sign-in failed. Please try again."
                    return
                }

                // Exchange Google ID token for Atmos JWT
                scope.launch {
                    val result = authService.signInWithGoogle(idToken)
                    result.onSuccess { response ->
                        // Persist tokens
                        tokenStore.save(response)
                        // Update in-memory auth state
                        AuthState.onSignedIn(
                            AuthUser(
                                id          = response.user.id,
                                email       = response.user.email,
                                displayName = response.user.displayName,
                                avatarUrl   = response.user.avatarUrl,
                            )
                        )
                        googleSignInLoading = false
                        screen = Screen.Home
                    }.onFailure { e ->
                        googleSignInLoading = false
                        googleSignInError   = e.message ?: "Sign-in failed. Please try again."
                    }
                }
            }
        })
    }

    fun handleSignOut() {
        tokenStore.clear()
        AuthState.onSignedOut()
        screen = Screen.Onboarding
    }

    // ── Trip detector StateFlows ─────────────────────────────────────────────
    val tripDetector = remember { createTripDetector() }
    val ongoingSession  by TripDetectorState.ongoingSession.collectAsState()
    val pendingSession  by TripDetectorState.pendingSession.collectAsState()
    val recentlySaved   by TripDetectorState.recentlySaved.collectAsState()

    // ── Home UI ──────────────────────────────────────────────────────────────
    var appearanceMode by remember { mutableStateOf(AppearanceMode.SYSTEM) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showLogActivity by remember { mutableStateOf(false) }
    var tripToEdit       by remember { mutableStateOf<PendingTripEntry?>(null) }
    var selectedTrip     by remember { mutableStateOf<RecentActivityEntry?>(null) }
    var selectedInsight  by remember { mutableStateOf<InsightEntry?>(null) }
    var homeIsLoading    by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2_000)
        homeIsLoading = false
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── "Trip saved · X km [Edit]" snackbar ─────────────────────────────────
    LaunchedEffect(recentlySaved) {
        val saved = recentlySaved ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message     = "Trip saved · ${saved.totalDistKm.toDisplayString()} km",
            actionLabel = "Edit",
            withDismissAction = false,
        )
        if (result == SnackbarResult.ActionPerformed) {
            // Pre-fill LogActivitySheet with the first leg's mode + total distance
            tripToEdit = PendingTripEntry(
                mode           = saved.firstLegMode
                    ?.let { TransportModeType.valueOf(it.name) }
                    ?: TransportModeType.DRIVING,
                origin         = "",
                destination    = "",
                distanceKm     = saved.totalDistKm,
                durationMin    = 0,
                estimatedKgCO2 = 0f,
            )
            showLogActivity = true
        }
    }

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
                    onSignIn             = { screen = Screen.Home },  // email/password (stub)
                    onNavigateToSignUp   = { screen = Screen.SignUp },
                    onForgotPassword     = { screen = Screen.ForgotPassword },
                    onGoogleSignIn       = { handleGoogleSignIn() },
                    googleSignInLoading  = googleSignInLoading,
                    googleSignInError    = googleSignInError,
                )

                Screen.SignUp -> SignUpScreen(
                    onCreateAccount    = { screen = Screen.Home },    // email/password (stub)
                    onNavigateToSignIn = { screen = Screen.Login },
                    onGoogleSignIn     = { handleGoogleSignIn() },
                    googleSignInLoading = googleSignInLoading,
                    googleSignInError   = googleSignInError,
                )

                Screen.ForgotPassword -> ForgotPasswordScreen(
                    onBack         = { screen = Screen.Login },
                    onBackToSignIn = { screen = Screen.Login },
                )

                Screen.Home -> HomeScreen(
                    state = previewHomeUiState.copy(
                        greeting        = currentGreeting(),
                        dateLabel       = currentDateLabel(),
                        isLoading       = homeIsLoading,
                        ongoingSession  = ongoingSession,
                        pendingSession  = pendingSession,
                    ),
                    onNavigateToProfile    = { screen = Screen.Profile },
                    onNavigateToActivities = { screen = Screen.Activities },
                    onNavigateToInsights   = { screen = Screen.Insights },
                    onRetry                = { homeIsLoading = true },
                    onFabClick             = { tripToEdit = null; showLogActivity = true },
                    onConfirmPendingSession = {
                        pendingSession?.let { tripDetector.confirmPendingSession(it.sessionId) }
                    },
                    onDismissPendingSession = {
                        pendingSession?.let { tripDetector.dismissPendingSession(it.sessionId) }
                    },
                    onEditPendingSession    = {
                        pendingSession?.let { s ->
                            tripToEdit = PendingTripEntry(
                                mode           = s.legs.firstOrNull()
                                    ?.let { TransportModeType.valueOf(it.mode.name) }
                                    ?: TransportModeType.DRIVING,
                                origin         = "",
                                destination    = "",
                                distanceKm     = s.totalDistKm,
                                durationMin    = s.totalDurationMin,
                                estimatedKgCO2 = 0f,
                            )
                            showLogActivity = true
                        }
                    },
                    onTripClick            = { entry -> selectedTrip = entry; screen = Screen.TripDetail },
                    onInsightClick         = { entry -> selectedInsight = entry; screen = Screen.InsightDetail },
                    onStopAndSave          = { tripDetector.manualEndAndSave() },
                    onDiscard              = { tripDetector.discardSession() },
                    onResume               = { tripDetector.resumeLeg() },
                )

                Screen.Profile -> ProfileScreen(
                    state = previewProfileUiState.copy(
                        preferences = previewProfileUiState.preferences.copy(
                            pushNotificationsEnabled = notificationsEnabled,
                            appearanceMode = appearanceMode,
                        ),
                    ),
                    onBack                 = { screen = Screen.Home },
                    onNavigateToHome       = { screen = Screen.Home },
                    onNavigateToActivities = { screen = Screen.Activities },
                    onAppearanceChange     = { mode -> appearanceMode = mode },
                    onNotificationsToggle  = { enabled -> notificationsEnabled = enabled },
                    onSignOut    = { handleSignOut() },
                    onDeleteAccount = { handleSignOut() },
                    onFabClick   = { showLogActivity = true },
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
                    groupedEntries   = previewAllActivities,
                    onNavigateToHome = { screen = Screen.Home },
                    onTripClick      = { entry -> selectedTrip = entry; screen = Screen.TripDetail },
                    onFabClick       = { showLogActivity = true },
                )

                Screen.InsightDetail -> selectedInsight?.let { entry ->
                    InsightDetailScreen(
                        entry     = entry,
                        onBack    = { screen = Screen.Home },
                        onLogTrip = { showLogActivity = true },
                    )
                }

                Screen.Insights -> InsightsScreen(
                    entries        = previewHomeUiState.insights,
                    onBack         = { screen = Screen.Home },
                    onInsightClick = { entry -> selectedInsight = entry; screen = Screen.InsightDetail },
                )
            }

            // ── Log Activity sheet — global, shown over any screen ───────────
            if (showLogActivity) {
                LogActivitySheet(
                    prefill   = tripToEdit?.let { t ->
                        LogActivityPrefill(origin = t.origin, destination = t.destination, mode = t.mode)
                    },
                    onDismiss    = { showLogActivity = false; tripToEdit = null },
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
