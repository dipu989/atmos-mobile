package dev.atmos.shared.ui.stats

import dev.atmos.shared.ui.home.TransportModeEntry
import dev.atmos.shared.ui.home.TransportModeType

enum class StatsPeriod { DAY, WEEK, MONTH }

data class StatsSummary(
    val totalKgCo2: Float,
    val totalDistKm: Float,
    val activityCount: Int,
    val breakdown: List<TransportModeEntry>,
    val trendDirection: String,   // "up" | "down" | "flat"
    val trendPct: Float?,         // null when no previous data
    val prevTotalKgCo2: Float,
)

data class StatsBarPoint(
    val label: String,
    val kgCo2: Float,
    val isCurrent: Boolean = false,
)

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    val periodLabel: String = "",
    val summary: StatsSummary? = null,
    val barPoints: List<StatsBarPoint> = emptyList(),
    val canGoPrev: Boolean = true,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
)

// ── Preview data ──────────────────────────────────────────────────────────────

val previewStatsUiState = StatsUiState(
    period      = StatsPeriod.WEEK,
    periodLabel = "Jun 9 – Jun 15",
    summary = StatsSummary(
        totalKgCo2    = 18.4f,
        totalDistKm   = 89.3f,
        activityCount = 12,
        breakdown = listOf(
            TransportModeEntry(TransportModeType.DRIVING,        "Driving",        45.2f, 9.5f, 51),
            TransportModeEntry(TransportModeType.PUBLIC_TRANSIT, "Public Transit", 28.6f, 2.6f, 32),
            TransportModeEntry(TransportModeType.WALKING,        "Walking",        15.5f, 0.0f, 17),
        ),
        trendDirection = "down",
        trendPct       = -12f,
        prevTotalKgCo2 = 20.9f,
    ),
    barPoints = listOf(
        StatsBarPoint("Mon", 2.1f),
        StatsBarPoint("Tue", 4.8f),
        StatsBarPoint("Wed", 1.5f),
        StatsBarPoint("Thu", 3.7f),
        StatsBarPoint("Fri", 3.4f),
        StatsBarPoint("Sat", 1.2f),
        StatsBarPoint("Sun", 1.7f, isCurrent = true),
    ),
    canGoPrev = true,
    isLoading = false,
)
