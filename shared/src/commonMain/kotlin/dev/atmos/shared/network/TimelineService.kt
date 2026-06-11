package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Response DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class ModeBreakdownDto(
    @SerialName("kg_co2e")     val kgCo2e: Float = 0f,
    @SerialName("distance_km") val distanceKm: Float = 0f,
    val count: Int = 0,
)

@Serializable
data class TrendDto(
    @SerialName("prev_total_kg_co2e") val prevTotalKgCo2e: Float = 0f,
    @SerialName("change_pct")         val changePct: Float? = null,
    val direction: String = "flat",   // "up" | "down" | "flat"
)

@Serializable
data class DailySummaryDto(
    @SerialName("total_kg_co2e")     val totalKgCo2e: Float = 0f,
    @SerialName("total_distance_km") val totalDistanceKm: Float = 0f,
    @SerialName("activity_count")    val activityCount: Int = 0,
    val breakdown: Map<String, ModeBreakdownDto> = emptyMap(),
    val trend: TrendDto = TrendDto(),
)

@Serializable
data class WeeklyDataPointDto(
    @SerialName("week_start")        val weekStart: String = "",
    @SerialName("week_end")          val weekEnd: String = "",
    @SerialName("total_kg_co2e")     val totalKgCo2e: Float = 0f,
    @SerialName("total_distance_km") val totalDistanceKm: Float = 0f,
    val trend: TrendDto = TrendDto(),
)

/** The weekly endpoint returns a list of day-level breakdowns for the current week. */
@Serializable
data class WeeklySummaryDto(
    @SerialName("total_kg_co2e")     val totalKgCo2e: Float = 0f,
    @SerialName("total_distance_km") val totalDistanceKm: Float = 0f,
    @SerialName("activity_count")    val activityCount: Int = 0,
    @SerialName("week_start")        val weekStart: String = "",
    @SerialName("week_end")          val weekEnd: String = "",
    val breakdown: Map<String, ModeBreakdownDto> = emptyMap(),
    val trend: TrendDto = TrendDto(),
    /** Daily data points for the bar chart — backend may include these as part of the weekly summary */
    val days: List<DailySummaryDto> = emptyList(),
)

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * Fetches pre-aggregated timeline summaries from the Atmos backend.
 *
 * - [getDaily] — GET /api/v1/timeline/daily  (today's impact + trend vs yesterday)
 * - [getWeekly] — GET /api/v1/timeline/weekly (current week totals)
 */
class TimelineService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    /** Returns today's CO₂ total, distance, and trend vs the previous day. */
    suspend fun getDaily(): Result<DailySummaryDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/timeline/daily") {
            bearerAuth(token)
        }
        if (response.status.value in 200..299) {
            response.body<DailySummaryDto>()
        } else {
            throw Exception("Timeline fetch failed (${response.status.value})")
        }
    }

    /** Returns the current week's CO₂ total, distance, and trend vs the previous week. */
    suspend fun getWeekly(): Result<WeeklySummaryDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/timeline/weekly") {
            bearerAuth(token)
        }
        if (response.status.value in 200..299) {
            response.body<WeeklySummaryDto>()
        } else {
            throw Exception("Timeline fetch failed (${response.status.value})")
        }
    }
}
