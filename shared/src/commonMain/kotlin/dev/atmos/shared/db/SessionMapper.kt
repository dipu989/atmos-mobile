package dev.atmos.shared.db

import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.home.emissionFactor
import dev.atmos.shared.util.formatDateGroupLabel
import dev.atmos.shared.util.formatTimestamp

/**
 * Maps a [SessionWithLegs] row (as read from the local SQLDelight DB) to the
 * [RecentActivityEntry] shape expected by the UI layer.
 *
 * Field mapping notes:
 * - [RecentActivityEntry.origin] — On-device detection does not resolve street addresses.
 *   We synthesise a human-readable label from the leg modes instead (e.g. "Driving",
 *   "Driving + Walking"). ActivityRow handles an empty destination gracefully.
 * - [RecentActivityEntry.kgCO2] — Calculated on-device using India-region DEFRA 2023
 *   factors (kg CO₂e per km) summed across all legs. This is a local estimate; the
 *   backend may refine it once the session is POSTed to /activities.
 * - [RecentActivityEntry.isAutoDetected] — Always true for DB-sourced sessions.
 */
fun SessionWithLegs.toRecentActivityEntry(): RecentActivityEntry {
    val sortedLegs  = legs.sortedBy { it.sort_order }
    val primaryMode = sortedLegs.firstOrNull()
        ?.mode
        ?.let { runCatching { TransportModeType.valueOf(it) }.getOrNull() }
        ?: TransportModeType.DRIVING

    // "Driving", "Driving + Walking", "Driving + Walking + Metro" (cap at 3)
    val modeLabel = if (sortedLegs.size <= 1) {
        primaryMode.displayLabel
    } else {
        sortedLegs
            .take(3)
            .mapNotNull { leg -> runCatching { TransportModeType.valueOf(leg.mode) }.getOrNull() }
            .joinToString(" + ") { it.displayLabel }
    }

    val durationMin = session.ended_at_ms
        ?.let { end -> ((end - session.started_at_ms) / 60_000L).toInt().coerceAtLeast(0) }
        ?: 0

    // Sum kg CO₂e across all legs: distance_km × mode emission factor (DEFRA IN 2023)
    val kgCO2 = sortedLegs.sumOf { leg ->
        val mode = runCatching { TransportModeType.valueOf(leg.mode) }.getOrNull()
            ?: TransportModeType.DRIVING
        (leg.distance_km * mode.emissionFactor).toDouble()
    }.toFloat()

    return RecentActivityEntry(
        mode           = primaryMode,
        origin         = modeLabel,
        destination    = "",                 // no address on-device
        timeLabel      = formatTimestamp(session.started_at_ms),
        dateLabel      = formatDateGroupLabel(session.started_at_ms),
        distanceKm     = session.total_dist_km.toFloat(),
        durationMin    = durationMin,
        kgCO2          = kgCO2,
        isAutoDetected = true,
    )
}

/**
 * Groups a flat list of [RecentActivityEntry] items by their [RecentActivityEntry.dateLabel],
 * preserving the order they arrive in (newest first from the DB query).
 *
 * Returns a list of (dateLabel → entries) pairs in insertion order.
 */
fun List<RecentActivityEntry>.groupByDateLabel(): List<Pair<String, List<RecentActivityEntry>>> {
    val grouped = LinkedHashMap<String, MutableList<RecentActivityEntry>>()
    for (entry in this) {
        grouped.getOrPut(entry.dateLabel) { mutableListOf() }.add(entry)
    }
    return grouped.entries.map { it.key to it.value }
}

// ── Local helper — mirrors PendingTripCard / OngoingTripCard extensions ───────

private val TransportModeType.displayLabel: String
    get() = when (this) {
        TransportModeType.DRIVING        -> "Driving"
        TransportModeType.WALKING        -> "Walking"
        TransportModeType.CAB            -> "Cab"
        TransportModeType.TWO_WHEELER    -> "Two-wheeler"
        TransportModeType.AUTO_RICKSHAW  -> "Auto"
        TransportModeType.PUBLIC_TRANSIT -> "Transit"
        TransportModeType.BUS            -> "Bus"
        TransportModeType.METRO          -> "Metro"
        TransportModeType.TRAIN          -> "Train"
        TransportModeType.CYCLING        -> "Cycling"
        TransportModeType.FLIGHT         -> "Flight"
    }
