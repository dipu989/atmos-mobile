package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

@Serializable
data class PlaceResult(
    val name: String,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class DistanceResult(
    val distanceKm: Double = 0.0,
    val found: Boolean = false,
)

class PlaceSearchService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {
    suspend fun search(query: String): Result<List<PlaceResult>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()

        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        httpClient
            .get("$ATMOS_BASE_URL/api/v1/places/autocomplete") {
                bearerAuth(token)
                parameter("q", query.trim())
            }
            .body<ApiEnvelope<List<PlaceResult>>>()
            .data
            ?: emptyList()
    }

    /** Route distance in km between two coordinates for the given Google travel mode. */
    suspend fun distance(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        mode: String,
    ): Result<DistanceResult> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        httpClient
            .get("$ATMOS_BASE_URL/api/v1/places/distance") {
                bearerAuth(token)
                parameter("originLat", originLat)
                parameter("originLng", originLng)
                parameter("destLat", destLat)
                parameter("destLng", destLng)
                parameter("mode", mode)
            }
            .body<ApiEnvelope<DistanceResult>>()
            .data
            ?: DistanceResult()
    }
}
