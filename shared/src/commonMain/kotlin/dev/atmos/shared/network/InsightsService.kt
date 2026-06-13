package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import dev.atmos.shared.ui.home.InsightEntry
import dev.atmos.shared.ui.home.InsightType
import kotlin.math.roundToInt
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Response DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class InsightMetadataDto(
    @SerialName("streak_count")      val streakCount: Float = 0f,
    @SerialName("goal_progress_pct") val goalProgressPct: Float = 0f,
    @SerialName("savings_pct")       val savingsPct: Float = 0f,
    @SerialName("comparison_pct")    val comparisonPct: Float = 0f,
)

@Serializable
data class InsightDto(
    val id: String = "",
    @SerialName("insight_type") val insightType: String = "",
    val title: String = "",
    val body: String = "",
    @SerialName("is_read")      val isRead: Boolean = false,
    val metadata: InsightMetadataDto = InsightMetadataDto(),
)

@Serializable
private data class MarkReadRequest(@SerialName("is_read") val isRead: Boolean)

@Serializable
data class InsightsResponseDto(
    val items: List<InsightDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
)

fun InsightDto.toInsightEntry(): InsightEntry = InsightEntry(
    id   = id,
    type = when (insightType) {
        "streak"                          -> InsightType.STREAK
        "milestone"                       -> InsightType.MILESTONE
        "tip"                             -> InsightType.TIP
        "comparison", "weekly_comparison" -> InsightType.COMPARISON
        "mode_spike", "mode_summary"      -> InsightType.TIP
        else                              -> InsightType.ANOMALY
    },
    title           = title,
    body            = body,
    isRead          = isRead,
    streakCount     = metadata.streakCount.roundToInt(),
    goalProgressPct = metadata.goalProgressPct.roundToInt(),
    savingsPct      = metadata.savingsPct.roundToInt(),
    comparisonPct   = metadata.comparisonPct.roundToInt(),
)

// ── Service ───────────────────────────────────────────────────────────────────

class InsightsService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    suspend fun markRead(insightId: String): Result<Unit> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.patch("$ATMOS_BASE_URL/api/v1/insights/$insightId") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(MarkReadRequest(isRead = true))
        }
        if (response.status.value !in 200..299) {
            throw Exception("Mark-read failed (${response.status.value})")
        }
    }

    suspend fun getInsights(limit: Int = 20, period: String = "week"): Result<InsightsResponseDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/insights") {
            bearerAuth(token)
            parameter("limit", limit)
            parameter("period", period)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Insights fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<InsightsResponseDto>>().data
            ?: throw Exception("Empty response from server")
    }
}
