package dev.atmos.shared.location

import dev.atmos.shared.ui.home.TransportModeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ── Permission state ──────────────────────────────────────────────────────────

enum class LocationPermissionState {
    UNKNOWN,          // not yet asked
    GRANTED,          // always / background granted
    BACKGROUND_ONLY,  // while-in-use only — foreground detection works, not background
    DENIED,           // user denied — manual logging only
}

// ── UI models: ongoing trip ───────────────────────────────────────────────────

/**
 * Snapshot of an in-progress session fed to OngoingTripCard.
 * Published after every state machine transition and every GPS fix.
 */
data class OngoingSessionUiState(
    val sessionId: String,
    /** Current phase — drives which visual variant of OngoingTripCard to show. */
    val phase: SessionPhase,
    val elapsedMin: Int,
    val totalDistKm: Float,
    /** The leg currently accumulating distance. Null during SessionStarting. */
    val currentLeg: OngoingLegUiState?,
    /** All legs that have already ended within this session. */
    val completedLegs: List<CompletedLegSummary>,
    /** Non-null during LegEnding — seconds remaining in the 60 s grace window. */
    val secondsUntilClose: Int? = null,
)

/** Live stats for the leg currently in progress. */
data class OngoingLegUiState(
    val mode: TransportModeType,
    val distanceKm: Float,
    val elapsedMin: Int,
)

/** Compact summary of a finished leg — shown in the completed-legs strip ("🚗 3.1 km  🚶 2.1 km"). */
data class CompletedLegSummary(
    val mode: TransportModeType,
    val distanceKm: Float,
)

// ── UI models: pending confirmation ──────────────────────────────────────────

/**
 * Emitted when a session ends via the system auto-end path (20-min stationary timeout).
 * Shown in PendingTripCard; user can confirm, edit, or dismiss.
 */
data class PendingSessionEntry(
    val sessionId: String,
    val legs: List<PendingLegEntry>,
    val totalDistKm: Float,
    val totalDurationMin: Int,
)

/** One leg within a pending session. Mode can be edited by the user before confirming. */
data class PendingLegEntry(
    val legId: String,
    val mode: TransportModeType,
    val distanceKm: Float,
)

// ── UI model: recently saved session (5-second snackbar window) ───────────────

/**
 * Emitted into [TripDetectorState.recentlySaved] immediately after a user-initiated save.
 * Cleared automatically after 5 s. AtmosApp shows a "Trip saved · X km [Edit]" snackbar.
 */
data class RecentlySavedSession(
    val sessionId: String,
    val totalDistKm: Float,
    /** Mode of the first leg — used to pre-fill LogActivitySheet when user taps "Edit". */
    val firstLegMode: TransportModeType?,
    /** Epoch-ms start time — preserved when editing so the trip stays in the same date group. */
    val startedAtMs: Long = 0L,
)

// ── Reactive state bridge (background → Compose UI) ──────────────────────────

/**
 * Singleton StateFlow holder. The platform detector emits here; AtmosApp collects.
 * Lives in commonMain — no platform dependencies.
 */
object TripDetectorState {

    /** Non-null while a session is actively being tracked (SessionStarting → SessionClosing). */
    private val _ongoingSession = MutableStateFlow<OngoingSessionUiState?>(null)
    val ongoingSession: StateFlow<OngoingSessionUiState?> = _ongoingSession.asStateFlow()

    /** Non-null when a session ended via system auto-end and awaits user confirmation. */
    private val _pendingSession = MutableStateFlow<PendingSessionEntry?>(null)
    val pendingSession: StateFlow<PendingSessionEntry?> = _pendingSession.asStateFlow()

    /**
     * Non-null for ~5 s after a user-initiated save. Carries distance + first leg mode
     * so AtmosApp can show "Trip saved · X.X km [Edit]" without a DB round-trip.
     * Cleared automatically by the detector after 5 s.
     */
    private val _recentlySaved = MutableStateFlow<RecentlySavedSession?>(null)
    val recentlySaved: StateFlow<RecentlySavedSession?> = _recentlySaved.asStateFlow()

    /** Drives the Location permission pill on OnboardingScreen. */
    private val _permissionState = MutableStateFlow(LocationPermissionState.UNKNOWN)
    val permissionState: StateFlow<LocationPermissionState> = _permissionState.asStateFlow()

    /** Drives the Notifications permission pill on OnboardingScreen. */
    private val _notificationsGranted = MutableStateFlow(false)
    val notificationsGranted: StateFlow<Boolean> = _notificationsGranted.asStateFlow()

    fun emitOngoingSession(state: OngoingSessionUiState?) { _ongoingSession.value = state }
    fun emitPendingSession(session: PendingSessionEntry?) { _pendingSession.value = session }
    fun emitRecentlySaved(session: RecentlySavedSession?) { _recentlySaved.value = session }
    fun updatePermissionState(state: LocationPermissionState) { _permissionState.value = state }
    fun updateNotificationsGranted(granted: Boolean) { _notificationsGranted.value = granted }
}

// ── TripDetector interface + expect factory ───────────────────────────────────

/**
 * Platform-specific trip detector. Implementations live in androidMain / iosMain.
 * Obtain via [createTripDetector] — call once and keep the instance alive.
 */
interface TripDetector {
    /** Start low-power monitoring. Transitions to active GPS on vehicle entry. */
    fun startMonitoring()

    /** Stop all monitoring. Safe to call multiple times. */
    fun stopMonitoring()

    /** Trigger the platform permission request flow. No-op if already granted. */
    fun requestPermissions()

    /**
     * User-initiated session end (notification "End Trip" button or in-app "Stop & save").
     * Saves immediately — does NOT emit to [TripDetectorState.pendingSession].
     */
    fun manualEndAndSave()

    /** Discard the current session without saving. Clears [TripDetectorState.ongoingSession]. */
    fun discardSession()

    /**
     * User confirmed a system-auto-ended session (tapped "Confirm ✓" on PendingTripCard).
     * Marks the session as confirmed in the DB and clears [TripDetectorState.pendingSession].
     */
    fun confirmPendingSession(sessionId: String)

    /**
     * User dismissed a pending session without saving (tapped "✕" or scrolled away).
     * Hard-deletes the session from the DB and clears [TripDetectorState.pendingSession].
     */
    fun dismissPendingSession(sessionId: String)

    /**
     * Cancel the LegEnding grace window and resume the current active leg.
     * Called when the user taps "Resume" on the LegEnding countdown banner.
     * No-op if not in [SessionPhase.LegEnding].
     */
    fun resumeLeg()

    /**
     * Inject an activity transition event directly.
     * Used by [TripTransitionReceiver] (Android).
     */
    fun handleTransition(event: VehicleActivity)
}

expect fun createTripDetector(): TripDetector
