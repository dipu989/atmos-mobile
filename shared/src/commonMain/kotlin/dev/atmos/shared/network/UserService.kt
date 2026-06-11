package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
private data class UpdateUserRequest(
    @SerialName("display_name") val displayName: String,
)

@Serializable
private data class UpdatePreferencesRequest(
    @SerialName("daily_goal_kg_co2e") val dailyGoalKgCo2e: Double,
)

@Serializable
data class UserPreferencesDto(
    @SerialName("daily_goal_kg_co2e") val dailyGoalKgCo2e: Double? = null,
)

@Serializable
data class UserDto(
    val id: String = "",
    val email: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url")   val avatarUrl: String? = null,
    val locale: String = "",
    val timezone: String = "",
)

// ── Service ───────────────────────────────────────────────────────────────────

class UserService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    suspend fun updateMe(displayName: String): Result<UserDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.patch("$ATMOS_BASE_URL/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(UpdateUserRequest(displayName = displayName))
        }
        if (response.status.value !in 200..299) {
            throw Exception("Profile update failed (${response.status.value})")
        }
        response.body<ApiEnvelope<UserDto>>().data
            ?: throw Exception("Empty response from server")
    }

    suspend fun deleteMe(): Result<Unit> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.delete("$ATMOS_BASE_URL/api/v1/users/me") {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Account deletion failed (${response.status.value})")
        }
    }

    suspend fun getMe(): Result<UserDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/users/me") {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("User fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<UserDto>>().data
            ?: throw Exception("Empty response from server")
    }

    suspend fun getPreferences(): Result<UserPreferencesDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/users/me/preferences") {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Preferences fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<UserPreferencesDto>>().data
            ?: throw Exception("Empty response from server")
    }

    suspend fun updatePreferences(dailyGoalKgCO2e: Double): Result<UserPreferencesDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.put("$ATMOS_BASE_URL/api/v1/users/me/preferences") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(UpdatePreferencesRequest(dailyGoalKgCo2e = dailyGoalKgCO2e))
        }
        if (response.status.value !in 200..299) {
            throw Exception("Preferences update failed (${response.status.value})")
        }
        response.body<ApiEnvelope<UserPreferencesDto>>().data
            ?: throw Exception("Empty response from server")
    }
}
