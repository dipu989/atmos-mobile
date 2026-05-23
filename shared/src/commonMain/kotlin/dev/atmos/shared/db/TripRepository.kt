package dev.atmos.shared.db

import kotlinx.coroutines.flow.Flow

/**
 * A session row paired with all its leg rows, as read from the database.
 */
data class SessionWithLegs(
    val session: Sessions,
    val legs: List<Legs>,
)

/**
 * All persistence operations for trip sessions and their legs.
 *
 * Use [TripRepositoryImpl] as the concrete implementation, constructed with
 * [DatabaseProvider.database] after [DatabaseProvider.init] has been called.
 */
interface TripRepository {

    // ── Session operations ────────────────────────────────────────────────────

    /** Insert a new session row in the pending / in-progress state. */
    suspend fun startSession(id: String, startedAtMs: Long)

    /**
     * Finalise a session: record end time, total distance, and optionally confirm it.
     *
     * - [isConfirmed] = `true`  → user-initiated "End Trip"; saved immediately, no PendingTripCard.
     * - [isConfirmed] = `false` → system auto-end; stays pending until user confirms or dismisses.
     */
    suspend fun completeSession(
        id: String,
        endedAtMs: Long,
        totalDistKm: Double,
        isConfirmed: Boolean,
    )

    /** Mark an already-ended pending session as confirmed (user tapped "Confirm" on PendingTripCard). */
    suspend fun confirmSession(id: String)

    /** Hard-delete a session. Legs are removed automatically via ON DELETE CASCADE. */
    suspend fun deleteSession(id: String)

    /** All sessions that have ended but not yet been confirmed. */
    suspend fun getPendingSessions(): List<SessionWithLegs>

    /**
     * Live stream of all confirmed sessions, newest first.
     * Re-emits every time any session or leg row changes.
     */
    fun observeConfirmedSessions(): Flow<List<SessionWithLegs>>

    // ── Leg operations ────────────────────────────────────────────────────────

    /**
     * Insert a new leg when it starts.
     * [mode] is [TransportModeType.name] stored as plain TEXT.
     * [sortOrder] is the leg's 0-based position within the session.
     */
    suspend fun addLeg(
        id: String,
        sessionId: String,
        mode: String,
        startedAtMs: Long,
        sortOrder: Long,
    )

    /** Finalise a leg: set end time, accumulated distance, and serialised waypoints JSON. */
    suspend fun completeLeg(
        id: String,
        endedAtMs: Long,
        distanceKm: Double,
        waypointsJson: String,
    )
}
