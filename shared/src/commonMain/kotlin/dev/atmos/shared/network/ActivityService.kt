package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import dev.atmos.shared.ui.home.RecentActivityEntry
import dev.atmos.shared.ui.home.TransportModeType
import dev.atmos.shared.ui.home.emissionFactor
import dev.atmos.shared.util.formatDateGroupLabel
import dev.atmos.shared.util.formatTimestamp
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Transport mode mapping ────────────────────────────────────────────────────

/**
 * Maps the mobile [TransportModeType] enum to the backend's validated mode string.
 * Backend accepts: car, cab, auto_rickshaw, bus, metro, train, two_wheeler, walking, cycling, flight
 */
val TransportModeType.backendMode: String
    get() = when (this) {
        TransportModeType.DRIVING        -> "car"
        TransportModeType.CAB            -> "cab"
        TransportModeType.AUTO_RICKSHAW  -> "auto_rickshaw"
        TransportModeType.BUS            -> "bus"
        TransportModeType.PUBLIC_TRANSIT -> "bus"
        TransportModeType.METRO          -> "metro"
        TransportModeType.TRAIN          -> "train"
        TransportModeType.TWO_WHEELER    -> "two_wheeler"
        TransportModeType.WALKING        -> "walking"
        TransportModeType.CYCLING        -> "cycling"
        TransportModeType.FLIGHT         -> "flight"
    }

// ── Request / Response DTOs ───────────────────────────────────────────────────

@Serializable
private data class IngestActivityRequest(
    @SerialName("transport_mode")   val transportMode: String,
    @SerialName("distance_km")      val distanceKm: Double,
    @SerialName("duration_minutes") val durationMinutes: Int?,
    @SerialName("source")           val source: String,
    @SerialName("started_at")       val startedAt: String,   // ISO-8601
    @SerialName("ended_at")         val endedAt: String?,    // ISO-8601
    @SerialName("idempotency_key")  val idempotencyKey: String,
    // Coordinates from Google Places autocomplete — used for server-side trip dedup.
    @SerialName("origin_lat")       val originLat: Double? = null,
    @SerialName("origin_lng")       val originLng: Double? = null,
    @SerialName("dest_lat")         val destLat: Double? = null,
    @SerialName("dest_lng")         val destLng: Double? = null,
)

@Serializable
data class ActivityDto(
    val id: String,
    @SerialName("transport_mode")   val transportMode: String? = null,
    @SerialName("distance_km")      val distanceKm: Double? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("started_at")       val startedAt: String = "",
    val source: String? = null,
    val status: String = "pending",
    val origin: String? = null,
    val destination: String? = null,
    @SerialName("origin_lat")       val originLat: Double? = null,
    @SerialName("origin_lng")       val originLng: Double? = null,
    @SerialName("dest_lat")         val destLat: Double? = null,
    @SerialName("dest_lng")         val destLng: Double? = null,
)

@Serializable
data class ActivitiesPageDto(
    val activities: List<ActivityDto> = emptyList(),
    val total: Long = 0,
    val limit: Int = 50,
    val offset: Int = 0,
)

// ── Mapper ────────────────────────────────────────────────────────────────────

private fun String.toTransportModeType(): TransportModeType = when (this) {
    "car"           -> TransportModeType.DRIVING
    "cab"           -> TransportModeType.CAB
    "auto_rickshaw" -> TransportModeType.AUTO_RICKSHAW
    "bus"           -> TransportModeType.BUS
    "metro"         -> TransportModeType.METRO
    "train"         -> TransportModeType.TRAIN
    "two_wheeler"   -> TransportModeType.TWO_WHEELER
    "walking", "walk"    -> TransportModeType.WALKING   // "walk" is a legacy backend alias
    "cycling", "bicycle" -> TransportModeType.CYCLING   // "bicycle" is a legacy backend alias
    "flight"        -> TransportModeType.FLIGHT
    else            -> TransportModeType.DRIVING
}

