package dev.atmos.shared.ui.home

// ── Data models ───────────────────────────────────────────────────────────────

data class UserProfile(
    val displayName: String,
    val initials: String,
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

enum class TransportModeType {
    DRIVING, PUBLIC_TRANSIT, CYCLING, WALKING,
    CAB, METRO, TRAIN, BUS, AUTO_RICKSHAW, TWO_WHEELER, FLIGHT;

    val isZeroEmission: Boolean
        get() = this == CYCLING || this == WALKING

    val isHighEmission: Boolean
        get() = this == DRIVING || this == CAB || this == FLIGHT
}

data class TransportModeEntry(
    val mode: TransportModeType,
    val displayName: String,
    val distanceKm: Float,
    val kgCO2: Float,
    val percentage: Int,           // share of today's total emissions
)

data class HomeUiState(
    val greeting: String,
    val dateLabel: String,
    val user: UserProfile,
    val todayImpact: TodayImpact,
    val weeklyTrend: List<WeeklyDataPoint>,
    val transportBreakdown: List<TransportModeEntry>,
    val unreadInsightsCount: Int,
)

// ── Mock data ─────────────────────────────────────────────────────────────────

val previewHomeUiState = HomeUiState(
    greeting = "Good morning",
    dateLabel = "Sunday, May 10",
    user = UserProfile(displayName = "John Doe", initials = "JD"),
    todayImpact = TodayImpact(
        kgCO2 = 3.4f,
        dailyGoalKgCO2 = 5.0f,
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
        TransportModeEntry(
            mode = TransportModeType.DRIVING,
            displayName = "Driving",
            distanceKm = 12.3f,
            kgCO2 = 2.8f,
            percentage = 65,
        ),
        TransportModeEntry(
            mode = TransportModeType.PUBLIC_TRANSIT,
            displayName = "Public Transit",
            distanceKm = 8.5f,
            kgCO2 = 0.9f,
            percentage = 21,
        ),
        TransportModeEntry(
            mode = TransportModeType.CYCLING,
            displayName = "Cycling",
            distanceKm = 3.2f,
            kgCO2 = 0f,
            percentage = 8,
        ),
        TransportModeEntry(
            mode = TransportModeType.WALKING,
            displayName = "Walking",
            distanceKm = 1.8f,
            kgCO2 = 0f,
            percentage = 6,
        ),
    ),
    unreadInsightsCount = 3,
)
