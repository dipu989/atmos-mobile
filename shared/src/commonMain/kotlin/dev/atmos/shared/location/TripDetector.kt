package dev.atmos.shared.location

import dev.atmos.shared.ui.home.PendingTripEntry
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

// ── Reactive state bridge (background service → Compose UI) ──────────────────

/**
 * Singleton StateFlow holder. Background detection code emits here; AtmosApp collects.
 * Lives in commonMain — no platform dependencies.
 */
object TripDetectorState {
    private val _pendingTrip = MutableStateFlow<PendingTripEntry?>(null)
    val pendingTrip: StateFlow<PendingTripEntry?> = _pendingTrip.asStateFlow()

    private val _permissionState = MutableStateFlow(LocationPermissionState.UNKNOWN)
    val permissionState: StateFlow<LocationPermissionState> = _permissionState.asStateFlow()

    fun emitPendingTrip(trip: PendingTripEntry?) {
        _pendingTrip.value = trip
    }

    fun clearPendingTrip() {
        _pendingTrip.value = null
    }

    fun updatePermissionState(state: LocationPermissionState) {
        _permissionState.value = state
    }
}

// ── TripDetector interface + expect factory ───────────────────────────────────

/**
 * Platform-specific trip detector. Implementations live in androidMain / iosMain.
 * Obtained via [createTripDetector] factory — call once and keep the instance alive.
 */
interface TripDetector {
    /** Start low-power monitoring. Transitions to active GPS when vehicle entry is detected. */
    fun startMonitoring()

    /** Stop all monitoring. Safe to call multiple times. */
    fun stopMonitoring()

    /** Trigger platform-specific permission request. No-op if already granted. */
    fun requestPermissions()
}

expect fun createTripDetector(): TripDetector
