package dev.atmos.shared.ui.home

import dev.atmos.shared.location.OngoingSessionUiState
import dev.atmos.shared.location.PendingLegEntry
import dev.atmos.shared.location.PendingSessionEntry

// ── Data models ───────────────────────────────────────────────────────────────

data class UserProfile(
    val displayName: String,
    val initials: String,
    val avatarUrl: String = "",
)

data class TodayImpact(
    val kgCO2: Float,
    val dailyGoalKgCO2: Float,
    val percentVsWeeklyAvg: Int,   // negative = below avg (good)
)

data class WeeklyDataPoint(
    val dayLabel: String,
    val kgCO2: Float,
    val isToday: Boolean = false,
)

/** Bucket granularity for the Home screen's trend card — DAILY buckets by day (last 7 days),
 *  WEEKLY buckets by week (last 6 weeks), FORTNIGHTLY buckets by 2-week span (last 6 fortnights). */
enum class HomeTrendPeriod { DAILY, WEEKLY, FORTNIGHTLY }

enum class TransportModeType {
    DRIVING, PUBLIC_TRANSIT, CYCLING, WALKING,
    CAB, METRO, TRAIN, BUS, AUTO_RICKSHAW, TWO_WHEELER, FLIGHT;

    val isZeroEmission: Boolean
        get() = this == CYCLING || this == WALKING

    val isHighEmission: Boolean
        get() = this == DRIVING || this == CAB || this == FLIGHT
}

/**
 * India-region DEFRA 2023 emission factors (kg CO₂e per km).
 * Canonical definition — import from here instead of duplicating.
 * Used by LogActivitySheet (estimate card), SessionMapper (per-leg CO₂), and the dashboard.
 */
val TransportModeType.emissionFactor: Float
    get() = when (this) {
        TransportModeType.DRIVING        -> 0.21f
        TransportModeType.CAB            -> 0.21f
        TransportModeType.TWO_WHEELER    -> 0.11f
        TransportModeType.AUTO_RICKSHAW  -> 0.10f
        TransportModeType.BUS,
        TransportModeType.PUBLIC_TRANSIT -> 0.09f
        TransportModeType.TRAIN,
        TransportModeType.METRO          -> 0.04f
        TransportModeType.FLIGHT         -> 0.26f
        TransportModeType.CYCLING,
        TransportModeType.WALKING        -> 0.00f
    }

data class TransportModeEntry(
    val mode: TransportModeType,
    val displayName: String,
    val distanceKm: Float,
    val kgCO2: Float,
    val percentage: Int,
)

enum class InsightType {
    STREAK, MILESTONE, TIP, COMPARISON, ANOMALY
}

data class InsightEntry(
    val id: String = "",
    val type: InsightType,
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val streakCount: Int = 0,       // STREAK — days/weeks in streak
    val goalProgressPct: Int = 0,   // MILESTONE — 0-100
    val savingsPct: Int = 0,        // TIP — % CO₂ reduction if followed
    val comparisonPct: Int = 0,     // COMPARISON — % vs baseline (negative = below avg)
)

data class RecentActivityEntry(
    val mode: TransportModeType,
    val origin: String,
    val destination: String,
    val timeLabel: String,
    val dateLabel: String = "Today",
    val distanceKm: Float = 0f,
    val durationMin: Int,
    val kgCO2: Float,
    val isAutoDetected: Boolean = true,   // false = manually logged
    /** DB session ID — empty string for hardcoded preview entries. */
    val sessionId: String = "",
    /** Original epoch-ms start time — preserved when editing so the trip stays in the correct date group. */
    val timestampMs: Long = 0L,
    /** Backend source value (e.g. "gps", "gps+receipt", "uber", "manual"). */
    val source: String = "",
    /** GPS coordinates stored by the backend — used to pre-fill PlaceAutocompleteField when editing. */
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
    // ── Server-computed impact context (GET /activities/:id only) ─────────────
    // Null until TripDetailScreen's enrichment fetch resolves. Never computed
    // client-side — see CLAUDE.md: CO₂-derived values come from the backend.
    val treesNeededToOffset: Int? = null,
    val ledHoursEquivalent: Int? = null,
    val globalAveragePct: Int? = null,
    val impactApproximate: Boolean = false,
    val alternativeMode: TransportModeType? = null,
    val alternativeKgCO2: Float? = null,
    val savingsKgCO2: Float? = null,
    val savingsPct: Int? = null,
)

