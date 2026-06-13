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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
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
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dev.atmos.shared.location.LocalPermissionRequester
import dev.atmos.shared.ui.common.LocalAvatarUploader
import dev.atmos.shared.location.LocationPermissionState
import dev.atmos.shared.location.TripDetectorState
import dev.atmos.shared.location.createTripDetector
import dev.atmos.shared.network.ActivityService
import dev.atmos.shared.network.AuthResponseDto
import dev.atmos.shared.network.AuthService
import dev.atmos.shared.network.DeviceService
import dev.atmos.shared.network.FcmTokenStore
import dev.atmos.shared.network.InsightsService
import dev.atmos.shared.network.TimelineService
import dev.atmos.shared.network.UserService
import dev.atmos.shared.network.backendMode
import dev.atmos.shared.network.toInsightEntry
import dev.atmos.shared.network.toRecentActivityEntry
import dev.atmos.shared.ui.NotificationState
import dev.atmos.shared.ui.home.TodayImpact
import dev.atmos.shared.ui.home.TransportModeEntry
import dev.atmos.shared.ui.home.UserProfile
import dev.atmos.shared.ui.home.WeeklyDataPoint
import dev.atmos.shared.platformId
import kotlin.math.roundToInt
import kotlin.random.Random
import dev.atmos.shared.ui.activities.ActivitiesScreen
import dev.atmos.shared.ui.auth.EmailVerificationScreen
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
import dev.atmos.shared.ui.profile.CommuteLocation
import dev.atmos.shared.ui.profile.ProfileScreen
import dev.atmos.shared.ui.profile.previewProfileUiState
import dev.atmos.shared.ui.profile.toInitials
import dev.atmos.shared.util.toDisplayString
import dev.atmos.shared.ui.stats.StatsPeriod
import dev.atmos.shared.ui.stats.StatsScreen
import dev.atmos.shared.ui.stats.StatsSummary
import dev.atmos.shared.ui.stats.StatsBarPoint
import dev.atmos.shared.ui.stats.StatsUiState
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

private fun TransportModeType.toDisplayName(): String = when (this) {
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
}

private fun String.toDistanceUnit(): String = when (this) {
    "Metric (km)"   -> "km"
    "Imperial (mi)" -> "miles"
    else            -> "km"
}

private fun String.toUnitsLabel(): String = when (this) {
    "km"    -> "Metric (km)"
    "miles" -> "Imperial (mi)"
    else    -> "Metric (km)"
}

// Generates a UUID v4-shaped string for stable device identification.
// Persisted in Settings["device_stable_id"] on first run and reused thereafter.
private fun generateDeviceStableId(): String = buildString {
    val rng = Random.Default
    repeat(8)  { append(rng.nextInt(16).toString(16)) }; append('-')
    repeat(4)  { append(rng.nextInt(16).toString(16)) }; append('-')
    append('4'); repeat(3) { append(rng.nextInt(16).toString(16)) }; append('-')
    append((8 + rng.nextInt(4)).toString(16)); repeat(3) { append(rng.nextInt(16).toString(16)) }; append('-')
    repeat(12) { append(rng.nextInt(16).toString(16)) }
}

private sealed class Screen {
    data object Onboarding         : Screen()
    data object Login              : Screen()
    data object SignUp             : Screen()
    data object ForgotPassword     : Screen()
    /** Carries the email so the screen never needs a separate pendingVerificationEmail var. */
    data class  EmailVerification(val email: String) : Screen()
    data object Home               : Screen()
    data object Activities         : Screen()
    data object Stats              : Screen()
    data object TripDetail         : Screen()
    data object InsightDetail      : Screen()
    data object Profile            : Screen()
    data object Insights           : Screen()
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
    val deviceService    = remember { DeviceService() }
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
    // Holds the signed-in user after signup until the user continues past the verification screen.
    // Deferring AuthState.onSignedIn() prevents the LaunchedEffect(authUser?.id) from firing
    // GET /users/me while the user is still on the unverified-email prompt.
    var pendingVerificationAuthUser by remember { mutableStateOf<AuthUser?>(null) }

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
                    // Save tokens so the resend call has a valid Bearer token.
                    // AuthState.onSignedIn is intentionally deferred to onContinue so that
                    // LaunchedEffect(authUser?.id) does not fire GET /users/me while the user
                    // is on the verification screen with an unverified account.
                    tokenStore.save(response)
                    pendingVerificationAuthUser = AuthUser(
                        id          = response.user.id,
                        email       = response.user.email,
                        displayName = response.user.displayName,
                        avatarUrl   = response.user.avatarUrl ?: "",
                    )
                    screen = Screen.EmailVerification(email = response.user.email)
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
        pendingVerificationAuthUser = null
        NotificationState.pendingInsightId.value = null
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

