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
    @SerialName("date_local")        val dateLocal: String = "",
    @SerialName("total_kg_co2e")     val totalKgCo2e: Float = 0f,
    @SerialName("total_distance_km") val totalDistanceKm: Float = 0f,
    @SerialName("activity_count")    val activityCount: Int = 0,
    val breakdown: Map<String, ModeBreakdownDto> = emptyMap(),
    val trend: TrendDto = TrendDto(),
)

@Serializable
data class MonthlySummaryDto(
    val year: Int = 0,
    val month: Int = 0,
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

    /** Returns a day's CO₂ total, distance, and trend vs the previous day.
     *  [date] is "YYYY-MM-DD"; defaults to today when null. */
    suspend fun getDaily(date: String? = null): Result<DailySummaryDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/timeline/daily") {
            bearerAuth(token)
            if (date != null) url { parameters.append("date", date) }
        }
        if (response.status.value !in 200..299) {
            throw Exception("Timeline fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<DailySummaryDto>>().data
            ?: throw Exception("Empty response from server")
    }

    /** Returns a week's CO₂ total, distance, and trend vs the previous week.
     *  [weekStart] is a Monday in "YYYY-MM-DD"; defaults to current week when null. */
    suspend fun getWeekly(weekStart: String? = null): Result<WeeklySummaryDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/timeline/weekly") {
            bearerAuth(token)
            if (weekStart != null) url { parameters.append("week_start", weekStart) }
        }
        if (response.status.value !in 200..299) {
            throw Exception("Timeline fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<WeeklySummaryDto>>().data
            ?: throw Exception("Empty response from server")
    }

    /** Returns a month's CO₂ total, distance, and trend vs the previous month. */
    suspend fun getMonthly(year: Int, month: Int): Result<MonthlySummaryDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/timeline/monthly") {
            bearerAuth(token)
            url {
                parameters.append("year", year.toString())
                parameters.append("month", month.toString())
            }
        }
        if (response.status.value !in 200..299) {
            throw Exception("Timeline fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<MonthlySummaryDto>>().data
            ?: throw Exception("Empty response from server")
    }

    /** Returns daily summaries for a date range (max 90 days).
     *  [from] and [to] are "YYYY-MM-DD". Results are ordered oldest-first. */
    suspend fun getRange(from: String, to: String): Result<List<DailySummaryDto>> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/timeline/range") {
            bearerAuth(token)
            url {
                parameters.append("from", from)
                parameters.append("to", to)
            }
        }
        if (response.status.value !in 200..299) {
            throw Exception("Timeline fetch failed (${response.status.value})")
        }
        // Backend returns DESC order; reverse for chronological (oldest-first) display.
        (response.body<ApiEnvelope<List<DailySummaryDto>>>().data
            ?: throw Exception("Empty response from server")).reversed()
    }
}
