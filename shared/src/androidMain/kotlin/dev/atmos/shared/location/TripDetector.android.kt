package dev.atmos.shared.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dev.atmos.shared.ui.home.PendingTripEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.logactivity.emissionFactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

// ── Context holder ────────────────────────────────────────────────────────────

/**
 * Stores the application context before Compose starts.
 * Called from MainActivity.onCreate() before setContent {}.
 */
object TripDetectorHolder {
    internal var appContext: Context? = null
    private var instance: AndroidTripDetector? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): AndroidTripDetector {
        val ctx = requireNotNull(appContext) {
            "TripDetectorHolder.init(context) must be called before get()"
        }
        return instance ?: AndroidTripDetector(ctx).also { instance = it }
    }
}

// ── actual factory ────────────────────────────────────────────────────────────

actual fun createTripDetector(): TripDetector = TripDetectorHolder.get()

// ── Android implementation ────────────────────────────────────────────────────

class AndroidTripDetector(private val context: Context) : TripDetector {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val prefs by lazy {
        context.getSharedPreferences("atmos_trip", android.content.Context.MODE_PRIVATE)
    }

    // ── State machine ─────────────────────────────────────────────────────────

    @Volatile private var phase: TripPhase = TripPhase.Idle
    private var rawTrip: RawTrip? = null
    private var tripStartingTime = 0L
    private var tripEndingJob: kotlinx.coroutines.Job? = null
    private var gpsLossJob: kotlinx.coroutines.Job? = null
    private var gpsFixes = 0

    // ── Activity Transition registration ──────────────────────────────────────

