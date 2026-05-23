package dev.atmos.shared.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * SQLDelight-backed implementation of [TripRepository].
 *
 * Construct once (e.g. alongside [DatabaseProvider.init]) and share as a singleton.
 * All suspend functions dispatch to [Dispatchers.IO]; the Flow also runs on IO.
 */
class TripRepositoryImpl(private val database: AtmosDatabase) : TripRepository {

    // ── Session operations ────────────────────────────────────────────────────

    override suspend fun startSession(id: String, startedAtMs: Long) =
        withContext(Dispatchers.IO) {
            database.sessionsQueries.insert(
                id            = id,
                started_at_ms = startedAtMs,
                ended_at_ms   = null,
                total_dist_km = 0.0,
                is_confirmed  = 0L,
            )
        }

    override suspend fun completeSession(
        id: String,
        endedAtMs: Long,
        totalDistKm: Double,
        isConfirmed: Boolean,
    ) = withContext(Dispatchers.IO) {
        database.transaction {
            database.sessionsQueries.updateEndTime(ended_at_ms = endedAtMs, id = id)
            database.sessionsQueries.updateTotalDist(total_dist_km = totalDistKm, id = id)
            if (isConfirmed) database.sessionsQueries.updateConfirmed(id = id)
        }
    }

    override suspend fun confirmSession(id: String) = withContext(Dispatchers.IO) {
        database.sessionsQueries.updateConfirmed(id = id)
    }

    override suspend fun deleteSession(id: String) = withContext(Dispatchers.IO) {
        // ON DELETE CASCADE in the schema removes all legs automatically
        database.sessionsQueries.delete(id = id)
    }

    override suspend fun getPendingSessions(): List<SessionWithLegs> =
        withContext(Dispatchers.IO) {
            database.sessionsQueries
                .getPending()
                .executeAsList()
                .map { session ->
                    val legs = database.legsQueries
                        .getForSession(session.id)
                        .executeAsList()
                    SessionWithLegs(session, legs)
                }
        }

    override fun observeConfirmedSessions(): Flow<List<SessionWithLegs>> =
        database.sessionsQueries
            .getConfirmed(limit = Long.MAX_VALUE)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sessions ->
                sessions.map { session ->
                    val legs = database.legsQueries
                        .getForSession(session.id)
                        .executeAsList()
                    SessionWithLegs(session, legs)
                }
            }
            .flowOn(Dispatchers.IO)

    // ── Leg operations ────────────────────────────────────────────────────────

    override suspend fun addLeg(
        id: String,
        sessionId: String,
        mode: String,
        startedAtMs: Long,
        sortOrder: Long,
    ) = withContext(Dispatchers.IO) {
        database.legsQueries.insert(
            id             = id,
            session_id     = sessionId,
            mode           = mode,
            started_at_ms  = startedAtMs,
            ended_at_ms    = null,
            distance_km    = 0.0,
            waypoints_json = "[]",
            sort_order     = sortOrder,
        )
    }

    override suspend fun completeLeg(
        id: String,
        endedAtMs: Long,
        distanceKm: Double,
        waypointsJson: String,
    ) = withContext(Dispatchers.IO) {
        database.legsQueries.updateEnd(
            ended_at_ms    = endedAtMs,
            distance_km    = distanceKm,
            waypoints_json = waypointsJson,
            id             = id,
        )
    }
}
