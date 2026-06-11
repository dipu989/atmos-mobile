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
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import dev.atmos.shared.network.AuthResponseDto
import dev.atmos.shared.network.AuthService
import dev.atmos.shared.network.InsightsService
import dev.atmos.shared.network.TimelineService
import dev.atmos.shared.network.UserService
import dev.atmos.shared.network.backendMode
import dev.atmos.shared.network.toInsightEntry
import dev.atmos.shared.network.toRecentActivityEntry
import dev.atmos.shared.ui.home.TodayImpact
import dev.atmos.shared.ui.home.TransportModeEntry
import dev.atmos.shared.ui.home.UserProfile
import dev.atmos.shared.ui.home.WeeklyDataPoint
import kotlin.math.roundToInt
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
import dev.atmos.shared.ui.profile.toInitials
import dev.atmos.shared.util.toDisplayString
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
    val activityService  = remember { ActivityService() }
    val timelineService  = remember { TimelineService() }
    val insightsService  = remember { InsightsService() }
    val userService      = remember { UserService() }
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

    // ── Auth state (Google + email/password) ────────────────────────────────
    var googleSignInLoading by remember { mutableStateOf(false) }
    var googleSignInError   by remember { mutableStateOf<String?>(null) }
    var emailSignInLoading  by remember { mutableStateOf(false) }
    var emailSignInError    by remember { mutableStateOf<String?>(null) }
    var emailSignUpLoading  by remember { mutableStateOf(false) }
    var emailSignUpError    by remember { mutableStateOf<String?>(null) }
    var forgotLoading       by remember { mutableStateOf(false) }
    var forgotError         by remember { mutableStateOf<String?>(null) }
    var forgotSucceeded     by remember { mutableStateOf(false) }
    val forgotJobHolder     = remember { object { var job: Job? = null } }

    // Clear stale auth errors and reset forgot-password state on every screen transition.
    // Also cancels any in-flight password-reset request so it cannot ghost-write state
    // after the user has already navigated away.
    LaunchedEffect(screen) {
        emailSignInError  = null
        emailSignUpError  = null
        googleSignInError = null
        forgotError       = null
        forgotLoading     = false
        forgotSucceeded   = false
        forgotJobHolder.job?.cancel()
        forgotJobHolder.job = null
    }

    val scope = rememberCoroutineScope()

    /** Shared helper — persists the response and navigates to Home. */
    fun onAuthSuccess(response: AuthResponseDto) {
        tokenStore.save(response)
        AuthState.onSignedIn(
            AuthUser(
                id          = response.user.id,
                email       = response.user.email,
                displayName = response.user.displayName,
                avatarUrl   = response.user.avatarUrl ?: "",
            )
        )
        screen = Screen.Home
    }

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
                    authService.signInWithGoogle(idToken)
                        .onSuccess { response ->
                            googleSignInLoading = false
                            onAuthSuccess(response)
                        }
                        .onFailure { e ->
                            googleSignInLoading = false
                            googleSignInError   = e.message ?: "Sign-in failed. Please try again."
                        }
                }
            }
        })
    }

    /** Email + password sign-in against POST /api/v1/auth/login. */
    fun handleEmailSignIn(email: String, password: String) {
        if (emailSignInLoading) return
        emailSignInLoading = true
        emailSignInError   = null
        scope.launch {
            authService.signIn(email.trim(), password)
                .onSuccess { response ->
                    emailSignInLoading = false
                    onAuthSuccess(response)
                }
                .onFailure { e ->
                    emailSignInLoading = false
                    emailSignInError   = e.message ?: "Sign-in failed. Please try again."
                }
        }
    }

    /** Email + password registration against POST /api/v1/auth/register. */
    fun handleEmailSignUp(name: String, email: String, password: String) {
        if (emailSignUpLoading) return
        emailSignUpLoading = true
        emailSignUpError   = null
        scope.launch {
            authService.signUp(displayName = name.trim(), email = email.trim(), password = password)
                .onSuccess { response ->
                    emailSignUpLoading = false
                    onAuthSuccess(response)
                }
                .onFailure { e ->
                    emailSignUpLoading = false
                    emailSignUpError   = e.message ?: "Sign-up failed. Please try again."
                }
        }
    }

    /** Request a password-reset email against POST /api/v1/auth/forgot-password. */
    fun handleForgotPassword(email: String) {
        if (forgotLoading) return
        forgotLoading = true
        forgotError   = null
        // Do NOT reset forgotSucceeded here: on the resend path it is already true and
        // clearing it would animate the success screen away before the request completes.
        forgotJobHolder.job = scope.launch {
            authService.requestPasswordReset(email)
                .onSuccess {
                    forgotLoading   = false
                    forgotSucceeded = true
                }
                .onFailure { e ->
                    forgotLoading = false
                    forgotError   = e.message ?: "Failed to send reset link. Please try again."
                }
        }
    }

    fun handleSignOut() {
        tokenStore.clear()
        AuthState.onSignedOut()
        screen = Screen.Onboarding
    }

    // ── Trip detector StateFlows ─────────────────────────────────────────────
    val tripDetector = remember { createTripDetector() }

    // Start low-power monitoring once on composition entry. Safe to call again on
    // recomposition (remember guards against re-creation, and startMonitoring is
    // idempotent — it returns early if already active).
    LaunchedEffect(Unit) {
        tripDetector.startMonitoring()
    }

    val ongoingSession      by TripDetectorState.ongoingSession.collectAsState()
    val pendingSession      by TripDetectorState.pendingSession.collectAsState()
    val recentlySaved       by TripDetectorState.recentlySaved.collectAsState()
    val locationPermState   by TripDetectorState.permissionState.collectAsState()
    val notifGranted        by TripDetectorState.notificationsGranted.collectAsState()

    val locationGranted = locationPermState == LocationPermissionState.GRANTED ||
                          locationPermState == LocationPermissionState.BACKGROUND_ONLY
    val permReq = LocalPermissionRequester.current

    // ── Auth user ─────────────────────────────────────────────────────────────
    val authUser by AuthState.currentUser.collectAsState()

    // Derived from authUser — instant from cached token, refreshed after GET /users/me.
    // No separate mutableStateOf needed: updating AuthState re-emits currentUser and
    // triggers recomposition, so homeUser and profileScreen both update automatically.
    val homeUser = authUser?.let { u ->
        UserProfile(
            displayName = u.displayName,
            initials    = u.displayName.takeIf { it.isNotBlank() }?.toInitials()
                ?: u.email.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
        )
    } ?: UserProfile(displayName = "", initials = "?")

    // ── Confirmed sessions from DB ────────────────────────────────────────────
    // TripDetector.init() guarantees DatabaseProvider is ready before we reach here.
    val repo = remember { TripRepositoryImpl(DatabaseProvider.database) }
    val confirmedSessions by repo.observeConfirmedSessions().collectAsState(initial = emptyList())
    // Backend activities fetched from GET /api/v1/activities — populated by the timeline LaunchedEffect.
    var backendActivities by remember { mutableStateOf(emptyList<RecentActivityEntry>()) }
    // Merge local DB sessions with backend-only trips (trips from other devices / reinstalls).
    // Dedup: exclude backend entries whose id already appears as a backend_activity_id in local DB.
    val groupedActivities = remember(confirmedSessions, backendActivities) {
        val syncedIds = confirmedSessions.mapNotNull { it.session.backend_activity_id }.toSet()
        val backendOnly = backendActivities.filter { it.sessionId !in syncedIds }
        (confirmedSessions.map { it.toRecentActivityEntry() } + backendOnly)
            .sortedByDescending { it.timestampMs }
            .groupByDateLabel()
    }
    // Most-recent 3 sessions for the HomeScreen "Recent Activity" card — local DB only
    // so just-confirmed trips appear immediately without waiting for the next backend fetch.
    val recentActivityEntries = remember(confirmedSessions) {
        confirmedSessions.take(3).map { it.toRecentActivityEntry() }
    }
    // Profile impact stats — CO₂ from backend weekly data (region-accurate DEFRA factors),
    // days tracked from local DB confirmed sessions.
    val profileDaysTracked = groupedActivities.size

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

    // ── Persisted settings ────────────────────────────────────────────────────
    val settings = remember { Settings() }
    var dailyGoalKgCO2 by remember { mutableStateOf(settings.getFloat("daily_goal_kg", 5.0f)) }

    // ── Timeline data (real CO₂ totals from backend) ──────────────────────────
    // Initialised to neutral zeros — the Home screen skeleton renders while the first fetch runs.
    // Using preview/fake values here would silently display fabricated data on network failure.
    var todayImpact        by remember { mutableStateOf(TodayImpact(kgCO2 = 0f, dailyGoalKgCO2 = dailyGoalKgCO2, percentVsWeeklyAvg = 0)) }
    var weeklyTrend        by remember { mutableStateOf(emptyList<WeeklyDataPoint>()) }
    var insights           by remember { mutableStateOf(emptyList<InsightEntry>()) }
    var transportBreakdown by remember { mutableStateOf(emptyList<TransportModeEntry>()) }
    val unreadInsightsCount = remember(insights) { insights.count { !it.isRead } }
    val handleInsightClick: (InsightEntry) -> Unit = { entry ->
        if (!entry.isRead && entry.id.isNotBlank()) {
            insights = insights.map { if (it.id == entry.id) it.copy(isRead = true) else it }
            scope.launch { insightsService.markRead(entry.id) }
        }
        selectedInsight = entry.copy(isRead = true)
        screen = Screen.InsightDetail
    }

    // Fetch user profile once per sign-in. Keyed on authUser so it does NOT re-fire on every
    // trip save (timelineTrigger). On success, updates AuthState so homeUser and ProfileScreen
    // both recompose from the authoritative server display name.
    LaunchedEffect(authUser) {
        if (authUser == null) {
            backendActivities = emptyList()
            return@LaunchedEffect
        }
        if (!tokenStore.isLoggedIn) return@LaunchedEffect
        userService.getMe().onSuccess { dto ->
            AuthState.onSignedIn(AuthUser(
                id          = dto.id,
                email       = dto.email,
                displayName = dto.displayName,
                avatarUrl   = dto.avatarUrl ?: "",
            ))
        }
    }

    // Fetch timeline whenever the user is authenticated and lands on Home.
    // Re-fetches automatically after any trip is saved (timelineTrigger counter below).
    // try/finally guarantees homeIsLoading = false even on cancellation (LaunchedEffect restart)
    // or if the early-return path fires (unauthenticated mid-session).
    var timelineTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(screen, timelineTrigger) {
        if (screen != Screen.Home) return@LaunchedEffect
        if (!tokenStore.isLoggedIn) {
            homeIsLoading = false
            return@LaunchedEffect
        }
        try {
            coroutineScope {
                val dailyDeferred      = async { timelineService.getDaily() }
                val weeklyDeferred     = async { timelineService.getWeekly() }
                val insightDeferred    = async { insightsService.getInsights() }
                val activitiesDeferred = async { activityService.listActivities() }

                dailyDeferred.await().onSuccess { daily ->
                    todayImpact = TodayImpact(
                        kgCO2              = daily.totalKgCo2e,
                        dailyGoalKgCO2     = dailyGoalKgCO2,
                        percentVsWeeklyAvg = daily.trend.changePct?.toInt() ?: 0,
                    )
                    val totalDistKm = daily.breakdown.values.sumOf { it.distanceKm.toDouble() }.toFloat()
                    transportBreakdown = daily.breakdown.entries
                        .mapNotNull { (key, dto) ->
                            val mode = TransportModeType.entries.firstOrNull {
                                it.name.equals(key, ignoreCase = true)
                            } ?: return@mapNotNull null
                            TransportModeEntry(
                                mode        = mode,
                                displayName = when (mode) {
                                    TransportModeType.DRIVING        -> "Driving"
                                    TransportModeType.CAB            -> "Cab"
                                    TransportModeType.PUBLIC_TRANSIT -> "Public Transit"
                                    TransportModeType.BUS            -> "Bus"
                                    TransportModeType.TRAIN          -> "Train"
                                    TransportModeType.METRO          -> "Metro"
                                    TransportModeType.CYCLING        -> "Cycling"
                                    TransportModeType.WALKING        -> "Walking"
                                    TransportModeType.TWO_WHEELER    -> "Two-Wheeler"
                                    TransportModeType.AUTO_RICKSHAW  -> "Auto"
                                    TransportModeType.FLIGHT         -> "Flight"
                                },
                                distanceKm  = dto.distanceKm,
                                kgCO2       = dto.kgCo2e,
                                percentage  = if (totalDistKm > 0f) (dto.distanceKm / totalDistKm * 100).roundToInt() else 0,
                            )
                        }
                        .sortedByDescending { it.distanceKm }
                }
                weeklyDeferred.await().onSuccess { weekly ->
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
                insightDeferred.await().onSuccess { response ->
                    insights = response.items.map { it.toInsightEntry() }
                }
                activitiesDeferred.await().onSuccess { page ->
                    backendActivities = page.activities
                        .map { it.toRecentActivityEntry() }
                        .filter { it.timestampMs > 0L }
                }
            }
        } finally {
            homeIsLoading = false
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
                    onSignIn            = { email, password -> handleEmailSignIn(email, password) },
                    onNavigateToSignUp  = { screen = Screen.SignUp },
                    onForgotPassword    = { forgotSucceeded = false; forgotError = null; screen = Screen.ForgotPassword },
                    onGoogleSignIn      = { handleGoogleSignIn() },
                    googleSignInLoading = googleSignInLoading,
                    googleSignInError   = googleSignInError,
                    emailSignInLoading  = emailSignInLoading,
                    emailSignInError    = emailSignInError,
                )

                Screen.SignUp -> SignUpScreen(
                    onCreateAccount     = { name, email, password -> handleEmailSignUp(name, email, password) },
                    onNavigateToSignIn  = { screen = Screen.Login },
                    onGoogleSignIn      = { handleGoogleSignIn() },
                    googleSignInLoading = googleSignInLoading,
                    googleSignInError   = googleSignInError,
                    emailSignUpLoading  = emailSignUpLoading,
                    emailSignUpError    = emailSignUpError,
                )

                Screen.ForgotPassword -> ForgotPasswordScreen(
                    onBack          = { screen = Screen.Login },
                    onBackToSignIn  = { screen = Screen.Login },
                    onSendResetLink = { email -> handleForgotPassword(email) },
                    sendLoading     = forgotLoading,
                    sendError       = forgotError,
                    showSuccess     = forgotSucceeded,
                )

                Screen.Home -> HomeScreen(
                    state = previewHomeUiState.copy(
                        greeting            = currentGreeting(),
                        dateLabel           = currentDateLabel(),
                        user                = homeUser,
                        isLoading           = homeIsLoading,
                        ongoingSession      = ongoingSession,
                        pendingSession      = pendingSession,
                        recentActivity      = recentActivityEntries,
                        todayImpact         = todayImpact,
                        weeklyTrend         = weeklyTrend,
                        transportBreakdown  = transportBreakdown,
                        insights            = insights,
                        unreadInsightsCount = unreadInsightsCount,
                    ),
                    onNavigateToProfile    = { screen = Screen.Profile },
                    onNavigateToActivities = { screen = Screen.Activities },
                    onNavigateToInsights   = { screen = Screen.Insights },
                    onRetry                = { homeIsLoading = true; timelineTrigger++ },
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
                    onInsightClick         = handleInsightClick,
                    onStopAndSave          = { tripDetector.manualEndAndSave() },
                    onDiscard              = { tripDetector.discardSession() },
                    onResume               = { tripDetector.resumeLeg() },
                )

                Screen.Profile -> ProfileScreen(
                    state = previewProfileUiState.copy(
                        displayName     = authUser?.displayName ?: "",
                        initials        = authUser?.let { user ->
                            user.displayName.takeIf { it.isNotBlank() }?.toInitials()
                                ?: user.email.firstOrNull()?.uppercaseChar()?.toString()
                                ?: "?"
                        } ?: "?",
                        email           = authUser?.email ?: "",
                        totalCO2SavedKg = weeklyTrend.sumOf { it.kgCO2.toDouble() }.toFloat(),
                        daysTracked     = profileDaysTracked,
                        todayKgCO2      = todayImpact.kgCO2,
                        dailyGoalKgCO2  = dailyGoalKgCO2,
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
                    onGoalChange           = { goal ->
                        dailyGoalKgCO2 = goal
                        settings.putFloat("daily_goal_kg", goal)
                        todayImpact = todayImpact.copy(dailyGoalKgCO2 = goal)
                    },
                    onSignOut    = { handleSignOut() },
                    onDeleteAccount = { handleSignOut() },
                    onFabClick   = { showLogActivity = true },
                )

                Screen.TripDetail -> selectedTrip?.let { entry ->
                    TripDetailScreen(
                        entry          = entry,
                        dailyGoalKgCO2 = dailyGoalKgCO2,
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
                    entries        = insights,
                    onBack         = { screen = Screen.Home },
                    onInsightClick = { entry ->
                        if (!entry.isRead && entry.id.isNotBlank()) {
                            insights = insights.map { if (it.id == entry.id) it.copy(isRead = true) else it }
                            scope.launch { insightsService.markRead(entry.id) }
                        }
                        selectedInsight = entry.copy(isRead = true)
                        screen = Screen.InsightDetail
                    },
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