    private val transitionPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, TripTransitionReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            TRANSITION_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private val transitionRequest = ActivityTransitionRequest(
        listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
        )
    )

    // ── Location callback ─────────────────────────────────────────────────────

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            if (location.accuracy > 50f) return  // discard low-accuracy fixes

            val point = LatLng(location.latitude, location.longitude)
            onGpsFix(point)
        }
    }

    // ── TripDetector interface ────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override fun startMonitoring() {
        // Register activity transition updates (persists across app restarts)
        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(transitionRequest, transitionPendingIntent)

        // Restore any in-progress trip that survived process death
        restorePartialTrip()
    }

    override fun stopMonitoring() {
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(transitionPendingIntent)
        stopLocationUpdates()
        tripEndingJob?.cancel()
        gpsLossJob?.cancel()
        phase = TripPhase.Idle
    }

    override fun requestPermissions() {
        // Permissions are requested via AndroidPermissionRequester in MainActivity.
        // This method is a no-op on Android.
    }

    // ── Transition events (called from TripTransitionReceiver) ────────────────

    fun handleTransition(vehicleActivity: VehicleActivity) {
        scope.launch {
            when (vehicleActivity) {
                VehicleActivity.IN_VEHICLE_ENTERED -> onVehicleEntered()
                VehicleActivity.IN_VEHICLE_EXITED  -> onVehicleExited()
                VehicleActivity.WALKING_ENTERED    -> onWalkingEntered()
            }
        }
    }

    // ── State transitions ─────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun onVehicleEntered() {
        when (phase) {
            TripPhase.Idle -> {
                // Start debounce window — confirm it's a real trip, not a brief sit
                phase = TripPhase.TripStarting
                tripStartingTime = System.currentTimeMillis()
                gpsFixes = 0
                startLocationUpdates()
                startForegroundService()
            }
            TripPhase.TripEnding -> {
                // User got back in vehicle (petrol stop, quick errand) — resume trip
                tripEndingJob?.cancel()
                phase = TripPhase.Active
            }
            else -> Unit
        }
    }

    private fun onVehicleExited() {
        when (phase) {
            TripPhase.TripStarting -> {
                // Exited before confirming — false start, discard
                phase = TripPhase.Idle
                rawTrip = null
                stopLocationUpdates()
                stopForegroundService()
            }
            TripPhase.Active -> {
                // Start grace window — don't end trip yet (could be red light or short stop)
                phase = TripPhase.TripEnding
                tripEndingJob = scope.launch {
                    delay(TRIP_ENDING_GRACE_MS)
                    // Grace expired without re-entering vehicle or walking — end trip
                    if (phase == TripPhase.TripEnding) completeTrip()
                }
            }
            else -> Unit
        }
    }

    private fun onWalkingEntered() {
        if (phase == TripPhase.TripEnding) {
            // User is now walking after exiting vehicle — confident trip ended
            tripEndingJob?.cancel()
            completeTrip()
        }
    }

    // ── GPS tracking ──────────────────────────────────────────────────────────

    private fun onGpsFix(point: LatLng) {
        gpsLossJob?.cancel()
        gpsLossJob = scope.launch {
            delay(GPS_LOSS_TIMEOUT_MS)
            // GPS lost for 10 min — end trip with what we have
            if (phase == TripPhase.Active || phase == TripPhase.TripStarting) completeTrip()
        }

        when (phase) {
            TripPhase.TripStarting -> {
                gpsFixes++
                val elapsed = System.currentTimeMillis() - tripStartingTime
                if (gpsFixes >= 3 && elapsed >= TRIP_STARTING_DEBOUNCE_MS) {
                    // Confirmed trip start
                    rawTrip = RawTrip(startTimeMs = tripStartingTime)
                    rawTrip!!.waypoints.add(point)
                    phase = TripPhase.Active
                    persistPartialTrip()
                }
            }
            TripPhase.Active, TripPhase.TripEnding -> {
                val trip = rawTrip ?: return
                val prev = trip.waypoints.lastOrNull()
                if (prev != null) {
                    trip.distanceKm += haversineKm(prev, point)
                }
                trip.waypoints.add(point)
                trip.lastFixTimeMs = System.currentTimeMillis()
                trip.durationMin = ((System.currentTimeMillis() - trip.startTimeMs) / 60_000).toInt()
                persistPartialTrip()
            }
            else -> Unit
        }
    }

    // ── Trip completion ───────────────────────────────────────────────────────

    private fun completeTrip() {
        val trip = rawTrip ?: run {
            resetToIdle()
            return
        }
        trip.durationMin = ((System.currentTimeMillis() - trip.startTimeMs) / 60_000).toInt()

        // Discard noise trips
        if (trip.distanceKm < MIN_TRIP_DISTANCE_KM || trip.durationMin < MIN_TRIP_DURATION_MIN) {
            resetToIdle()
            return
        }

        val mode = TransportModeType.DRIVING  // Activity Recognition confirms IN_VEHICLE
        val co2 = trip.distanceKm * mode.emissionFactor

        val pending = PendingTripEntry(
            mode            = mode,
            origin          = resolveAddress(trip.waypoints.firstOrNull()),
            destination     = resolveAddress(trip.waypoints.lastOrNull()),
            distanceKm      = trip.distanceKm,
            durationMin     = trip.durationMin,
            estimatedKgCO2  = co2,
        )

        TripDetectorState.emitPendingTrip(pending)
        postTripNotification()
        resetToIdle()
    }

    private fun resetToIdle() {
        phase = TripPhase.Idle
        rawTrip = null
        tripEndingJob?.cancel()
        gpsLossJob?.cancel()
        stopLocationUpdates()
        stopForegroundService()
        clearPersistedTrip()
    }

    // ── Location updates ──────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Guard — if permission not granted yet (e.g. debug buttons on onboarding), skip silently.
        // GPS fixes will still arrive via Extended Controls → FusedLocation once permission is granted.
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30_000L)
            .setMinUpdateIntervalMillis(15_000L)
            .build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    // ── Foreground service ────────────────────────────────────────────────────

    private fun startForegroundService() {
        // API 34+: startForeground() on a foregroundServiceType="location" service throws
        // SecurityException if location permission isn't granted — guard before starting.
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        try {
            val intent = Intent(context, Class.forName("dev.atmos.android.TripDetectionService"))
                .putExtra(TripDetectionService.EXTRA_ACTION, TripDetectionService.ACTION_START)
            context.startForegroundService(intent)
        } catch (_: Exception) { /* service not available in test/debug scenarios */ }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(context, Class.forName("dev.atmos.android.TripDetectionService"))
                .putExtra(TripDetectionService.EXTRA_ACTION, TripDetectionService.ACTION_STOP)
            context.startService(intent)
        } catch (_: Exception) { }
    }

    private fun postTripNotification() {
        val intent = Intent(context, Class.forName("dev.atmos.android.TripDetectionService"))
            .putExtra(TripDetectionService.EXTRA_ACTION, TripDetectionService.ACTION_TRIP_DETECTED)
        context.startService(intent)
    }

    // ── Address resolution (stub — replace with reverse geocoding) ────────────

    private fun resolveAddress(point: LatLng?): String {
        // TODO: replace with Geocoder or a reverse-geocoding API call
        return point?.let { "(${String.format("%.4f", it.lat)}, ${String.format("%.4f", it.lng)})" }
            ?: "Unknown"
    }

    // ── Persistence (survive process death) ───────────────────────────────────

    private fun persistPartialTrip() {
        val trip = rawTrip ?: return
        val json = Json.encodeToString(
            RawTripDto(
                startTimeMs = trip.startTimeMs,
                distanceKm  = trip.distanceKm,
                durationMin = trip.durationMin,
                waypoints   = trip.waypoints.map { WaypointDto(it.lat, it.lng) },
            )
        )
        prefs.edit()
            .putString(PREF_RAW_TRIP, json)
            .putString(PREF_PHASE, phase::class.simpleName ?: "Idle")
            .apply()
    }

    private fun restorePartialTrip() {
        val json = prefs.getString(PREF_RAW_TRIP, null) ?: return
        try {
            val dto = Json.decodeFromString<RawTripDto>(json)
            rawTrip = RawTrip(
                startTimeMs = dto.startTimeMs,
                waypoints   = dto.waypoints.map { LatLng(it.lat, it.lng) }.toMutableList(),
                distanceKm  = dto.distanceKm,
                durationMin = dto.durationMin,
            )
            phase = TripPhase.Active  // Restore as active — user was in a trip
            startLocationUpdates()
            startForegroundService()
        } catch (_: Exception) {
            clearPersistedTrip()
        }
    }

    private fun clearPersistedTrip() {
        prefs.edit().remove(PREF_RAW_TRIP).remove(PREF_PHASE).apply()
    }

    // ── Companion (static access from BroadcastReceiver) ─────────────────────

    companion object {
        private const val TRANSITION_REQUEST_CODE = 1001
        private const val PREF_RAW_TRIP = "atmos_raw_trip"
        private const val PREF_PHASE    = "atmos_trip_phase"

        fun handleTransition(context: Context, event: VehicleActivity) {
            TripDetectorHolder.init(context)
            TripDetectorHolder.get().handleTransition(event)
        }
    }
}

// ── Serialization DTOs (for multiplatform-settings persistence) ───────────────

@kotlinx.serialization.Serializable
data class WaypointDto(val lat: Double, val lng: Double)

@kotlinx.serialization.Serializable
data class RawTripDto(
    val startTimeMs: Long,
    val distanceKm: Float,
    val durationMin: Int,
    val waypoints: List<WaypointDto>,
)

// ── Service action constants (referenced before androidApp is compiled) ───────

object TripDetectionService {
    const val EXTRA_ACTION     = "action"
    const val ACTION_START     = "start"
    const val ACTION_STOP      = "stop"
    const val ACTION_TRIP_DETECTED = "trip_detected"
}
