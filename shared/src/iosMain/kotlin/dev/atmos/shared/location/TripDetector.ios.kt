package dev.atmos.shared.location

import dev.atmos.shared.db.DatabaseDriverFactory
import dev.atmos.shared.db.DatabaseProvider
import dev.atmos.shared.db.TripRepository
import dev.atmos.shared.db.TripRepositoryImpl
import dev.atmos.shared.ui.home.TransportModeType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
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
import platform.Foundation.NSUUID
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSUserDefaults
import platform.darwin.NSObject

// ── actual factory ────────────────────────────────────────────────────────────

actual fun createTripDetector(): TripDetector {
    // Idempotent — safe to call even if MainActivity already called it
    DatabaseProvider.init(DatabaseDriverFactory())
    return IosTripDetector(TripRepositoryImpl(DatabaseProvider.database))
}

// ── Time helper ───────────────────────────────────────────────────────────────

private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

// ── CLLocationManager delegate ────────────────────────────────────────────────

/**
 * CLLocationManager delegate must extend NSObject — it cannot be a plain Kotlin class.
 * We keep it thin and forward events to lambdas so [IosTripDetector] stays a pure Kotlin class.
 */
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

// ── iOS TripDetector ──────────────────────────────────────────────────────────

@OptIn(ExperimentalForeignApi::class)
class IosTripDetector(private val repo: TripRepository) : TripDetector {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val locationManager = CLLocationManager()
    private val motionManager = CMMotionActivityManager()
    private val defaults = NSUserDefaults.standardUserDefaults

    private val locationDelegate = AtmosLocationDelegate(
        onLocationUpdate      = { loc -> scope.launch { handleLocation(loc) } },
        onAuthorizationChange = { checkCurrentPermissionState() },
    )

    // ── State ─────────────────────────────────────────────────────────────────

    private var currentPhase: SessionPhase = SessionPhase.Idle
    private var sessionState: SessionState? = null
    private var legSortOrder = 0
    private var debounceStartMs = 0L
    private var legEndingSecondsLeft = 0

    // Motion edge-tracking
    private var wasAutomotive = false
    private var wasWalking = false

    // ── Coroutine jobs ────────────────────────────────────────────────────────

