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
    val percentage: Int,
)

enum class InsightType {
    STREAK, MILESTONE, TIP, COMPARISON, ANOMALY
}

data class InsightEntry(
    val type: InsightType,
    val title: String,
    val body: String,
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
)

// ── Pending trip (auto-detected, awaiting user confirmation) ──────────────────

data class PendingTripEntry(
    val mode: TransportModeType,
    val origin: String,
    val destination: String,
    val distanceKm: Float,
    val durationMin: Int,
    val estimatedKgCO2: Float,
)

data class HomeUiState(
    val greeting: String,
    val dateLabel: String,
    val user: UserProfile,
    val todayImpact: TodayImpact,
    val weeklyTrend: List<WeeklyDataPoint>,
    val transportBreakdown: List<TransportModeEntry>,
    val recentActivity: List<RecentActivityEntry>,
    val insights: List<InsightEntry>,
    val unreadInsightsCount: Int,
    val pendingTrip: PendingTripEntry? = null,
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
    pendingTrip = PendingTripEntry(
        mode             = TransportModeType.DRIVING,
        origin           = "Office",
        destination      = "Home",
        distanceKm       = 11.8f,
        durationMin      = 24,
        estimatedKgCO2   = 1.7f,
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
