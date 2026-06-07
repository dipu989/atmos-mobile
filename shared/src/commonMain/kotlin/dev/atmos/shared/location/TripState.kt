package dev.atmos.shared.location

import dev.atmos.shared.ui.home.TransportModeType

// ── Session phases ────────────────────────────────────────────────────────────

sealed class SessionPhase {
    /** No movement detected. Low-power monitoring active. */
    data object Idle : SessionPhase()

    /** Vehicle entry just detected. Collecting confirming GPS fixes (60 s debounce). */
    data object SessionStarting : SessionPhase()

    /** Session confirmed and in progress. Current leg mode is known. */
    data class Active(val currentMode: TransportModeType) : SessionPhase()

    /**
     * Current leg ended (e.g. exited vehicle). 60 s grace window — user might
     * re-enter the same vehicle (petrol stop) or transition to a new mode (walking).
     */
    data class LegEnding(val previousMode: TransportModeType) : SessionPhase()

    /** Movement has stopped for 20+ min. Session is being closed and saved. */
    data object SessionClosing : SessionPhase()
}

// ── In-memory leg + session models ───────────────────────────────────────────

/**
 * A single continuous movement segment using one transport mode.
 * Accumulated in memory during detection; written to DB when the leg ends.
 */
data class LegState(
    val legId: String,
    val mode: TransportModeType,
    val startTimeMs: Long,
    val waypoints: MutableList<LatLng> = mutableListOf(),
    var distanceKm: Float = 0f,
)

/**
 * The full in-progress session. Contains all completed legs plus the
 * currently-active leg. Written to DB incrementally; finalised on SessionClosing.
 */
data class SessionState(
    val sessionId: String,
    val startTimeMs: Long,
    val legs: MutableList<LegState> = mutableListOf(),  // completed legs
    var currentLeg: LegState? = null,                   // leg in progress
)

/** Total distance across all completed legs + the current active leg. */
val SessionState.totalDistanceKm: Float
    get() = legs.sumOf { it.distanceKm.toDouble() }.toFloat() +
            (currentLeg?.distanceKm ?: 0f)

// ── Activity transition events ────────────────────────────────────────────────

enum class VehicleActivity {
    IN_VEHICLE_ENTERED,
    IN_VEHICLE_EXITED,
    WALKING_ENTERED,
    STILL_ENTERED,   // stationary for sustained period — triggers session close
}

// ── GPS model ─────────────────────────────────────────────────────────────────

data class LatLng(
    val lat: Double,
    val lng: Double,
)

// ── Distance helpers ──────────────────────────────────────────────────────────

/** Haversine formula — returns distance in km between two lat/lng points. */
fun haversineKm(a: LatLng, b: LatLng): Float {
    val r = 6371.0
    val toRad = kotlin.math.PI / 180.0
    val dLat = (b.lat - a.lat) * toRad
    val dLon = (b.lng - a.lng) * toRad
    val sinDLat = kotlin.math.sin(dLat / 2)
    val sinDLon = kotlin.math.sin(dLon / 2)
    val h = sinDLat * sinDLat +
            kotlin.math.cos(a.lat * toRad) *
            kotlin.math.cos(b.lat * toRad) *
            sinDLon * sinDLon
    return (2 * r * kotlin.math.asin(kotlin.math.sqrt(h))).toFloat()
}

// ── Trip validity thresholds ──────────────────────────────────────────────────

const val MIN_TRIP_DISTANCE_KM       = 0.5f
const val MIN_TRIP_DURATION_MIN      = 3
const val TRIP_STARTING_DEBOUNCE_MS  = 30_000L       // 30 s confirmation window
const val TRIP_ENDING_GRACE_MS       = 60_000L        // 60 s grace before ending a leg
const val GPS_LOSS_TIMEOUT_MS        = 10 * 60_000L   // end trip after 10 min GPS loss
const val SESSION_STILL_TIMEOUT_MS   = 20 * 60_000L   // close session after 20 min stationary