// ── Pending trip (auto-detected, awaiting user confirmation) ──────────────────

data class PendingTripEntry(
    val mode: TransportModeType,
    val origin: String,
    val destination: String,
    val distanceKm: Float,
    val durationMin: Int,
    val estimatedKgCO2: Float,
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
)

data class HomeUiState(
    val greeting: String,
    val dateLabel: String,
    val user: UserProfile,
    val todayImpact: TodayImpact,
    val weeklyTrend: List<WeeklyDataPoint>,
    val trendPeriod: HomeTrendPeriod = HomeTrendPeriod.DAILY,
    val transportBreakdown: List<TransportModeEntry>,
    val homeLat: Double? = null,
    val homeLng: Double? = null,
    val workLat: Double? = null,
    val workLng: Double? = null,
    val recentActivity: List<RecentActivityEntry>,
    val insights: List<InsightEntry>,
    val unreadInsightsCount: Int,
    val pendingSession: PendingSessionEntry? = null,
    val ongoingSession: OngoingSessionUiState? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
)

// ── Mock data ─────────────────────────────────────────────────────────────────

val previewHomeUiState = HomeUiState(
    greeting  = "Good morning",
    dateLabel = "Sunday, May 10",
    user      = UserProfile(displayName = "Shantnu Kumar", initials = "SK"),
    todayImpact = TodayImpact(
        kgCO2            = 3.4f,
        dailyGoalKgCO2   = 5.0f,
        percentVsWeeklyAvg = -12,
    ),
    weeklyTrend = listOf(
        WeeklyDataPoint("Mon", 3.5f),
        WeeklyDataPoint("Tue", 6.2f),
        WeeklyDataPoint("Wed", 3.0f),
        WeeklyDataPoint("Thu", 5.8f),
        WeeklyDataPoint("Fri", 5.1f),
        WeeklyDataPoint("Sat", 1.9f),
        WeeklyDataPoint("Sun", 3.4f, isToday = true),
    ),
    transportBreakdown = listOf(
        TransportModeEntry(TransportModeType.DRIVING,        "Driving",        12.3f, 2.8f, 65),
        TransportModeEntry(TransportModeType.PUBLIC_TRANSIT, "Public Transit",  8.5f, 0.9f, 21),
        TransportModeEntry(TransportModeType.CYCLING,        "Cycling",         3.2f, 0.0f,  8),
        TransportModeEntry(TransportModeType.WALKING,        "Walking",         1.8f, 0.0f,  6),
    ),
    recentActivity = listOf(
        RecentActivityEntry(
            mode = TransportModeType.DRIVING,
            origin = "Home", destination = "Office",
            timeLabel = "8:45 AM", dateLabel = "Today",
            distanceKm = 8.6f, durationMin = 22, kgCO2 = 1.8f,
            isAutoDetected = true,
        ),
        RecentActivityEntry(
            mode = TransportModeType.WALKING,
            origin = "Office", destination = "Café",
            timeLabel = "12:30 PM", dateLabel = "Today",
            distanceKm = 0.6f, durationMin = 8, kgCO2 = 0f,
            isAutoDetected = true,
        ),
        RecentActivityEntry(
            mode = TransportModeType.BUS,
            origin = "Café", destination = "Downtown",
            timeLabel = "2:15 PM", dateLabel = "Today",
            distanceKm = 5.5f, durationMin = 15, kgCO2 = 0.5f,
            isAutoDetected = false,
        ),
    ),
    insights = listOf(
        InsightEntry(
            type        = InsightType.STREAK,
            title       = "3-Day Streak",
            body        = "You've logged your environmental impact for 3 days in a row. Consistency is key to building lasting habits.",
            streakCount = 3,
        ),
        InsightEntry(
            type       = InsightType.TIP,
            title      = "Optimize Morning Commute",
            body       = "Taking public transit for your morning commute could reduce those trip emissions by 68%. That's a big impact compounded over a month.",
            savingsPct = 68,
        ),
        InsightEntry(
            type             = InsightType.MILESTONE,
            title            = "Monthly Goal: 82% Complete",
            body             = "You're on track to meet your monthly emissions target. A few more low-emission trips and you'll hit it.",
            goalProgressPct  = 82,
        ),
    ),
    unreadInsightsCount = 3,
    pendingSession = PendingSessionEntry(
        sessionId        = "preview-session",
        legs             = listOf(
            PendingLegEntry(legId = "leg-1", mode = TransportModeType.DRIVING, distanceKm = 8.6f),
            PendingLegEntry(legId = "leg-2", mode = TransportModeType.WALKING, distanceKm = 0.6f),
        ),
        totalDistKm      = 9.2f,
        totalDurationMin = 24,
    ),
)

