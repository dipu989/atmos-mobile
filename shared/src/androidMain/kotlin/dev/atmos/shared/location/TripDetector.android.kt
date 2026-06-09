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
import dev.atmos.shared.db.DatabaseDriverFactory
import dev.atmos.shared.db.DatabaseProvider
import dev.atmos.shared.db.TripRepository
import dev.atmos.shared.db.TripRepositoryImpl
import dev.atmos.shared.ui.home.TransportModeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

// ── Context + repository holder ───────────────────────────────────────────────

/**
 * Stores the application context and vends a singleton [AndroidTripDetector].
 * Call [init] from MainActivity.onCreate() before setContent {}.
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
        // Ensure DB is initialised — needed when woken by BroadcastReceiver (app process dead)
        DatabaseProvider.init(DatabaseDriverFactory(ctx))
        val repo = TripRepositoryImpl(DatabaseProvider.database)
        return instance ?: AndroidTripDetector(ctx, repo).also { instance = it }
    }
}

// ── actual factory ────────────────────────────────────────────────────────────

actual fun createTripDetector(): TripDetector = TripDetectorHolder.get()

// ── Android implementation ────────────────────────────────────────────────────

class AndroidTripDetector(
    private val context: Context,
    private val repo: TripRepository,
) : TripDetector {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val prefs by lazy {
        context.getSharedPreferences("atmos_session", Context.MODE_PRIVATE)
    }

    // ── State ─────────────────────────────────────────────────────────────────

    @Volatile private var currentPhase: SessionPhase = SessionPhase.Idle
    private var sessionState: SessionState? = null
    private var legSortOrder = 0
    private var debounceStartMs = 0L
    private var legEndingSecondsLeft = 0

    // ── Coroutine jobs ────────────────────────────────────────────────────────

    private var debounceJob: Job? = null      // SessionStarting → Active after 30 s
    private var legEndingJob: Job? = null     // LegEnding countdown → close
    private var stillTimeoutJob: Job? = null  // STILL_ENTERED → close after 20 min
    private var gpsLossJob: Job? = null       // GPS lost → close after 10 min

    // ── Activity Transition API ───────────────────────────────────────────────

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
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
        )
    )

    // ── Location callback ─────────────────────────────────────────────────────

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            if (loc.accuracy > 50f) return   // discard low-accuracy fixes
            onGpsFix(LatLng(loc.latitude, loc.longitude))
        }
    }

    // ── TripDetector interface ────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    override fun startMonitoring() {
        ActivityRecognition.getClient(context)
            .requestActivityTransitionUpdates(transitionRequest, transitionPendingIntent)
        restorePartialSession()
    }

    @SuppressLint("MissingPermission")
    override fun stopMonitoring() {
        ActivityRecognition.getClient(context)
            .removeActivityTransitionUpdates(transitionPendingIntent)
        cancelAllJobs()
        stopLocationUpdates()
        currentPhase = SessionPhase.Idle
    }

    override fun requestPermissions() = Unit  // Handled by AndroidPermissionRequester in MainActivity

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
                updateServiceNotification()
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

    // ── State transitions ─────────────────────────────────────────────────────

    private suspend fun onVehicleEntered() {
        when (val phase = currentPhase) {
            is SessionPhase.Idle -> {
                currentPhase = SessionPhase.SessionStarting
                debounceStartMs = System.currentTimeMillis()
                startLocationUpdates()
                startForegroundService()
                debounceJob = scope.launch {
                    delay(TRIP_STARTING_DEBOUNCE_MS)
                    if (currentPhase is SessionPhase.SessionStarting) {
                        confirmSessionStart(TransportModeType.DRIVING)
                    }
                }
            }
            is SessionPhase.LegEnding -> {
                // User re-entered vehicle (petrol stop, quick errand)
                legEndingJob?.cancel()
                stillTimeoutJob?.cancel()
                if (phase.previousMode != TransportModeType.DRIVING) {
                    // Was walking → complete walking leg, start driving leg
                    completeCurrentLeg()
                    startNewLeg(TransportModeType.DRIVING)
                }
                // If previously driving, resume the existing driving leg
                currentPhase = SessionPhase.Active(TransportModeType.DRIVING)
                updateServiceNotification()
                publishOngoingSession()
            }
            is SessionPhase.Active -> {
                if (phase.currentMode == TransportModeType.WALKING) {
                    // Mode switch within active session: walking → driving
                    completeCurrentLeg()
                    startNewLeg(TransportModeType.DRIVING)
                    currentPhase = SessionPhase.Active(TransportModeType.DRIVING)
                    updateServiceNotification()
                    publishOngoingSession()
                }
                // Already in IN_VEHICLE — ignore
            }
            else -> Unit
        }
    }

    private suspend fun onVehicleExited() {
        when (val phase = currentPhase) {
            is SessionPhase.SessionStarting -> {
                // Exited before debounce confirmed — false start, discard
                debounceJob?.cancel()
                stopLocationUpdates()
                stopForegroundService()
                currentPhase = SessionPhase.Idle
                publishOngoingSession()
            }
            is SessionPhase.Active -> {
                // Start 60 s grace — user might re-enter (petrol stop, short errand)
                currentPhase = SessionPhase.LegEnding(phase.currentMode)
                updateServiceNotification()   // switch notification to "Trip ending…"
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
                // Confirmed end of driving leg — start walking leg
                legEndingJob?.cancel()
                stillTimeoutJob?.cancel()
                completeCurrentLeg()
                startNewLeg(TransportModeType.WALKING)
                currentPhase = SessionPhase.Active(TransportModeType.WALKING)
                updateServiceNotification()
                publishOngoingSession()
            }
            is SessionPhase.Active -> {
                if (phase.currentMode == TransportModeType.DRIVING) {
                    // Missed EXIT event — treat walking as confirmation leg ended
                    completeCurrentLeg()
                    startNewLeg(TransportModeType.WALKING)
                    currentPhase = SessionPhase.Active(TransportModeType.WALKING)
                    updateServiceNotification()
                    publishOngoingSession()
                }
            }
            else -> Unit
        }
    }

    private fun onStillEntered() {
        when (currentPhase) {
            is SessionPhase.Active, is SessionPhase.LegEnding -> {
                // Cancel ongoing grace/debounce; start 20-min stationary timeout
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
        val sessionId = UUID.randomUUID().toString()
        val legId = UUID.randomUUID().toString()
        val nowMs = System.currentTimeMillis()

        repo.startSession(sessionId, nowMs)
        repo.addLeg(legId, sessionId, mode.name, nowMs, 0L)

        sessionState = SessionState(
            sessionId  = sessionId,
            startTimeMs = nowMs,
            legs       = mutableListOf(),
            currentLeg = LegState(legId, mode, nowMs),
        )
        legSortOrder = 0

        currentPhase = SessionPhase.Active(mode)
        persistPartialSession()
        updateServiceNotification()
        publishOngoingSession()
    }

    private suspend fun completeCurrentLeg() {
        val session = sessionState ?: return
        val leg = session.currentLeg ?: return
        val nowMs = System.currentTimeMillis()

        repo.completeLeg(
            id             = leg.legId,
            endedAtMs      = nowMs,
            distanceKm     = leg.distanceKm.toDouble(),
            waypointsJson  = serializeWaypoints(leg.waypoints),
        )
        session.legs.add(leg)
        session.currentLeg = null
    }

    private suspend fun startNewLeg(mode: TransportModeType) {
        val session = sessionState ?: return
        val legId = UUID.randomUUID().toString()
        val nowMs = System.currentTimeMillis()
        legSortOrder++

        repo.addLeg(legId, session.sessionId, mode.name, nowMs, legSortOrder.toLong())
        session.currentLeg = LegState(legId, mode, nowMs)
        persistPartialSession()
    }

    private suspend fun closeSession(userInitiated: Boolean) {
        val session = sessionState ?: run { resetToIdle(); return }
        val nowMs = System.currentTimeMillis()

        // Finalise current leg if still active
        completeCurrentLeg()

        val totalDist = session.totalDistanceKm
        val totalDurationMin = ((nowMs - session.startTimeMs) / 60_000L).toInt()

        // Discard noise sessions (very short or too brief)
        if (totalDist < MIN_TRIP_DISTANCE_KM || totalDurationMin < MIN_TRIP_DURATION_MIN) {
            repo.deleteSession(session.sessionId)
            resetToIdle()
            return
        }

        repo.completeSession(
            id           = session.sessionId,
            endedAtMs    = nowMs,
            totalDistKm  = totalDist.toDouble(),
            isConfirmed  = userInitiated,
        )

        if (userInitiated) {
            // User hit "End Trip" — save immediately, show snackbar in UI
            TripDetectorState.emitRecentlySaved(
                RecentlySavedSession(
                    sessionId    = session.sessionId,
                    totalDistKm  = totalDist,
                    firstLegMode = session.legs.firstOrNull()?.mode,
                    startedAtMs  = session.startTimeMs,
                )
            )
            scope.launch {
                delay(5_000L)
                TripDetectorState.emitRecentlySaved(null)
            }
            postSavedNotification(totalDist)
        } else {
            // System auto-end — show PendingTripCard for confirmation
            val pending = PendingSessionEntry(
                sessionId       = session.sessionId,
                legs            = session.legs.map { PendingLegEntry(it.legId, it.mode, it.distanceKm) },
                totalDistKm     = totalDist,
                totalDurationMin = totalDurationMin,
            )
            TripDetectorState.emitPendingSession(pending)
            postTripReadyNotification()
        }

        resetToIdle()
    }

    private fun resetToIdle() {
        currentPhase = SessionPhase.Idle
        sessionState = null
        legSortOrder = 0
        cancelAllJobs()
        stopLocationUpdates()
        stopForegroundService()
        clearPersistedSession()
        TripDetectorState.emitOngoingSession(null)
    }

    // ── GPS tracking ──────────────────────────────────────────────────────────

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
        val nowMs = System.currentTimeMillis()
        val currentLegUi = session.currentLeg?.let { leg ->
            OngoingLegUiState(
                mode        = leg.mode,
                distanceKm  = leg.distanceKm,
                elapsedMin  = ((nowMs - leg.startTimeMs) / 60_000L).toInt(),
            )
        }
        val completedLegs = session.legs.map { leg ->
            CompletedLegSummary(mode = leg.mode, distanceKm = leg.distanceKm)
        }
        TripDetectorState.emitOngoingSession(
            OngoingSessionUiState(
                sessionId        = session.sessionId,
                phase            = currentPhase,
                elapsedMin       = ((nowMs - session.startTimeMs) / 60_000L).toInt(),
                totalDistKm      = session.totalDistanceKm,
                currentLeg       = currentLegUi,
                completedLegs    = completedLegs,
                secondsUntilClose = if (currentPhase is SessionPhase.LegEnding) legEndingSecondsLeft else null,
            )
        )
    }

    // ── Location updates ──────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
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
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return
        try {
            val intent = serviceIntent(TripDetectionServiceConsts.ACTION_START)
            context.startForegroundService(intent)
        } catch (_: Exception) { }
    }

    private fun stopForegroundService() {
        try {
            context.startService(serviceIntent(TripDetectionServiceConsts.ACTION_STOP))
        } catch (_: Exception) { }
    }

    private fun updateServiceNotification() {
        val phase = currentPhase
        val mode = when (phase) {
            is SessionPhase.Active    -> phase.currentMode
            is SessionPhase.LegEnding -> phase.previousMode
            else -> return
        }
        val phaseStr = if (phase is SessionPhase.LegEnding)
            TripDetectionServiceConsts.PHASE_LEG_ENDING
        else
            TripDetectionServiceConsts.PHASE_ACTIVE
        try {
            context.startService(
                serviceIntent(TripDetectionServiceConsts.ACTION_UPDATE)
                    .putExtra(TripDetectionServiceConsts.EXTRA_MODE, mode.name)
                    .putExtra(TripDetectionServiceConsts.EXTRA_DIST_KM,
                        sessionState?.totalDistanceKm ?: 0f)
                    .putExtra(TripDetectionServiceConsts.EXTRA_START_TIME_MS,
                        sessionState?.startTimeMs ?: System.currentTimeMillis())
                    .putExtra(TripDetectionServiceConsts.EXTRA_PHASE, phaseStr)
            )
        } catch (_: Exception) { }
    }

    private fun postTripReadyNotification() {
        try {
            context.startService(serviceIntent(TripDetectionServiceConsts.ACTION_TRIP_DETECTED))
        } catch (_: Exception) { }
    }

    private fun postSavedNotification(totalDistKm: Float) {
        try {
            context.startService(
                serviceIntent(TripDetectionServiceConsts.ACTION_TRIP_SAVED)
                    .putExtra(TripDetectionServiceConsts.EXTRA_DIST_KM, totalDistKm)
            )
        } catch (_: Exception) { }
    }

    private fun serviceIntent(action: String): Intent =
        Intent(context, Class.forName("dev.atmos.android.TripDetectionService"))
            .putExtra(TripDetectionServiceConsts.EXTRA_ACTION, action)

    // ── Persistence (survive process death) ───────────────────────────────────

    private fun persistPartialSession() {
        val session = sessionState ?: return
        val leg = session.currentLeg
        val legJson = leg?.let {
            Json.encodeToString(
                LegStateDto(
                    legId       = it.legId,
                    mode        = it.mode.name,
                    startTimeMs = it.startTimeMs,
                    distanceKm  = it.distanceKm,
                    sortOrder   = legSortOrder,
                    waypoints   = it.waypoints.map { p -> WaypointDto(p.lat, p.lng) },
                )
            )
        }
        prefs.edit()
            .putString(PREF_SESSION_ID, session.sessionId)
            .putLong(PREF_SESSION_START_MS, session.startTimeMs)
            .putString(PREF_LEG_DTO, legJson)
            .apply()
    }

    private fun restorePartialSession() {
        val sessionId = prefs.getString(PREF_SESSION_ID, null) ?: return
        val startMs   = prefs.getLong(PREF_SESSION_START_MS, 0L)
        val legJson   = prefs.getString(PREF_LEG_DTO, null)

        try {
            val leg = legJson?.let {
                val dto = Json.decodeFromString<LegStateDto>(it)
                LegState(
                    legId       = dto.legId,
                    mode        = TransportModeType.valueOf(dto.mode),
                    startTimeMs = dto.startTimeMs,
                    waypoints   = dto.waypoints.map { p -> LatLng(p.lat, p.lng) }.toMutableList(),
                    distanceKm  = dto.distanceKm,
                )
            }
            sessionState = SessionState(
                sessionId   = sessionId,
                startTimeMs = startMs,
                legs        = mutableListOf(),
                currentLeg  = leg,
            )
            val mode = leg?.mode ?: TransportModeType.DRIVING
            currentPhase = SessionPhase.Active(mode)
            legSortOrder = leg?.let { Json.decodeFromString<LegStateDto>(legJson!!).sortOrder } ?: 0

            startLocationUpdates()
            startForegroundService()
            publishOngoingSession()
        } catch (_: Exception) {
            clearPersistedSession()
        }
    }

    private fun clearPersistedSession() {
        prefs.edit()
            .remove(PREF_SESSION_ID)
            .remove(PREF_SESSION_START_MS)
            .remove(PREF_LEG_DTO)
            .apply()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun cancelAllJobs() {
        debounceJob?.cancel()
        legEndingJob?.cancel()
        stillTimeoutJob?.cancel()
        gpsLossJob?.cancel()
    }

    // ── Companion (static access from BroadcastReceiver) ─────────────────────

    companion object {
        private const val TRANSITION_REQUEST_CODE = 1001
        private const val PREF_SESSION_ID         = "atmos_session_id"
        private const val PREF_SESSION_START_MS   = "atmos_session_start_ms"
        private const val PREF_LEG_DTO            = "atmos_leg_dto"

        /** Called from [TripTransitionReceiver] — app process may have been dead. */
        fun handleTransition(context: Context, event: VehicleActivity) {
            TripDetectorHolder.init(context)
            TripDetectorHolder.get().handleTransition(event)
        }
    }
}

