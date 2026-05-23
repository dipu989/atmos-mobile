package dev.atmos.shared.location

import dev.atmos.shared.ui.home.PendingTripEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.logactivity.emissionFactor
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.CoreMotion.CMMotionActivity
import platform.CoreMotion.CMMotionActivityConfidenceMedium
import platform.CoreMotion.CMMotionActivityManager
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject

// ── actual factory ────────────────────────────────────────────────────────────

actual fun createTripDetector(): TripDetector = IosTripDetector()

// ── Time helper ───────────────────────────────────────────────────────────────

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

// ── CLLocationManager delegate (must extend NSObject exclusively) ─────────────

@OptIn(ExperimentalForeignApi::class)
class AtmosLocationDelegate(
    private val onLocationUpdate: (CLLocation) -> Unit,
    private val onAuthorizationChange: () -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        onLocationUpdate(loc)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        onAuthorizationChange()
    }
}

// ── iOS TripDetector — only implements Kotlin TripDetector interface ───────────

@OptIn(ExperimentalForeignApi::class)
class IosTripDetector : TripDetector {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val locationManager = CLLocationManager()
    private val motionManager = CMMotionActivityManager()

    private val locationDelegate = AtmosLocationDelegate(
        onLocationUpdate   = { loc -> scope.launch { handleLocation(loc) } },
        onAuthorizationChange = { checkCurrentPermissionState() },
    )

    // State machine — accessed only on Main dispatcher
    private var phase: TripPhase = TripPhase.Idle
    private var rawTrip: RawTrip? = null
    private var tripStartingTime = 0L
    private var tripEndingJob: Job? = null
    private var gpsLossJob: Job? = null
    private var wasAutomotive = false

    init {
        locationManager.delegate = locationDelegate
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
    }

    // ── TripDetector interface ────────────────────────────────────────────────

    override fun startMonitoring() {
        locationManager.startMonitoringSignificantLocationChanges()
        startMotionUpdates()
        checkCurrentPermissionState()
    }

    override fun stopMonitoring() {
        locationManager.stopUpdatingLocation()
        locationManager.stopMonitoringSignificantLocationChanges()
        motionManager.stopActivityUpdates()
        tripEndingJob?.cancel()
        gpsLossJob?.cancel()
        phase = TripPhase.Idle
    }

    override fun requestPermissions() {
        locationManager.requestAlwaysAuthorization()
    }

    // ── Motion activity updates ───────────────────────────────────────────────

    private fun startMotionUpdates() {
        if (!CMMotionActivityManager.isActivityAvailable()) return

        motionManager.startActivityUpdatesToQueue(NSOperationQueue.mainQueue) { activity ->
            activity ?: return@startActivityUpdatesToQueue
            scope.launch { handleMotionActivity(activity) }
        }
    }

    private fun handleMotionActivity(activity: CMMotionActivity) {
        val hasEnoughConfidence = activity.confidence >= CMMotionActivityConfidenceMedium
        val isAutomotive = activity.automotive && hasEnoughConfidence
        val isWalking    = (activity.walking || activity.running) && hasEnoughConfidence

        when {
            isAutomotive && !wasAutomotive -> onVehicleEntered()
            !isAutomotive && wasAutomotive  -> onVehicleExited()
            isWalking && phase == TripPhase.TripEnding -> onWalkingEntered()
        }
        wasAutomotive = isAutomotive
    }

    // ── State transitions ─────────────────────────────────────────────────────

    private fun onVehicleEntered() {
        when (phase) {
            TripPhase.Idle -> {
                phase = TripPhase.TripStarting
                tripStartingTime = nowMs()
                locationManager.desiredAccuracy = kCLLocationAccuracyBest
                locationManager.startUpdatingLocation()
            }
            TripPhase.TripEnding -> {
                tripEndingJob?.cancel()
                phase = TripPhase.Active
            }
            else -> Unit
        }
    }

    private fun onVehicleExited() {
        when (phase) {
            TripPhase.TripStarting -> {
                phase = TripPhase.Idle
                rawTrip = null
                locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
                locationManager.stopUpdatingLocation()
                locationManager.startMonitoringSignificantLocationChanges()
            }
            TripPhase.Active -> {
                phase = TripPhase.TripEnding
                tripEndingJob = scope.launch {
                    delay(TRIP_ENDING_GRACE_MS)
                    if (phase == TripPhase.TripEnding) completeTrip()
                }
            }
            else -> Unit
        }
    }

    private fun onWalkingEntered() {
        tripEndingJob?.cancel()
        completeTrip()
    }

    // ── Location handling ─────────────────────────────────────────────────────

    private fun handleLocation(location: CLLocation) {
        if (location.horizontalAccuracy > 50.0) return
        val (lat, lng) = location.coordinate.useContents { latitude to longitude }
        onGpsFix(LatLng(lat, lng))
    }

    private fun checkCurrentPermissionState() {
        val state = when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways    -> LocationPermissionState.GRANTED
            kCLAuthorizationStatusAuthorizedWhenInUse -> LocationPermissionState.BACKGROUND_ONLY
            kCLAuthorizationStatusDenied              -> LocationPermissionState.DENIED
            kCLAuthorizationStatusNotDetermined       -> LocationPermissionState.UNKNOWN
            else                                      -> LocationPermissionState.UNKNOWN
        }
        TripDetectorState.updatePermissionState(state)
    }

    // ── GPS fix handling ──────────────────────────────────────────────────────

    private fun onGpsFix(point: LatLng) {
        gpsLossJob?.cancel()
        gpsLossJob = scope.launch {
            delay(GPS_LOSS_TIMEOUT_MS)
            if (phase == TripPhase.Active || phase == TripPhase.TripStarting) completeTrip()
        }

        when (phase) {
            TripPhase.TripStarting -> {
                val elapsed = nowMs() - tripStartingTime
                if (elapsed >= TRIP_STARTING_DEBOUNCE_MS) {
                    rawTrip = RawTrip(startTimeMs = tripStartingTime)
                    rawTrip!!.waypoints.add(point)
                    phase = TripPhase.Active
                }
            }
            TripPhase.Active, TripPhase.TripEnding -> {
                val trip = rawTrip ?: return
                val prev = trip.waypoints.lastOrNull()
                if (prev != null) trip.distanceKm += haversineKm(prev, point)
                trip.waypoints.add(point)
                trip.durationMin = ((nowMs() - trip.startTimeMs) / 60_000L).toInt()
            }
            else -> Unit
        }
    }

    // ── Trip completion ───────────────────────────────────────────────────────

    private fun completeTrip() {
        val trip = rawTrip ?: run { resetToIdle(); return }
        trip.durationMin = ((nowMs() - trip.startTimeMs) / 60_000L).toInt()

        if (trip.distanceKm < MIN_TRIP_DISTANCE_KM || trip.durationMin < MIN_TRIP_DURATION_MIN) {
            resetToIdle()
            return
        }

        val mode = TransportModeType.DRIVING
        val pending = PendingTripEntry(
            mode           = mode,
            origin         = "Detected start",
            destination    = "Detected end",
            distanceKm     = trip.distanceKm,
            durationMin    = trip.durationMin,
            estimatedKgCO2 = trip.distanceKm * mode.emissionFactor,
        )

        TripDetectorState.emitPendingTrip(pending)
        resetToIdle()
    }

    private fun resetToIdle() {
        phase = TripPhase.Idle
        rawTrip = null
        tripEndingJob?.cancel()
        gpsLossJob?.cancel()
        wasAutomotive = false
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.stopUpdatingLocation()
        locationManager.startMonitoringSignificantLocationChanges()
    }
}