    private var debounceJob: Job? = null
    private var legEndingJob: Job? = null
    private var stillTimeoutJob: Job? = null
    private var gpsLossJob: Job? = null

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
        restorePartialSession()
    }

    override fun stopMonitoring() {
        locationManager.stopUpdatingLocation()
        locationManager.stopMonitoringSignificantLocationChanges()
        motionManager.stopActivityUpdates()
        cancelAllJobs()
        currentPhase = SessionPhase.Idle
    }

    override fun requestPermissions() {
        locationManager.requestAlwaysAuthorization()
    }

    override fun manualEndAndSave() {
        scope.launch { closeSession(userInitiated = true) }
    }

    override fun discardSession() {
        scope.launch {
            sessionState?.let { repo.deleteSession(it.sessionId) }
            resetToIdle()
        }
    }

    override fun confirmPendingSession(sessionId: String) {
        scope.launch {
            repo.confirmSession(sessionId)
            TripDetectorState.emitPendingSession(null)
        }
    }

    override fun dismissPendingSession(sessionId: String) {
        scope.launch {
            repo.deleteSession(sessionId)
            TripDetectorState.emitPendingSession(null)
        }
    }

    override fun resumeLeg() {
        scope.launch {
            val phase = currentPhase
            if (phase is SessionPhase.LegEnding) {
                legEndingJob?.cancel()
                stillTimeoutJob?.cancel()
                currentPhase = SessionPhase.Active(phase.previousMode)
                publishOngoingSession()
            }
        }
    }

    override fun handleTransition(event: VehicleActivity) {
        scope.launch {
            when (event) {
                VehicleActivity.IN_VEHICLE_ENTERED -> onVehicleEntered()
                VehicleActivity.IN_VEHICLE_EXITED  -> onVehicleExited()
                VehicleActivity.WALKING_ENTERED    -> onWalkingEntered()
                VehicleActivity.STILL_ENTERED      -> onStillEntered()
            }
        }
    }

    // ── Motion activity ───────────────────────────────────────────────────────

    private fun startMotionUpdates() {
        if (!CMMotionActivityManager.isActivityAvailable()) return
        motionManager.startActivityUpdatesToQueue(NSOperationQueue.mainQueue) { activity ->
            activity ?: return@startActivityUpdatesToQueue
            scope.launch { handleMotionActivity(activity) }
        }
    }

    private fun handleMotionActivity(activity: CMMotionActivity) {
        val confident    = activity.confidence >= CMMotionActivityConfidenceMedium
        val isAutomotive = activity.automotive && confident
        val isWalking    = (activity.walking || activity.running) && confident
        val isStationary = activity.stationary && confident

        when {
            isAutomotive && !wasAutomotive          -> handleTransition(VehicleActivity.IN_VEHICLE_ENTERED)
            !isAutomotive && wasAutomotive          -> handleTransition(VehicleActivity.IN_VEHICLE_EXITED)
            isWalking && !wasWalking && !isAutomotive -> handleTransition(VehicleActivity.WALKING_ENTERED)
            isStationary && !isAutomotive           -> handleTransition(VehicleActivity.STILL_ENTERED)
        }

        wasAutomotive = isAutomotive
        wasWalking    = isWalking
    }

    // ── State transitions (mirror Android exactly) ────────────────────────────

    private suspend fun onVehicleEntered() {
        when (val phase = currentPhase) {
            is SessionPhase.Idle -> {
                currentPhase = SessionPhase.SessionStarting
                debounceStartMs = nowMs()
                switchToHighAccuracy()
                debounceJob = scope.launch {
                    delay(TRIP_STARTING_DEBOUNCE_MS)
                    if (currentPhase is SessionPhase.SessionStarting) {
                        confirmSessionStart(TransportModeType.DRIVING)
                    }
                }
            }
            is SessionPhase.LegEnding -> {
                legEndingJob?.cancel()
                stillTimeoutJob?.cancel()
                if (phase.previousMode != TransportModeType.DRIVING) {
                    completeCurrentLeg()
                    startNewLeg(TransportModeType.DRIVING)
                }
                currentPhase = SessionPhase.Active(TransportModeType.DRIVING)
                publishOngoingSession()
            }
            is SessionPhase.Active -> {
                if (phase.currentMode == TransportModeType.WALKING) {
                    completeCurrentLeg()
                    startNewLeg(TransportModeType.DRIVING)
                    currentPhase = SessionPhase.Active(TransportModeType.DRIVING)
                    publishOngoingSession()
                }
            }
            else -> Unit
        }
    }

    private suspend fun onVehicleExited() {
        when (val phase = currentPhase) {
            is SessionPhase.SessionStarting -> {
                debounceJob?.cancel()
                switchToLowPower()
                currentPhase = SessionPhase.Idle
                publishOngoingSession()
            }
            is SessionPhase.Active -> {
                currentPhase = SessionPhase.LegEnding(phase.currentMode)
                legEndingJob = scope.launch {
                    for (i in (TRIP_ENDING_GRACE_MS / 1_000L).toInt() downTo 0) {
                        legEndingSecondsLeft = i
                        publishOngoingSession()
                        if (i == 0) break
                        delay(1_000L)
                    }
                    if (currentPhase is SessionPhase.LegEnding) {
                        closeSession(userInitiated = false)
                    }
                }
            }
            else -> Unit
        }
    }

    private suspend fun onWalkingEntered() {
        when (val phase = currentPhase) {
            is SessionPhase.LegEnding -> {
                legEndingJob?.cancel()
                stillTimeoutJob?.cancel()
                completeCurrentLeg()
                startNewLeg(TransportModeType.WALKING)
                currentPhase = SessionPhase.Active(TransportModeType.WALKING)
                publishOngoingSession()
            }
            is SessionPhase.Active -> {
                if (phase.currentMode == TransportModeType.DRIVING) {
                    completeCurrentLeg()
                    startNewLeg(TransportModeType.WALKING)
                    currentPhase = SessionPhase.Active(TransportModeType.WALKING)
                    publishOngoingSession()
                }
            }
            else -> Unit
        }
    }

    private fun onStillEntered() {
        when (currentPhase) {
            is SessionPhase.Active, is SessionPhase.LegEnding -> {
                legEndingJob?.cancel()
                stillTimeoutJob?.cancel()
                stillTimeoutJob = scope.launch {
                    delay(SESSION_STILL_TIMEOUT_MS)
                    if (currentPhase is SessionPhase.Active ||
                        currentPhase is SessionPhase.LegEnding) {
                        closeSession(userInitiated = false)
                    }
                }
            }
            else -> Unit
        }
    }

    // ── Session + leg lifecycle ───────────────────────────────────────────────

    private suspend fun confirmSessionStart(mode: TransportModeType) {
        val sessionId = NSUUID().UUIDString()
        val legId     = NSUUID().UUIDString()
        val nowMs     = nowMs()

        repo.startSession(sessionId, nowMs)
        repo.addLeg(legId, sessionId, mode.name, nowMs, 0L)

        sessionState = SessionState(
            sessionId   = sessionId,
            startTimeMs = nowMs,
            legs        = mutableListOf(),
            currentLeg  = LegState(legId, mode, nowMs),
        )
        legSortOrder = 0

        currentPhase = SessionPhase.Active(mode)
        persistPartialSession()
        publishOngoingSession()
    }

    private suspend fun completeCurrentLeg() {
        val session = sessionState ?: return
        val leg     = session.currentLeg ?: return
        val nowMs   = nowMs()

        repo.completeLeg(
            id             = leg.legId,
            endedAtMs      = nowMs,
            distanceKm     = leg.distanceKm.toDouble(),
            waypointsJson  = "[]",   // iOS: waypoints not persisted (GPS re-accumulates on resume)
        )
        session.legs.add(leg)
        session.currentLeg = null
    }

    private suspend fun startNewLeg(mode: TransportModeType) {
        val session = sessionState ?: return
        val legId   = NSUUID().UUIDString()
        val nowMs   = nowMs()
        legSortOrder++

        repo.addLeg(legId, session.sessionId, mode.name, nowMs, legSortOrder.toLong())
        session.currentLeg = LegState(legId, mode, nowMs)
        persistPartialSession()
    }

    private suspend fun closeSession(userInitiated: Boolean) {
        val session = sessionState ?: run { resetToIdle(); return }
        val nowMs   = nowMs()

        completeCurrentLeg()

        val totalDist        = session.totalDistanceKm
        val totalDurationMin = ((nowMs - session.startTimeMs) / 60_000L).toInt()

        if (totalDist < MIN_TRIP_DISTANCE_KM || totalDurationMin < MIN_TRIP_DURATION_MIN) {
            repo.deleteSession(session.sessionId)
            resetToIdle()
            return
        }

        repo.completeSession(
            id          = session.sessionId,
            endedAtMs   = nowMs,
            totalDistKm = totalDist.toDouble(),
            isConfirmed = userInitiated,
        )

        if (userInitiated) {
            TripDetectorState.emitRecentlySaved(
                RecentlySavedSession(
                    sessionId    = session.sessionId,
                    totalDistKm  = totalDist,
                    firstLegMode = session.legs.firstOrNull()?.mode,
                )
            )
            scope.launch {
                delay(5_000L)
                TripDetectorState.emitRecentlySaved(null)
            }
        } else {
            val pending = PendingSessionEntry(
                sessionId        = session.sessionId,
                legs             = session.legs.map { PendingLegEntry(it.legId, it.mode, it.distanceKm) },
                totalDistKm      = totalDist,
                totalDurationMin = totalDurationMin,
            )
            TripDetectorState.emitPendingSession(pending)
        }

        resetToIdle()
    }

    private fun resetToIdle() {
        currentPhase  = SessionPhase.Idle
        sessionState  = null
        legSortOrder  = 0
        wasAutomotive = false
        wasWalking    = false
        cancelAllJobs()
        switchToLowPower()
        clearPersistedSession()
        TripDetectorState.emitOngoingSession(null)
    }

    // ── GPS tracking ──────────────────────────────────────────────────────────

    private fun handleLocation(location: CLLocation) {
        if (location.horizontalAccuracy > 50.0) return
        val (lat, lng) = location.coordinate.useContents { latitude to longitude }
        onGpsFix(LatLng(lat, lng))
    }

    private fun onGpsFix(point: LatLng) {
        // Reset GPS-loss watchdog
        gpsLossJob?.cancel()
        gpsLossJob = scope.launch {
            delay(GPS_LOSS_TIMEOUT_MS)
            val p = currentPhase
            if (p is SessionPhase.Active || p is SessionPhase.LegEnding) {
                closeSession(userInitiated = false)
            }
        }

        val leg = sessionState?.currentLeg ?: return
        val prev = leg.waypoints.lastOrNull()
        if (prev != null) leg.distanceKm += haversineKm(prev, point)
        leg.waypoints.add(point)

        persistPartialSession()
        publishOngoingSession()
    }

    // ── Ongoing session UI state ──────────────────────────────────────────────

    private fun publishOngoingSession() {
        val session = sessionState
        if (session == null || currentPhase is SessionPhase.Idle) {
            TripDetectorState.emitOngoingSession(null)
            return
        }
        val nowMs = nowMs()
        val currentLegUi = session.currentLeg?.let { leg ->
            OngoingLegUiState(
                mode       = leg.mode,
                distanceKm = leg.distanceKm,
                elapsedMin = ((nowMs - leg.startTimeMs) / 60_000L).toInt(),
            )
        }
        TripDetectorState.emitOngoingSession(
            OngoingSessionUiState(
                sessionId         = session.sessionId,
                phase             = currentPhase,
                elapsedMin        = ((nowMs - session.startTimeMs) / 60_000L).toInt(),
                totalDistKm       = session.totalDistanceKm,
                currentLeg        = currentLegUi,
                completedLegs     = session.legs.map { CompletedLegSummary(it.mode, it.distanceKm) },
                secondsUntilClose = if (currentPhase is SessionPhase.LegEnding) legEndingSecondsLeft else null,
            )
        )
    }

    // ── Location mode switching ───────────────────────────────────────────────

    private fun switchToHighAccuracy() {
        locationManager.stopMonitoringSignificantLocationChanges()
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.startUpdatingLocation()
    }

    private fun switchToLowPower() {
        locationManager.stopUpdatingLocation()
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.startMonitoringSignificantLocationChanges()
    }

    // ── Permission state ──────────────────────────────────────────────────────

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

    // ── Persistence — NSUserDefaults (no JSON needed) ─────────────────────────

    private fun persistPartialSession() {
        val session = sessionState ?: return
        val leg     = session.currentLeg
        defaults.setObject(session.sessionId,          forKey = KEY_SESSION_ID)
        defaults.setDouble(session.startTimeMs.toDouble(), forKey = KEY_SESSION_START_MS)
        if (leg != null) {
            defaults.setObject(leg.legId,              forKey = KEY_LEG_ID)
            defaults.setObject(leg.mode.name,          forKey = KEY_LEG_MODE)
            defaults.setDouble(leg.startTimeMs.toDouble(), forKey = KEY_LEG_START_MS)
            defaults.setDouble(leg.distanceKm.toDouble(), forKey = KEY_LEG_DIST_KM)
            defaults.setInteger(legSortOrder.toLong(), forKey = KEY_LEG_SORT_ORDER)
        } else {
            clearLegDefaults()
        }
        defaults.synchronize()
    }

    private fun restorePartialSession() {
        val sessionId = defaults.stringForKey(KEY_SESSION_ID) ?: return
        val startMs   = defaults.doubleForKey(KEY_SESSION_START_MS).toLong()
        val legId     = defaults.stringForKey(KEY_LEG_ID)
        val modeStr   = defaults.stringForKey(KEY_LEG_MODE)
        val legStartMs = defaults.doubleForKey(KEY_LEG_START_MS).toLong()
        val distKm    = defaults.doubleForKey(KEY_LEG_DIST_KM).toFloat()
        val sortOrder = defaults.integerForKey(KEY_LEG_SORT_ORDER).toInt()

        try {
            val mode = modeStr?.let { TransportModeType.valueOf(it) } ?: TransportModeType.DRIVING
            val leg = if (legId != null) {
                LegState(legId, mode, legStartMs, mutableListOf(), distKm)
            } else null

            sessionState = SessionState(
                sessionId   = sessionId,
                startTimeMs = startMs,
                legs        = mutableListOf(),
                currentLeg  = leg,
            )
            legSortOrder = sortOrder
            currentPhase = SessionPhase.Active(mode)

            switchToHighAccuracy()
            publishOngoingSession()
        } catch (_: Exception) {
            clearPersistedSession()
        }
    }

    private fun clearPersistedSession() {
        defaults.removeObjectForKey(KEY_SESSION_ID)
        defaults.removeObjectForKey(KEY_SESSION_START_MS)
        clearLegDefaults()
        defaults.synchronize()
    }

    private fun clearLegDefaults() {
        defaults.removeObjectForKey(KEY_LEG_ID)
        defaults.removeObjectForKey(KEY_LEG_MODE)
        defaults.removeObjectForKey(KEY_LEG_START_MS)
        defaults.removeObjectForKey(KEY_LEG_DIST_KM)
        defaults.removeObjectForKey(KEY_LEG_SORT_ORDER)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun cancelAllJobs() {
        debounceJob?.cancel()
        legEndingJob?.cancel()
        stillTimeoutJob?.cancel()
        gpsLossJob?.cancel()
    }

    companion object {
        private const val KEY_SESSION_ID       = "atmos_session_id"
        private const val KEY_SESSION_START_MS = "atmos_session_start_ms"
        private const val KEY_LEG_ID           = "atmos_leg_id"
        private const val KEY_LEG_MODE         = "atmos_leg_mode"
        private const val KEY_LEG_START_MS     = "atmos_leg_start_ms"
        private const val KEY_LEG_DIST_KM      = "atmos_leg_dist_km"
        private const val KEY_LEG_SORT_ORDER   = "atmos_leg_sort_order"
    }
}