// ── Serialization DTOs ────────────────────────────────────────────────────────

@Serializable
data class WaypointDto(val lat: Double, val lng: Double)

@Serializable
data class LegStateDto(
    val legId: String,
    val mode: String,
    val startTimeMs: Long,
    val distanceKm: Float,
    val sortOrder: Int,
    val waypoints: List<WaypointDto>,
)

// ── Service action constants ──────────────────────────────────────────────────

/**
 * String constants shared between [AndroidTripDetector] and [TripDetectionService].
 * Defined here (shared module) to avoid a circular dependency on the androidApp module.
 */
object TripDetectionServiceConsts {
    const val EXTRA_ACTION        = "action"
    const val EXTRA_MODE          = "mode"
    const val EXTRA_DIST_KM       = "dist_km"
    const val EXTRA_START_TIME_MS = "start_time_ms"
    const val EXTRA_PHASE         = "phase"

    const val ACTION_START         = "start"
    const val ACTION_STOP          = "stop"
    const val ACTION_UPDATE        = "update"
    const val ACTION_TRIP_DETECTED = "trip_detected"
    const val ACTION_TRIP_SAVED    = "trip_saved"

    /** Broadcast action fired by TripActionReceiver when the user taps "End Trip" in the notification. */
    const val ACTION_END_TRIP = "dev.atmos.android.ACTION_END_TRIP"
    /** Broadcast action fired by TripActionReceiver when the user taps "Discard" in the notification. */
    const val ACTION_DISCARD  = "dev.atmos.android.ACTION_DISCARD"

    const val PHASE_ACTIVE     = "active"
    const val PHASE_LEG_ENDING = "leg_ending"
}

private fun serializeWaypoints(waypoints: List<LatLng>): String =
    Json.encodeToString(waypoints.map { WaypointDto(it.lat, it.lng) })