// ── Full activity history mock (for Activities screen) ────────────────────────

val previewAllActivities: List<Pair<String, List<RecentActivityEntry>>> = listOf(
    "Today" to listOf(
        RecentActivityEntry(TransportModeType.DRIVING, "Home", "Office",
            timeLabel = "8:45 AM", dateLabel = "Today", distanceKm = 8.6f, durationMin = 22, kgCO2 = 1.8f, isAutoDetected = true),
        RecentActivityEntry(TransportModeType.WALKING, "Office", "Café",
            timeLabel = "12:30 PM", dateLabel = "Today", distanceKm = 0.6f, durationMin = 8, kgCO2 = 0f, isAutoDetected = true),
        RecentActivityEntry(TransportModeType.BUS, "Café", "Downtown",
            timeLabel = "2:15 PM", dateLabel = "Today", distanceKm = 5.5f, durationMin = 15, kgCO2 = 0.5f, isAutoDetected = false),
    ),
    "Yesterday" to listOf(
        RecentActivityEntry(TransportModeType.METRO, "Home", "Mall",
            timeLabel = "10:00 AM", dateLabel = "Yesterday", distanceKm = 12.3f, durationMin = 28, kgCO2 = 0.8f, isAutoDetected = true),
        RecentActivityEntry(TransportModeType.DRIVING, "Mall", "Home",
            timeLabel = "6:30 PM", dateLabel = "Yesterday", distanceKm = 12.3f, durationMin = 35, kgCO2 = 2.1f, isAutoDetected = false),
    ),
    "Mon, May 13" to listOf(
        RecentActivityEntry(TransportModeType.CYCLING, "Home", "Park",
            timeLabel = "7:30 AM", dateLabel = "Mon, May 13", distanceKm = 3.2f, durationMin = 20, kgCO2 = 0f, isAutoDetected = true),
        RecentActivityEntry(TransportModeType.CAB, "Office", "Airport",
            timeLabel = "4:00 PM", dateLabel = "Mon, May 13", distanceKm = 18.5f, durationMin = 45, kgCO2 = 3.2f, isAutoDetected = false),
    ),
    "Sat, May 10" to listOf(
        RecentActivityEntry(TransportModeType.TRAIN, "City Centre", "Suburbs",
            timeLabel = "9:00 AM", dateLabel = "Sat, May 10", distanceKm = 35.0f, durationMin = 55, kgCO2 = 1.2f, isAutoDetected = true),
    ),
    "Thu, May 8" to listOf(
        RecentActivityEntry(TransportModeType.FLIGHT, "Delhi", "Mumbai",
            timeLabel = "6:00 AM", dateLabel = "Thu, May 8", distanceKm = 1150.0f, durationMin = 125, kgCO2 = 98.6f, isAutoDetected = true),
        RecentActivityEntry(TransportModeType.CAB, "Mumbai Airport", "Hotel",
            timeLabel = "8:45 AM", dateLabel = "Thu, May 8", distanceKm = 22.0f, durationMin = 40, kgCO2 = 3.8f, isAutoDetected = true),
    ),
)