    // Configure Coil once with a Ktor-backed network fetcher so AsyncImage can load
    // avatar URLs on both Android (OkHttp engine) and iOS (Darwin engine).
    LaunchedEffect(Unit) {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }
    }

    val avatarUploader = LocalAvatarUploader.current

    // Navigate to Onboarding when the token refresher forces a sign-out
    // (expired / revoked refresh token). Tokens are already cleared by TokenRefresher.
    LaunchedEffect(Unit) {
        AuthState.forceSignOut.collect { handleSignOut() }
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
            avatarUrl   = u.avatarUrl,
        )
    } ?: UserProfile(displayName = "", initials = "?")

    // ── Confirmed sessions from DB ────────────────────────────────────────────
    // TripDetector.init() guarantees DatabaseProvider is ready before we reach here.
    val repo = remember { TripRepositoryImpl(DatabaseProvider.database) }
    val confirmedSessions by repo.observeConfirmedSessions().collectAsState(initial = emptyList())
    // Backend activities fetched from GET /api/v1/activities — populated by the timeline LaunchedEffect.
    var backendActivities by remember { mutableStateOf(emptyList<RecentActivityEntry>()) }
    // IDs of backend-only trips the user has deleted this session. Persists across listActivities
    // re-fetches so a deleted entry doesn't reappear if the backend read lags behind the delete.
    var deletedBackendIds by remember { mutableStateOf(emptySet<String>()) }
    // Merge local DB sessions with backend-only trips (trips from other devices / reinstalls).
    // Dedup: exclude backend entries whose id already appears as a backend_activity_id in local DB,
    // and exclude entries the user has explicitly deleted in this session.
    val groupedActivities = remember(confirmedSessions, backendActivities, deletedBackendIds) {
        val syncedIds = confirmedSessions.mapNotNull { it.session.backend_activity_id }.toSet()
        val backendOnly = backendActivities.filter { it.sessionId !in syncedIds && it.sessionId !in deletedBackendIds }
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
    // appearanceMode and notificationsEnabled are initialised below after settings is created.
    var isDeletingAccount    by remember { mutableStateOf(false) }
    var notificationsJob:  Job? by remember { mutableStateOf(null) }
    var weeklyReportJob:   Job? by remember { mutableStateOf(null) }
    var dataSharingJob:    Job? by remember { mutableStateOf(null) }
    var homeAddressJob:    Job? by remember { mutableStateOf(null) }
    var workAddressJob:    Job? by remember { mutableStateOf(null) }
    var transportJob:      Job? by remember { mutableStateOf(null) }
    var unitsJob:          Job? by remember { mutableStateOf(null) }
    var showLogActivity by remember { mutableStateOf(false) }
    var tripToEdit       by remember { mutableStateOf<PendingTripEntry?>(null) }
    var selectedTrip     by remember { mutableStateOf<RecentActivityEntry?>(null) }
    var selectedInsight  by remember { mutableStateOf<InsightEntry?>(null) }
    var homeIsLoading    by remember { mutableStateOf(true) }
    var homeIsError      by remember { mutableStateOf(false) }
    // Non-null when the user tapped Edit on a confirmed DB trip — holds the session to replace.
    var editingSessionId  by remember { mutableStateOf<String?>(null) }
    var editingTimestampMs by remember { mutableStateOf(0L) }

    // ── Persisted settings ────────────────────────────────────────────────────
    val settings = remember { Settings() }
    var dailyGoalKgCO2   by remember { mutableStateOf(settings.getFloat("daily_goal_kg", 5.0f)) }
    var commuteHome      by remember { mutableStateOf(settings.getString("commute_home", "")) }
    var commuteWork      by remember { mutableStateOf(settings.getString("commute_work", "")) }
    // "Bus" is the label that profileTransportOptions assigns to PUBLIC_TRANSIT — must match exactly.
    var defaultTransport by remember { mutableStateOf(settings.getString("default_transport", "Bus")) }
    var unitsLabel       by remember { mutableStateOf(settings.getString("units_label", "Metric (km)")) }
    var appearanceMode   by remember {
        mutableStateOf(
            settings.getStringOrNull("appearance_mode")
                ?.let { name -> AppearanceMode.entries.firstOrNull { it.name == name } }
                ?: AppearanceMode.SYSTEM
        )
    }
    var notificationsEnabled by remember { mutableStateOf(settings.getBoolean("notifications_enabled", true)) }
    var weeklyReportEnabled  by remember { mutableStateOf(settings.getBoolean("weekly_report", true)) }
    var dataSharingEnabled   by remember { mutableStateOf(settings.getBoolean("data_sharing", false)) }

    // FCM push token — updated reactively by FcmTokenStore (seeded by MainActivity on cold start,
    // updated by AtmosFirebaseMessagingService on token rotation). Null on iOS (FCM is Android-only).
    val fcmToken by FcmTokenStore.token.collectAsState()

    // ── Stats screen state ────────────────────────────────────────────────────
    var statsPeriod        by remember { mutableStateOf(StatsPeriod.WEEK) }
    var statsPeriodOffset  by remember { mutableStateOf(0) }  // 0=current, -1=prev, etc.
    var statsState         by remember { mutableStateOf(StatsUiState()) }
    var statsRetryTrigger  by remember { mutableStateOf(0) }

    // ── Timeline data (real CO₂ totals from backend) ──────────────────────────
    // Initialised to neutral zeros — the Home screen skeleton renders while the first fetch runs.
    // Using preview/fake values here would silently display fabricated data on network failure.
    var todayImpact        by remember { mutableStateOf(TodayImpact(kgCO2 = 0f, dailyGoalKgCO2 = dailyGoalKgCO2, percentVsWeeklyAvg = 0)) }
    var weeklyTrend        by remember { mutableStateOf(emptyList<WeeklyDataPoint>()) }
    var weeklyTotalKgCo2   by remember { mutableStateOf(0f) }
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

    // Navigate to InsightDetail when the user taps a push notification.
    // Waits for the insight to appear in the loaded list — if it's not there yet,
    // navigating to Home triggers the timeline fetch which populates insights,
    // and this effect re-runs when insights changes.
    val pendingInsightId by NotificationState.pendingInsightId.collectAsState()
    LaunchedEffect(pendingInsightId, insights, authUser?.id) {
        val id = pendingInsightId ?: return@LaunchedEffect
        if (authUser == null) return@LaunchedEffect  // wait until signed in
        // Ensure Home is the active screen so the timeline fetch fires
        if (screen != Screen.Home && screen != Screen.InsightDetail) screen = Screen.Home
        val insight = insights.firstOrNull { it.id == id } ?: return@LaunchedEffect
        NotificationState.pendingInsightId.value = null
        insights = insights.map { if (it.id == id) it.copy(isRead = true) else it }
        selectedInsight = insight.copy(isRead = true)
        screen = Screen.InsightDetail
        scope.launch { insightsService.markRead(id) }
    }

    // Fetch user profile once per sign-in. Keyed on authUser?.id so it fires on sign-in/sign-out
    // but NOT when updateMe() changes displayName (which would loop: updateMe → authUser change
    // → getMe → authUser change → ...).
    LaunchedEffect(authUser?.id) {
        if (authUser == null) {
            backendActivities = emptyList()
            return@LaunchedEffect
        }
        if (!tokenStore.isLoggedIn) return@LaunchedEffect
        supervisorScope {
            val meDeferred    = async { userService.getMe() }
            val prefsDeferred = async { userService.getPreferences() }
            meDeferred.await().onSuccess { dto ->
                AuthState.onSignedIn(AuthUser(
                    id          = dto.id,
                    email       = dto.email,
                    displayName = dto.displayName,
                    avatarUrl   = dto.avatarUrl ?: "",
                ))
            }
            prefsDeferred.await().onSuccess { prefs ->
                if (prefs.dailyGoalKgCo2e != null) {
                    val goal = prefs.dailyGoalKgCo2e.toFloat()
                    dailyGoalKgCO2 = goal
                    settings.putFloat("daily_goal_kg", goal)
                    todayImpact = todayImpact.copy(dailyGoalKgCO2 = goal)
                }
                if (prefs.pushNotificationsEnabled != null) {
                    notificationsEnabled = prefs.pushNotificationsEnabled
                    settings.putBoolean("notifications_enabled", prefs.pushNotificationsEnabled)
                }
                if (prefs.weeklyReportEnabled != null) {
                    weeklyReportEnabled = prefs.weeklyReportEnabled
                    settings.putBoolean("weekly_report", prefs.weeklyReportEnabled)
                }
                if (prefs.dataSharingEnabled != null) {
                    dataSharingEnabled = prefs.dataSharingEnabled
                    settings.putBoolean("data_sharing", prefs.dataSharingEnabled)
                }
                // distance_unit is always non-null on backend (defaults to "km") — always authoritative.
                val backendUnitsLabel = prefs.distanceUnit?.toUnitsLabel() ?: "Metric (km)"
                unitsLabel = backendUnitsLabel
                settings.putString("units_label", backendUnitsLabel)
                // Commute fields are nullable — only overwrite local if backend has a value.
                if (prefs.homeAddress != null) {
                    commuteHome = prefs.homeAddress
                    settings.putString("commute_home", prefs.homeAddress)
                }
                if (prefs.workAddress != null) {
                    commuteWork = prefs.workAddress
                    settings.putString("commute_work", prefs.workAddress)
                }
                if (prefs.defaultTransport != null) {
                    defaultTransport = prefs.defaultTransport
                    settings.putString("default_transport", prefs.defaultTransport)
                }
                // Single batched PUT for any fields not yet stored on the backend.
                // Uses launch (not scope.launch) so it is cancelled if the user signs out
                // before the request completes, preventing a write against a stale token.
                val needsBootstrap = prefs.dailyGoalKgCo2e == null ||
                    prefs.pushNotificationsEnabled == null ||
                    prefs.weeklyReportEnabled == null ||
                    prefs.dataSharingEnabled == null ||
                    (prefs.homeAddress == null && commuteHome.isNotBlank()) ||
                    (prefs.workAddress == null && commuteWork.isNotBlank()) ||
                    prefs.defaultTransport == null
                if (needsBootstrap) {
                    launch {
                        userService.updatePreferences(
                            dailyGoalKgCO2e          = if (prefs.dailyGoalKgCo2e == null) dailyGoalKgCO2.toDouble() else null,
                            pushNotificationsEnabled = if (prefs.pushNotificationsEnabled == null) notificationsEnabled else null,
                            weeklyReportEnabled      = if (prefs.weeklyReportEnabled == null) weeklyReportEnabled else null,
                            dataSharingEnabled       = if (prefs.dataSharingEnabled == null) dataSharingEnabled else null,
                            homeAddress              = if (prefs.homeAddress == null && commuteHome.isNotBlank()) commuteHome else null,
                            workAddress              = if (prefs.workAddress == null && commuteWork.isNotBlank()) commuteWork else null,
                            defaultTransport         = if (prefs.defaultTransport == null) defaultTransport else null,
                        )
                    }
                }
            }

        }
    }

    // Register (or re-register) this device with the backend whenever the user or push token changes.
    // Keyed on both authUser?.id and fcmToken so it fires on login AND on FCM token rotation.
    // iOS: fcmToken is always null (Firebase is Android-only) so this block is a no-op there.
    LaunchedEffect(authUser?.id, fcmToken) {
        if (authUser == null) return@LaunchedEffect
        if (!tokenStore.isLoggedIn) return@LaunchedEffect
        val token = fcmToken ?: return@LaunchedEffect

        val stableId = settings.getStringOrNull("device_stable_id") ?: run {
            val newId = generateDeviceStableId()
            settings.putString("device_stable_id", newId)
            newId
        }
        deviceService.registerDevice(
            deviceToken  = stableId,
            pushToken    = token,
            platform     = platformId,
            pushProvider = "fcm",
        ).onSuccess { dto ->
            if (dto.id.isNotBlank()) settings.putString("device_id", dto.id)
        }
    }

    // Fetch timeline whenever the user is authenticated and lands on Home.
    // Re-fetches automatically after any trip is saved (timelineTrigger counter below).
    // try/finally guarantees homeIsLoading = false even on cancellation (LaunchedEffect restart)
    // or if the early-return path fires (unauthenticated mid-session).
    var timelineTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(screen, timelineTrigger) {
        if (screen != Screen.Home) return@LaunchedEffect
        homeIsError = false
        homeIsLoading = true
        if (!tokenStore.isLoggedIn) {
            homeIsLoading = false
            return@LaunchedEffect
        }
        try {
            supervisorScope {
                val dailyDeferred      = async { timelineService.getDaily() }
                val weeklyDeferred     = async { timelineService.getWeekly() }
                val insightDeferred    = async { insightsService.getInsights() }
                val activitiesDeferred = async { activityService.listAllActivities() }

                val dailyResult  = dailyDeferred.await()
                val weeklyResult = weeklyDeferred.await()

                // Either primary call failed — cancel secondary fetches and show the error card.
                // Cancelling explicitly lets supervisorScope complete promptly rather than
                // waiting on in-flight insight/activity requests whose results we'd discard.
                if (dailyResult.isFailure || weeklyResult.isFailure) {
                    insightDeferred.cancel()
                    activitiesDeferred.cancel()
                    homeIsError = true
                    return@supervisorScope
                }

                dailyResult.onSuccess { daily ->
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
                                displayName = mode.toDisplayName(),
                                distanceKm  = dto.distanceKm,
                                kgCO2       = dto.kgCo2e,
                                percentage  = if (totalDistKm > 0f) (dto.distanceKm / totalDistKm * 100).roundToInt() else 0,
                            )
                        }
                        .sortedByDescending { it.distanceKm }
                }
                weeklyResult.onSuccess { weekly ->
                    if (weekly.days.isNotEmpty()) {
                        // Keep weeklyTotalKgCo2 in sync with weeklyTrend so the Profile
                        // card and the Home bar chart always reflect the same response.
                        weeklyTotalKgCo2 = weekly.totalKgCo2e
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
                activitiesDeferred.await().onSuccess { activities ->
                    backendActivities = activities
                        .map { it.toRecentActivityEntry() }
                        .filter { it.timestampMs > 0L }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            homeIsError = true
        } finally {
            homeIsLoading = false
        }
    }

    // Fetch stats data whenever the user navigates to Screen.Stats, changes period/offset, or retries.
    LaunchedEffect(screen, statsPeriod, statsPeriodOffset, statsRetryTrigger) {
        if (screen != Screen.Stats) return@LaunchedEffect
        if (!tokenStore.isLoggedIn) return@LaunchedEffect

        statsState = statsState.copy(isLoading = true, isError = false)

        val tz    = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date

        try {
            when (statsPeriod) {
                StatsPeriod.DAY -> {
                    val target = today.plus(statsPeriodOffset, DateTimeUnit.DAY)
                    val dateStr = "${target.year}-${target.monthNumber.toString().padStart(2,'0')}-${target.dayOfMonth.toString().padStart(2,'0')}"
                    val periodLabel = when (statsPeriodOffset) {
                        0    -> "Today"
                        -1   -> "Yesterday"
                        else -> "${target.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} ${target.dayOfMonth}"
                    }
                    timelineService.getDaily(date = dateStr).onSuccess { daily ->
                        val totalDist = daily.breakdown.values.sumOf { it.distanceKm.toDouble() }.toFloat()
                        statsState = StatsUiState(
                            period      = StatsPeriod.DAY,
                            periodLabel = periodLabel,
                            summary = StatsSummary(
                                totalKgCo2     = daily.totalKgCo2e,
                                totalDistKm    = totalDist,
                                activityCount  = daily.activityCount,
                                breakdown      = daily.breakdown.entries
                                    .mapNotNull { (key, dto) ->
                                        val mode = TransportModeType.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: return@mapNotNull null
                                        TransportModeEntry(mode, mode.toDisplayName(), dto.distanceKm, dto.kgCo2e,
                                            if (totalDist > 0f) (dto.distanceKm / totalDist * 100).roundToInt() else 0)
                                    }
                                    .sortedByDescending { it.distanceKm },
                                trendDirection = daily.trend.direction,
                                trendPct       = daily.trend.changePct,
                                prevTotalKgCo2 = daily.trend.prevTotalKgCo2e,
                            ),
                            barPoints   = emptyList(),
                            canGoPrev   = true,
                            isLoading   = false,
                        )
                    }.onFailure {
                        statsState = statsState.copy(isLoading = false, isError = true)
                    }
                }

                StatsPeriod.WEEK -> {
                    val dayOfWeek   = today.dayOfWeek.ordinal  // Mon=0 … Sun=6
                    val currentMonday = today.plus(-dayOfWeek, DateTimeUnit.DAY)
                    val weekStart   = currentMonday.plus(statsPeriodOffset * 7, DateTimeUnit.DAY)
                    val weekEnd     = weekStart.plus(6, DateTimeUnit.DAY)
                    fun LocalDate.toApiString() = "${year}-${monthNumber.toString().padStart(2,'0')}-${dayOfMonth.toString().padStart(2,'0')}"
                    val weekStartStr = weekStart.toApiString()
                    val weekEndStr   = weekEnd.toApiString()
                    val periodLabel  = when (statsPeriodOffset) {
                        0  -> "This week"
                        -1 -> "Last week"
                        else -> "${weekStart.month.name.lowercase().replaceFirstChar{it.uppercase()}.take(3)} ${weekStart.dayOfMonth} – ${weekEnd.month.name.lowercase().replaceFirstChar{it.uppercase()}.take(3)} ${weekEnd.dayOfMonth}"
                    }

                    coroutineScope {
                        val weeklyDeferred = async { timelineService.getWeekly(weekStart = weekStartStr) }
                        val rangeDeferred  = async { timelineService.getRange(from = weekStartStr, to = weekEndStr) }

                        val weeklyResult = weeklyDeferred.await()
                        val rangeResult  = rangeDeferred.await()

                        weeklyResult.onSuccess { weekly ->
                            val totalDist = weekly.breakdown.values.sumOf { it.distanceKm.toDouble() }.toFloat()
                            // Build per-day bars from range response (keyed by date_local).
                            // If the range call failed, omit bar points entirely so the chart
                            // doesn't render flat-zero bars contradicting the weekly total.
                            val dayNames = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
                            val barPoints = if (rangeResult.isSuccess) {
                                val rangeByDate = rangeResult.getOrThrow().associateBy { it.dateLocal }
                                (0..6).map { i ->
                                    val date = weekStart.plus(i, DateTimeUnit.DAY)
                                    StatsBarPoint(
                                        label     = dayNames[i],
                                        kgCo2     = rangeByDate[date.toApiString()]?.totalKgCo2e ?: 0f,
                                        isCurrent = date == today,
                                    )
                                }
                            } else {
                                emptyList()
                            }
                            statsState = StatsUiState(
                                period      = StatsPeriod.WEEK,
                                periodLabel = periodLabel,
                                summary = StatsSummary(
                                    totalKgCo2     = weekly.totalKgCo2e,
                                    totalDistKm    = totalDist,
                                    activityCount  = weekly.activityCount,
                                    breakdown      = weekly.breakdown.entries
                                        .mapNotNull { (key, dto) ->
                                            val mode = TransportModeType.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: return@mapNotNull null
                                            TransportModeEntry(mode, mode.toDisplayName(), dto.distanceKm, dto.kgCo2e,
                                                if (totalDist > 0f) (dto.distanceKm / totalDist * 100).roundToInt() else 0)
                                        }
                                        .sortedByDescending { it.distanceKm },
                                    trendDirection = weekly.trend.direction,
                                    trendPct       = weekly.trend.changePct,
                                    prevTotalKgCo2 = weekly.trend.prevTotalKgCo2e,
                                ),
                                barPoints   = barPoints,
                                canGoPrev   = true,
                                isLoading   = false,
                            )
                        }.onFailure {
                            statsState = statsState.copy(isLoading = false, isError = true)
                        }
                    }
                }

                StatsPeriod.MONTH -> {
                    var targetYear  = today.year
                    var targetMonth = today.monthNumber + statsPeriodOffset
                    while (targetMonth <= 0)  { targetYear--; targetMonth += 12 }
                    while (targetMonth > 12)  { targetYear++; targetMonth -= 12 }
                    val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                    val periodLabel = when (statsPeriodOffset) {
                        0    -> "This month"
                        -1   -> "Last month"
                        else -> "${monthNames[targetMonth - 1]} $targetYear"
                    }

                    timelineService.getMonthly(year = targetYear, month = targetMonth).onSuccess { monthly ->
                        val totalDist = monthly.breakdown.values.sumOf { it.distanceKm.toDouble() }.toFloat()
                        statsState = StatsUiState(
                            period      = StatsPeriod.MONTH,
                            periodLabel = periodLabel,
                            summary = StatsSummary(
                                totalKgCo2     = monthly.totalKgCo2e,
                                totalDistKm    = totalDist,
                                activityCount  = monthly.activityCount,
                                breakdown      = monthly.breakdown.entries
                                    .mapNotNull { (key, dto) ->
                                        val mode = TransportModeType.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: return@mapNotNull null
                                        TransportModeEntry(mode, mode.toDisplayName(), dto.distanceKm, dto.kgCo2e,
                                            if (totalDist > 0f) (dto.distanceKm / totalDist * 100).roundToInt() else 0)
                                    }
                                    .sortedByDescending { it.distanceKm },
                                trendDirection = monthly.trend.direction,
                                trendPct       = monthly.trend.changePct,
                                prevTotalKgCo2 = monthly.trend.prevTotalKgCo2e,
                            ),
                            barPoints   = emptyList(),
                            canGoPrev   = true,
                            isLoading   = false,
                        )
                    }.onFailure {
                        statsState = statsState.copy(isLoading = false, isError = true)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            statsState = statsState.copy(isLoading = false, isError = true)
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

    fun deleteTrip(entry: RecentActivityEntry, afterDelete: suspend () -> Unit) {
        val sessionsAtDeleteTime = confirmedSessions
        scope.launch {
            try {
                val localSession = sessionsAtDeleteTime
                    .firstOrNull { it.session.id == entry.sessionId }
                if (localSession != null) {
                    val backendId = localSession.session.backend_activity_id
                    if (!backendId.isNullOrEmpty()) {
                        activityService.deleteActivity(backendId).getOrThrow()
                    }
                    repo.deleteSession(localSession.session.id)
                } else if (entry.sessionId.isNotEmpty()) {
                    activityService.deleteActivity(entry.sessionId).getOrThrow()
                    deletedBackendIds = deletedBackendIds + entry.sessionId
                } else {
                    snackbarHostState.showSnackbar("Could not identify trip — please try again")
                    return@launch
                }
                afterDelete()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Could not delete trip — please try again")
            }
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

                is Screen.EmailVerification -> EmailVerificationScreen(
                    email     = (screen as Screen.EmailVerification).email,
                    onResend  = { onSuccess, onError ->
                        val token = tokenStore.getAccessToken()
                        if (token == null) { onError("Not authenticated"); return@EmailVerificationScreen }
                        scope.launch {
                            authService.resendVerification(token)
                                .onSuccess { onSuccess() }
                                .onFailure { e -> onError(e.message ?: "Could not send verification email.") }
                        }
                    },
                    onContinue = {
                        // Complete the deferred sign-in now that the user is proceeding to Home.
                        pendingVerificationAuthUser?.let { AuthState.onSignedIn(it) }
                        pendingVerificationAuthUser = null
                        screen = Screen.Home
                    },
                )

                Screen.Home -> HomeScreen(
                    state = previewHomeUiState.copy(
                        greeting            = currentGreeting(),
                        dateLabel           = currentDateLabel(),
                        user                = homeUser,
                        isLoading           = homeIsLoading,
                        isError             = homeIsError,
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
                    onNavigateToStats      = { statsPeriod = StatsPeriod.WEEK; statsPeriodOffset = 0; screen = Screen.Stats },
                    onNavigateToInsights   = { screen = Screen.Insights },
                    onRetry                = { homeIsLoading = true; homeIsError = false; timelineTrigger++ },
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

                Screen.Stats -> StatsScreen(
                    state        = statsState,
                    onBack       = { screen = Screen.Home },
                    onPeriodChange = { period ->
                        statsPeriod       = period
                        statsPeriodOffset = 0
                        statsState        = StatsUiState(period = period, isLoading = true)
                    },
                    onPrev    = { statsPeriodOffset-- },
                    onNext    = { if (statsPeriodOffset < 0) statsPeriodOffset++ },
                    canGoNext = statsPeriodOffset < 0,
                    onRetry   = { statsRetryTrigger++ },
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
                        avatarUrl       = authUser?.avatarUrl ?: "",
                        totalCO2SavedKg = weeklyTotalKgCo2,
                        daysTracked     = profileDaysTracked,
                        todayKgCO2      = todayImpact.kgCO2,
                        dailyGoalKgCO2  = dailyGoalKgCO2,
                        home        = CommuteLocation("Home", commuteHome.takeIf { it.isNotBlank() }),
                        work        = CommuteLocation("Work", commuteWork.takeIf { it.isNotBlank() }),
                        preferences = previewProfileUiState.preferences.copy(
                            pushNotificationsEnabled = notificationsEnabled,
                            weeklyReportEnabled      = weeklyReportEnabled,
                            dataSharingEnabled       = dataSharingEnabled,
                            appearanceMode           = appearanceMode,
                            defaultTransportLabel    = defaultTransport,
                            unitsLabel               = unitsLabel,
                        ),
                    ),
                    onBack                 = { screen = Screen.Home },
                    onAvatarClick          = {
                        val userId = authUser?.id?.takeIf { it.isNotEmpty() } ?: return@ProfileScreen
                        avatarUploader.launch(userId) { result ->
                            result.onSuccess { url ->
                                scope.launch {
                                    userService.updateAvatarUrl(url)
                                        .onSuccess { dto ->
                                            AuthState.onSignedIn(AuthUser(
                                                id          = dto.id,
                                                email       = dto.email,
                                                displayName = dto.displayName,
                                                avatarUrl   = dto.avatarUrl ?: url,
                                            ))
                                            snackbarHostState.showSnackbar("Profile photo updated")
                                        }
                                        .onFailure {
                                            snackbarHostState.showSnackbar("Could not save photo — try again")
                                        }
                                }
                            }
                            result.onFailure { e ->
                                if (e.message != "Photo selection cancelled") {
                                    scope.launch { snackbarHostState.showSnackbar("Upload failed — try again") }
                                }
                            }
                        }
                    },
                    onNavigateToHome       = { screen = Screen.Home },
                    onNavigateToActivities = { screen = Screen.Activities },
                    onAppearanceChange     = { mode -> appearanceMode = mode; settings.putString("appearance_mode", mode.name) },
                    onNotificationsToggle  = { enabled, onError ->
                        notificationsJob?.cancel()
                        val prev = notificationsEnabled
                        notificationsEnabled = enabled
                        settings.putBoolean("notifications_enabled", enabled)
                        notificationsJob = scope.launch {
                            userService.updatePreferences(pushNotificationsEnabled = enabled).onFailure {
                                notificationsEnabled = prev
                                settings.putBoolean("notifications_enabled", prev)
                                onError("Could not save preference — please try again")
                            }
                        }
                    },
                    onWeeklyReportToggle   = { enabled, onError ->
                        weeklyReportJob?.cancel()
                        val prev = weeklyReportEnabled
                        weeklyReportEnabled = enabled
                        settings.putBoolean("weekly_report", enabled)
                        weeklyReportJob = scope.launch {
                            userService.updatePreferences(weeklyReportEnabled = enabled).onFailure {
                                weeklyReportEnabled = prev
                                settings.putBoolean("weekly_report", prev)
                                onError("Could not save preference — please try again")
                            }
                        }
                    },
                    onDataSharingToggle    = { enabled, onError ->
                        dataSharingJob?.cancel()
                        val prev = dataSharingEnabled
                        dataSharingEnabled = enabled
                        settings.putBoolean("data_sharing", enabled)
                        dataSharingJob = scope.launch {
                            userService.updatePreferences(dataSharingEnabled = enabled).onFailure {
                                dataSharingEnabled = prev
                                settings.putBoolean("data_sharing", prev)
                                onError("Could not save preference — please try again")
                            }
                        }
                    },
                    onGoalChange           = { goal ->
                        val prevGoal = dailyGoalKgCO2
                        dailyGoalKgCO2 = goal
                        settings.putFloat("daily_goal_kg", goal)
                        todayImpact = todayImpact.copy(dailyGoalKgCO2 = goal)
                        scope.launch {
                            userService.updatePreferences(dailyGoalKgCO2e = goal.toDouble()).onFailure {
                                dailyGoalKgCO2 = prevGoal
                                settings.putFloat("daily_goal_kg", prevGoal)
                                todayImpact = todayImpact.copy(dailyGoalKgCO2 = prevGoal)
                                snackbarHostState.showSnackbar("Could not save goal — please try again")
                            }
                        }
                    },
                    onHomeChange      = { addr, onError ->
                        homeAddressJob?.cancel()
                        val prev = commuteHome
                        commuteHome = addr
                        settings.putString("commute_home", addr)
                        homeAddressJob = scope.launch {
                            userService.updatePreferences(homeAddress = addr).onFailure {
                                commuteHome = prev
                                settings.putString("commute_home", prev)
                                onError("Could not save home address — please try again")
                            }
                        }
                    },
                    onWorkChange      = { addr, onError ->
                        workAddressJob?.cancel()
                        val prev = commuteWork
                        commuteWork = addr
                        settings.putString("commute_work", addr)
                        workAddressJob = scope.launch {
                            userService.updatePreferences(workAddress = addr).onFailure {
                                commuteWork = prev
                                settings.putString("commute_work", prev)
                                onError("Could not save work address — please try again")
                            }
                        }
                    },
                    onTransportChange = { label, onError ->
                        transportJob?.cancel()
                        val prev = defaultTransport
                        defaultTransport = label
                        settings.putString("default_transport", label)
                        transportJob = scope.launch {
                            userService.updatePreferences(defaultTransport = label).onFailure {
                                defaultTransport = prev
                                settings.putString("default_transport", prev)
                                onError("Could not save transport mode — please try again")
                            }
                        }
                    },
                    onUnitsChange     = { units, onError ->
                        unitsJob?.cancel()
                        val prev = unitsLabel
                        unitsLabel = units
                        settings.putString("units_label", units)
                        unitsJob = scope.launch {
                            userService.updatePreferences(distanceUnit = units.toDistanceUnit()).onFailure {
                                unitsLabel = prev
                                settings.putString("units_label", prev)
                                onError("Could not save units — please try again")
                            }
                        }
                    },
                    onSaveName             = { name, onSuccess, onError ->
                        scope.launch {
                            userService.updateMe(name)
                                .onSuccess { dto ->
                                    AuthState.onSignedIn(AuthUser(
                                        id          = dto.id,
                                        email       = dto.email,
                                        displayName = dto.displayName,
                                        avatarUrl   = dto.avatarUrl ?: authUser?.avatarUrl ?: "",
                                    ))
                                    onSuccess()
                                }
                                .onFailure { onError() }
                        }
                    },
                    onSignOut    = { handleSignOut() },
                    onDeleteAccount = { password, onError ->
                        if (isDeletingAccount) return@ProfileScreen
                        isDeletingAccount = true
                        scope.launch {
                            userService.deleteMe(password = password)
                                .onSuccess {
                                    runCatching { repo.deleteAllSessions() }
                                    isDeletingAccount = false
                                    handleSignOut()
                                }
                                .onFailure { e ->
                                    isDeletingAccount = false
                                    val msg = e.message ?: "Could not delete account — please try again"
                                    onError(msg)
                                    snackbarHostState.showSnackbar(msg)
                                }
                        }
                    },
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
                        onDelete = { deleteTrip(entry) { screen = Screen.Home } },
                    )
                }

                Screen.Activities -> ActivitiesScreen(
                    groupedEntries   = groupedActivities,
                    onNavigateToHome = { screen = Screen.Home },
                    onTripClick      = { entry -> selectedTrip = entry; screen = Screen.TripDetail },
                    onFabClick       = { showLogActivity = true },
                    onDelete         = { entry -> deleteTrip(entry) { snackbarHostState.showSnackbar("Trip deleted") } },
                )

                Screen.InsightDetail -> selectedInsight?.let { entry ->
                    InsightDetailScreen(
                        entry                  = entry,
                        onBack                 = { screen = Screen.Home },
                        onLogTrip              = { showLogActivity = true },
                        onNavigateToActivities = { screen = Screen.Activities },
                        onNavigateToProfile    = { screen = Screen.Profile },
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
                        // Snapshot so the coroutine sees a stable view of confirmed sessions
                        // even if a DB flow emission arrives mid-edit.
                        val sessionsAtEditTime = confirmedSessions
                        scope.launch {
                            try {
                                if (sessionBeingEdited != null) {
                                    // Edit flow: atomic delete + replace inside one DB transaction
                                    val editNowMs = Clock.System.now().toEpochMilliseconds()

                                    // Resolve the old backend activity ID before mutating local DB.
                                    // For a local session it lives in session.backend_activity_id.
                                    // For a backend-only trip (no local row) the sessionBeingEdited
                                    // IS the backend UUID — that record must be deleted directly.
                                    val localSession = sessionsAtEditTime
                                        .firstOrNull { it.session.id == sessionBeingEdited }
                                    val oldBackendId = localSession?.session?.backend_activity_id
                                        ?: sessionBeingEdited.takeIf { localSession == null && it.isNotEmpty() }

                                    val newId = repo.updateManualTrip(
                                        oldSessionId = sessionBeingEdited,
                                        mode         = trip.mode.name,
                                        distanceKm   = trip.distanceKm,
                                        timestampMs  = originalTimestampMs,
                                    )

                                    // Best-effort: remove the old backend record so it does not
                                    // appear as a duplicate on the next Activities fetch.
                                    // 404 = already gone from another device — both are success.
                                    if (!oldBackendId.isNullOrEmpty()) {
                                        activityService.deleteActivity(oldBackendId)
                                        // Add to the exclusion set so the old record does not
                                        // reappear from the in-memory cache during the window
                                        // before the next timeline fetch. This applies to both
                                        // local trips (backend_activity_id) and backend-only trips.
                                        deletedBackendIds = deletedBackendIds + oldBackendId
                                    }

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