fun ActivityDto.toRecentActivityEntry(): RecentActivityEntry {
    val mode        = transportMode?.toTransportModeType() ?: TransportModeType.DRIVING
    val distKm      = distanceKm?.toFloat() ?: 0f
    val startMs     = runCatching { Instant.parse(startedAt).toEpochMilliseconds() }.getOrDefault(0L)
    val src         = source ?: ""
    return RecentActivityEntry(
        mode           = mode,
        origin         = origin?.takeIf { it.isNotBlank() } ?: mode.displayLabel,
        destination    = destination?.takeIf { it.isNotBlank() } ?: "",
        timeLabel      = if (startMs > 0L) formatTimestamp(startMs) else "",
        dateLabel      = if (startMs > 0L) formatDateGroupLabel(startMs) else "Unknown",
        distanceKm     = distKm,
        durationMin    = durationMinutes ?: 0,
        kgCO2          = mode.emissionFactor * distKm,
        isAutoDetected = src != "manual",
        sessionId      = id,
        timestampMs    = startMs,
        source         = src,
        originLat      = originLat,
        originLng      = originLng,
        destLat        = destLat,
        destLng        = destLng,
    )
}

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

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * Syncs local trips to the Atmos backend.
 *
 * **ingestActivity** — POST /api/v1/activities
 *   Called after every confirmed session (manual or auto-detected).
 *   Uses [sessionId] as the idempotency key so retries are safe.
 *   Returns [Result.success] with the backend UUID on 201, or
 *   [Result.success] with an empty ActivityDto on 409 (already synced).
 *
 * **listActivities** — GET /api/v1/activities
 *   Returns the last 30 days of activities from the backend.
 */
