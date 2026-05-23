package dev.atmos.shared.location

// ── Trip detection phases ─────────────────────────────────────────────────────

sealed class TripPhase {
    /** No movement detected. Low-power monitoring active. */
    data object Idle : TripPhase()

    /** Vehicle entry just detected. Collecting confirming GPS fixes (60 s debounce). */
    data object TripStarting : TripPhase()

    /** Trip confirmed and in progress. Full GPS tracking active. */
    data object Active : TripPhase()

    /** Vehicle exit detected. 60 s grace window — might be a short stop. */
    data object TripEnding : TripPhase()

    /** Trip complete. PendingTripEntry ready to emit. */
    data object Completed : TripPhase()
}

// ── Raw GPS models ────────────────────────────────────────────────────────────

data class LatLng(
    val lat: Double,
    val lng: Double,
)

/**
 * Accumulates raw GPS data during an active trip.
 * Persisted to multiplatform-settings so partial trips survive process death.
 */
data class RawTrip(
    val startTimeMs: Long,
    val waypoints: MutableList<LatLng> = mutableListOf(),
    var distanceKm: Float = 0f,
    var durationMin: Int = 0,
    var lastFixTimeMs: Long = startTimeMs,
)

// ── Activity transition events (platform-agnostic) ────────────────────────────

enum class VehicleActivity {
    IN_VEHICLE_ENTERED,
    IN_VEHICLE_EXITED,
    WALKING_ENTERED,
}

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

const val MIN_TRIP_DISTANCE_KM = 0.5f
const val MIN_TRIP_DURATION_MIN = 3
// TODO: TESTING — revert before shipping
const val TRIP_STARTING_DEBOUNCE_MS = 5_000L    // was 60_000L
const val TRIP_ENDING_GRACE_MS      = 5_000L    // was 60_000L
const val GPS_LOSS_TIMEOUT_MS = 10 * 60_000L    // end trip after 10 min GPS loss
