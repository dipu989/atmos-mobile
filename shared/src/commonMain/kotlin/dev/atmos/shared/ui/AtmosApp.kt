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
import dev.atmos.shared.auth.AuthState
import dev.atmos.shared.auth.AuthUser
import dev.atmos.shared.auth.GoogleSignInCallback
import dev.atmos.shared.auth.createGoogleSignInLauncher
import dev.atmos.shared.db.DatabaseProvider
import dev.atmos.shared.db.TripRepositoryImpl
import dev.atmos.shared.db.groupByDateLabel
import dev.atmos.shared.db.toRecentActivityEntry
import dev.atmos.shared.location.LocalPermissionRequester
import dev.atmos.shared.location.LocationPermissionState
import dev.atmos.shared.location.TripDetectorState
import dev.atmos.shared.location.createTripDetector
import dev.atmos.shared.network.ActivityService
import dev.atmos.shared.network.AuthService
import dev.atmos.shared.network.TimelineService
import dev.atmos.shared.network.backendMode
import dev.atmos.shared.ui.home.TodayImpact
import dev.atmos.shared.ui.home.WeeklyDataPoint
import dev.atmos.shared.ui.activities.ActivitiesScreen
import dev.atmos.shared.ui.auth.ForgotPasswordScreen
import dev.atmos.shared.ui.auth.LoginScreen
import dev.atmos.shared.ui.auth.SignUpScreen
import dev.atmos.shared.ui.home.HomeScreen
import dev.atmos.shared.ui.home.InsightEntry
import dev.atmos.shared.ui.home.PendingTripEntry
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.home.previewHomeUiState
import dev.atmos.shared.ui.insights.InsightsScreen
import dev.atmos.shared.ui.insightdetail.InsightDetailScreen
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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

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
    val activityService = remember { ActivityService() }
    val timelineService = remember { TimelineService() }
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
    val ongoingSession      by TripDetectorState.ongoingSession.collectAsState()
    val pendingSession      by TripDetectorState.pendingSession.collectAsState()
    val recentlySaved       by TripDetectorState.recentlySaved.collectAsState()
    val locationPermState   by TripDetectorState.permissionState.collectAsState()
    val notifGranted        by TripDetectorState.notificationsGranted.collectAsState()

    val locationGranted = locationPermState == LocationPermissionState.GRANTED ||
                          locationPermState == LocationPermissionState.BACKGROUND_ONLY
    val permReq = LocalPermissionRequester.current

    // ── Confirmed sessions from DB ────────────────────────────────────────────
    // TripDetector.init() guarantees DatabaseProvider is ready before we reach here.
    val repo = remember { TripRepositoryImpl(DatabaseProvider.database) }
    val confirmedSessions by repo.observeConfirmedSessions().collectAsState(initial = emptyList())
    val groupedActivities = remember(confirmedSessions) {
        confirmedSessions.map { it.toRecentActivityEntry() }.groupByDateLabel()
    }
    // Most-recent 3 sessions for the HomeScreen "Recent Activity" card
    val recentActivityEntries = remember(confirmedSessions) {
        confirmedSessions.take(3).map { it.toRecentActivityEntry() }
    }

    // ── Home UI ──────────────────────────────────────────────────────────────
    var appearanceMode by remember { mutableStateOf(AppearanceMode.SYSTEM) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var showLogActivity by remember { mutableStateOf(false) }
    var tripToEdit       by remember { mutableStateOf<PendingTripEntry?>(null) }
    var selectedTrip     by remember { mutableStateOf<RecentActivityEntry?>(null) }
    var selectedInsight  by remember { mutableStateOf<InsightEntry?>(null) }
    var homeIsLoading    by remember { mutableStateOf(true) }
    // Non-null when the user tapped Edit on a confirmed DB trip — holds the session to replace.
    var editingSessionId  by remember { mutableStateOf<String?>(null) }
    var editingTimestampMs by remember { mutableStateOf(0L) }

    // ── Timeline data (real CO₂ totals from backend) ──────────────────────────
    // Initialised to neutral zeros — the Home screen skeleton renders while the first fetch runs.
    // Using preview/fake values here would silently display fabricated data on network failure.
    var todayImpact  by remember { mutableStateOf(TodayImpact(kgCO2 = 0f, dailyGoalKgCO2 = 5.0f, percentVsWeeklyAvg = 0)) }
    var weeklyTrend  by remember { mutableStateOf(emptyList<WeeklyDataPoint>()) }

    LaunchedEffect(Unit) {
        delay(2_000)
        homeIsLoading = false
    }

    // Fetch timeline whenever the user is authenticated and lands on Home.
    // Re-fetches automatically after any trip is saved (triggeredBy counter below).
    var timelineTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(screen, timelineTrigger) {
        if (screen != Screen.Home || !tokenStore.isLoggedIn) return@LaunchedEffect
        timelineService.getDaily().onSuccess { daily ->
            todayImpact = TodayImpact(
                kgCO2              = daily.totalKgCo2e,
                dailyGoalKgCO2     = todayImpact.dailyGoalKgCO2,   // keep user-set goal
                percentVsWeeklyAvg = daily.trend.changePct?.toInt() ?: 0,
            )
        }
        timelineService.getWeekly().onSuccess { weekly ->
            if (weekly.days.isNotEmpty()) {
                // Parse week_start to derive each day's real date — avoids the wrong assumption
                // that today is always the last element in the list.
                val weekStartDate = try { LocalDate.parse(weekly.weekStart) } catch (_: Exception) { null }
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                weeklyTrend = weekly.days.mapIndexed { i, day ->
                    val dayDate = weekStartDate?.plus(i, DateTimeUnit.DAY)
                    // ordinal: 0=Mon … 6=Sun (Kotlin enum / java.time.DayOfWeek)
                    val label = when (dayDate?.dayOfWeek?.ordinal) {
                        0 -> "Mon"; 1 -> "Tue"; 2 -> "Wed"; 3 -> "Thu"
                        4 -> "Fri"; 5 -> "Sat"; 6 -> "Sun"
                        else -> listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun").getOrElse(i) { "D${i + 1}" }
                    }
                    WeeklyDataPoint(
                        dayLabel = label,
                        kgCO2    = day.totalKgCo2e,
                        isToday  = dayDate == today,
                    )
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // ── Backend sync helper ──────────────────────────────────────────────────
    /**
     * Fire-and-forget: POST /activities for [sessionId].
     * Stores the returned backend UUID locally; silently ignores network failures.
     * On success, bumps [timelineTrigger] so the Home screen re-fetches CO₂ totals.
     */
    fun syncToBackend(
        sessionId: String,
        mode: TransportModeType,
        distanceKm: Float,
        durationMin: Int,
        startedAtMs: Long,
        endedAtMs: Long,
    ) {
        if (!tokenStore.isLoggedIn || sessionId.isEmpty()) return
        scope.launch {
            activityService.ingestActivity(
                sessionId   = sessionId,
                mode        = mode,
                distanceKm  = distanceKm,
                durationMin = durationMin,
                startedAtMs = startedAtMs,
                endedAtMs   = endedAtMs,
            ).onSuccess { dto ->
                if (dto.id.isNotEmpty()) {
                    repo.updateBackendActivityId(sessionId, dto.id)
                    timelineTrigger++ // only bump on a real 201 — not on 409 no-ops
                }
            }
        }
    }

    // ── Startup backfill — re-sync sessions confirmed before the backend_activity_id column ──
    // Sessions confirmed before the migration (1.sqm) have backend_activity_id = NULL.
    // This effect runs once on first authenticated launch and fires syncToBackend for each,
    // so pre-migration trips eventually appear in the backend's CO₂ timeline.
    LaunchedEffect(Unit) {
        if (!tokenStore.isLoggedIn) return@LaunchedEffect
        val unsynced = try { repo.getUnsyncedConfirmedSessions() } catch (_: Exception) { return@LaunchedEffect }
        unsynced.forEach { swl ->
            val primaryLeg = swl.legs.firstOrNull() ?: return@forEach
            val mode = try { TransportModeType.valueOf(primaryLeg.mode) } catch (_: Exception) { return@forEach }
            syncToBackend(
                sessionId   = swl.session.id,
                mode        = mode,
                distanceKm  = swl.session.total_dist_km.toFloat(),
                durationMin = 0,
                startedAtMs = swl.session.started_at_ms,
                endedAtMs   = swl.session.ended_at_ms ?: swl.session.started_at_ms,
            )
        }
    }

    // ── "Trip saved · X km [Edit]" snackbar + backend sync ──────────────────
    LaunchedEffect(recentlySaved) {
        val saved = recentlySaved ?: return@LaunchedEffect
        // Sync the auto-saved session to the backend immediately
        val nowMs = Clock.System.now().toEpochMilliseconds()
        syncToBackend(
            sessionId   = saved.sessionId,
            mode        = saved.firstLegMode ?: TransportModeType.DRIVING,
            distanceKm  = saved.totalDistKm,
            durationMin = 0,
            startedAtMs = saved.startedAtMs,
            endedAtMs   = nowMs,
        )
        val result = snackbarHostState.showSnackbar(
            message     = "Trip saved · ${saved.totalDistKm.toDisplayString()} km",
            actionLabel = "Edit",
            withDismissAction = false,
        )
        if (result == SnackbarResult.ActionPerformed) {
            // Mark as an edit so onTripLogged replaces the auto-saved session
            editingSessionId   = saved.sessionId
            editingTimestampMs = saved.startedAtMs
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
                    onGetStarted           = { screen = Screen.SignUp },
                    onAlreadyHaveAccount   = { screen = Screen.Login },
                    locationGranted        = locationGranted,
                    notificationsGranted   = notifGranted,
                    onRequestLocation      = { permReq.requestLocation() },
                    onRequestNotifications = { permReq.requestNotification() },
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
                        recentActivity  = recentActivityEntries,
                        todayImpact     = todayImpact,
                        weeklyTrend     = weeklyTrend,
                    ),
                    onNavigateToProfile    = { screen = Screen.Profile },
                    onNavigateToActivities = { screen = Screen.Activities },
                    onNavigateToInsights   = { screen = Screen.Insights },
                    onRetry                = { homeIsLoading = true },
                    onFabClick             = { tripToEdit = null; showLogActivity = true },
                    onConfirmPendingSession = {
                        pendingSession?.let { s ->
                            tripDetector.confirmPendingSession(s.sessionId)
                            val nowMs = Clock.System.now().toEpochMilliseconds()
                            syncToBackend(
                                sessionId   = s.sessionId,
                                mode        = s.legs.firstOrNull()?.mode ?: TransportModeType.DRIVING,
                                distanceKm  = s.totalDistKm,
                                durationMin = s.totalDurationMin,
                                startedAtMs = s.startedAtMs,
                                endedAtMs   = nowMs,
                            )
                        }
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
                            // Capture session identity so onTripLogged can delete-then-replace
                            editingSessionId  = entry.sessionId.ifEmpty { null }
                            editingTimestampMs = entry.timestampMs
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
                        onDelete = {
                            val sessionId = entry.sessionId
                            scope.launch {
                                try {
                                    if (sessionId.isNotEmpty()) repo.deleteSession(sessionId)
                                    screen = Screen.Home  // navigate only after successful delete
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Could not delete trip — please try again")
                                    // stay on TripDetailScreen so the user can retry
                                }
                            }
                        },
                    )
                }

                Screen.Activities -> ActivitiesScreen(
                    groupedEntries   = groupedActivities,
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
                        LogActivityPrefill(
                            origin      = t.origin,
                            destination = t.destination,
                            mode        = t.mode,
                            distanceKm  = t.distanceKm,
                        )
                    },
                    onDismiss    = {
                        showLogActivity    = false
                        tripToEdit         = null
                        editingSessionId   = null
                        editingTimestampMs = 0L
                    },
                    onTripLogged = { trip ->
                        showLogActivity = false
                        tripToEdit      = null
                        // Snapshot + clear before the coroutine runs
                        val sessionBeingEdited  = editingSessionId
                        val originalTimestampMs = editingTimestampMs
                        editingSessionId   = null
                        editingTimestampMs = 0L
                        scope.launch {
                            try {
                                if (sessionBeingEdited != null) {
                                    // Edit flow: atomic delete + replace inside one DB transaction
                                    val editNowMs = Clock.System.now().toEpochMilliseconds()
                                    val newId = repo.updateManualTrip(
                                        oldSessionId = sessionBeingEdited,
                                        mode         = trip.mode.name,
                                        distanceKm   = trip.distanceKm,
                                        timestampMs  = originalTimestampMs,
                                    )
                                    syncToBackend(
                                        sessionId   = newId,
                                        mode        = trip.mode,
                                        distanceKm  = trip.distanceKm,
                                        durationMin = 0,
                                        startedAtMs = originalTimestampMs,
                                        endedAtMs   = editNowMs,  // use current time, not the trip's start
                                    )
                                    snackbarHostState.showSnackbar("Trip updated")
                                } else {
                                    // New-log flow: save with current timestamp
                                    val nowMs = Clock.System.now().toEpochMilliseconds()
                                    val newId = repo.saveManualTrip(
                                        mode        = trip.mode.name,
                                        distanceKm  = trip.distanceKm,
                                        timestampMs = nowMs,
                                    )
                                    syncToBackend(
                                        sessionId   = newId,
                                        mode        = trip.mode,
                                        distanceKm  = trip.distanceKm,
                                        durationMin = 0,
                                        startedAtMs = nowMs,
                                        endedAtMs   = nowMs,
                                    )
                                    snackbarHostState.showSnackbar(
                                        "Trip logged — ${trip.origin} → ${trip.destination} · ${
                                            if (trip.estimatedKgCO2 == 0f) "Zero emissions 🌿"
                                            else "${trip.estimatedKgCO2.toDisplayString()} kg CO₂"
                                        }"
                                    )
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Could not save trip — please try again")
                            }
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