class ActivityService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    suspend fun ingestActivity(
        sessionId: String,
        mode: TransportModeType,
        distanceKm: Float,
        durationMin: Int,
        startedAtMs: Long,
        endedAtMs: Long?,
        source: String = "manual",
        originLat: Double? = null,
        originLng: Double? = null,
        destLat: Double? = null,
        destLng: Double? = null,
    ): Result<ActivityDto> = runCatching {
        val token = requireAccessToken()

        val startedAt  = Instant.fromEpochMilliseconds(startedAtMs).toIso8601()
        val endedAt    = endedAtMs?.let { Instant.fromEpochMilliseconds(it).toIso8601() }

        val response = httpClient.post("$ATMOS_BASE_URL/api/v1/activities") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(
                IngestActivityRequest(
                    transportMode   = mode.backendMode,
                    distanceKm      = distanceKm.toDouble(),
                    durationMinutes = if (durationMin > 0) durationMin else null,
                    source          = source,
                    startedAt       = startedAt,
                    endedAt         = endedAt,
                    idempotencyKey  = sessionId,
                    originLat       = originLat,
                    originLng       = originLng,
                    destLat         = destLat,
                    destLng         = destLng,
                )
            )
        }

        when (response.status) {
            HttpStatusCode.Created  -> response.body<ApiEnvelope<ActivityDto>>().data
                ?: throw Exception("Empty response from server")
            HttpStatusCode.Conflict -> ActivityDto(id = "")   // already synced — no-op
            else -> throw Exception(httpErrorMessage(response.status.value))
        }
    }

    suspend fun deleteActivity(activityId: String): Result<Unit> = runCatching {
        val token = requireAccessToken()

        val response = httpClient.delete("$ATMOS_BASE_URL/api/v1/activities/$activityId") {
            bearerAuth(token)
        }
        // 404 = activity already gone (deleted from another device) — goal is achieved
        if (response.status.value !in 200..299 && response.status.value != 404) {
            throw Exception(httpErrorMessage(response.status.value))
        }
    }

    suspend fun listActivities(
        fromMs: Long? = null,
        toMs: Long? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<ActivitiesPageDto> = runCatching {
        val token = requireAccessToken()

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/activities") {
            bearerAuth(token)
            if (fromMs != null) parameter("from", Instant.fromEpochMilliseconds(fromMs).toDateString())
            if (toMs != null)   parameter("to",   Instant.fromEpochMilliseconds(toMs).toDateString())
            parameter("limit", limit)
            if (offset > 0) parameter("offset", offset)
        }
        requireOk(response)
        response.body<ApiEnvelope<ActivitiesPageDto>>().data
            ?: throw Exception("Empty response from server")
    }

    suspend fun listAllActivities(): Result<List<ActivityDto>> = runCatching {
        val allActivities = mutableListOf<ActivityDto>()
        val pageSize = 100  // backend hard cap is 100; exceeding it resets to 50 silently
        val maxPages  = 50  // safety cap: 50 × 100 = 5 000 activities max; guards against infinite loop on bad total
        var offset = 0
        repeat(maxPages) {
            // On a mid-pagination failure, return whatever we have rather than losing all prior pages.
            val page = listActivities(limit = pageSize, offset = offset)
                .getOrElse { return@runCatching allActivities.toList() }
            allActivities += page.activities
            // Primary exit: empty page means no more data.
            // Secondary exit: honour backend total only when it's a positive value — guards against
            // total=0 (count query failure) which would otherwise trigger an immediate early exit.
            if (page.activities.isEmpty() || (page.total > 0L && allActivities.size >= page.total)) {
                return@runCatching allActivities.toList()
            }
            offset += page.activities.size  // advance by items actually received, not requested
        }
        allActivities.toList()
    }

    suspend fun exportActivitiesCsv(): Result<String> = runCatching {
        val token = requireAccessToken()

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/activities/export") {
            bearerAuth(token)
        }
        // No-content responses (e.g. zero trips) skip body decoding — some engines
        // don't handle reading an empty body as a String cleanly.
        if (response.status == HttpStatusCode.NoContent) return@runCatching ""
        requireOk(response)
        if (response.contentType()?.match(ContentType.Application.Json) == true) {
            throw Exception("Unexpected response format from server.")
        }
        response.body<String>()
    }

    suspend fun getActivity(activityId: String): Result<RecentActivityEntry> = runCatching {
        val token = requireAccessToken()
        val dto = httpClient
            .get("$ATMOS_BASE_URL/api/v1/activities/$activityId") { bearerAuth(token) }
            .body<ApiEnvelope<ActivityDto>>()
            .data ?: throw Exception("Empty response from server")
        dto.toRecentActivityEntry()
    }

    private suspend fun requireAccessToken(): String =
        AppTokenStore.instance.getAccessToken() ?: error("Not authenticated")

    private fun requireOk(response: HttpResponse) {
        if (response.status.value !in 200..299) {
            throw Exception(httpErrorMessage(response.status.value))
        }
    }

    private fun httpErrorMessage(code: Int): String = when (code) {
        400 -> "Invalid activity data."
        401 -> "Session expired. Please sign in again."
        403 -> "Access denied."
        429 -> "Too many requests. Please try again later."
        in 500..599 -> "Server error ($code). Please try again later."
        else -> "Sync failed ($code)."
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** ISO-8601 datetime string e.g. "2024-05-10T08:45:00Z" */
private fun Instant.toIso8601(): String {
    val ldt = toLocalDateTime(TimeZone.UTC)
    return "${ldt.year.pad4()}-${ldt.monthNumber.pad2()}-${ldt.dayOfMonth.pad2()}" +
           "T${ldt.hour.pad2()}:${ldt.minute.pad2()}:${ldt.second.pad2()}Z"
}

/** Date-only string e.g. "2024-05-10" */
private fun Instant.toDateString(): String {
    val ldt = toLocalDateTime(TimeZone.UTC)
    return "${ldt.year.pad4()}-${ldt.monthNumber.pad2()}-${ldt.dayOfMonth.pad2()}"
}

private fun Int.pad2() = toString().padStart(2, '0')
private fun Int.pad4() = toString().padStart(4, '0')
