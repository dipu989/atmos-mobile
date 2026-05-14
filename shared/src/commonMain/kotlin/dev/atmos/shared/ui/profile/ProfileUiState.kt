package dev.atmos.shared.ui.profile

// ── Data models ───────────────────────────────────────────────────────────────

data class CommuteLocation(
    val label: String,
    val address: String?,
)

enum class AppearanceMode { LIGHT, DARK, SYSTEM }

data class ProfilePreferences(
    val pushNotificationsEnabled: Boolean,
    val appearanceMode: AppearanceMode,
    val defaultTransportLabel: String,
    val unitsLabel: String,
)

data class ProfileUiState(
    val displayName: String,
    val initials: String,
    val email: String,
    val totalCO2SavedKg: Float,
    val daysTracked: Int,
    val todayKgCO2: Float,
    val dailyGoalKgCO2: Float,
    val home: CommuteLocation,
    val work: CommuteLocation,
    val preferences: ProfilePreferences,
)

// ── Mock data ─────────────────────────────────────────────────────────────────

val previewProfileUiState = ProfileUiState(
    displayName    = "Shantnu Kumar",
    initials       = "SK",
    email          = "shantnu@atmos.dev",
    totalCO2SavedKg = 48.2f,
    daysTracked    = 23,
    todayKgCO2     = 3.4f,
    dailyGoalKgCO2 = 5.0f,
    home = CommuteLocation("Home", "123 Main Street, Brooklyn NY"),
    work = CommuteLocation("Work", "456 Office Plaza, Manhattan NY"),
    preferences = ProfilePreferences(
        pushNotificationsEnabled = true,
        appearanceMode = AppearanceMode.SYSTEM,
        defaultTransportLabel = "Public Transit",
        unitsLabel = "Metric",
    ),
)
