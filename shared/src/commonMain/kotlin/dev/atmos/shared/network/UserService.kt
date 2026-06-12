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
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
private data class UpdateUserRequest(
    @SerialName("display_name") val displayName: String,
)

@Serializable
private data class DeleteAccountRequest(
    val password: String,
    val confirmation: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class UpdatePreferencesRequest(
    @SerialName("daily_goal_kg_co2e")         @EncodeDefault(EncodeDefault.Mode.NEVER) val dailyGoalKgCo2e: Double? = null,
    @SerialName("push_notifications_enabled") @EncodeDefault(EncodeDefault.Mode.NEVER) val pushNotificationsEnabled: Boolean? = null,
    @SerialName("weekly_report_enabled")      @EncodeDefault(EncodeDefault.Mode.NEVER) val weeklyReportEnabled: Boolean? = null,
    @SerialName("data_sharing_enabled")       @EncodeDefault(EncodeDefault.Mode.NEVER) val dataSharingEnabled: Boolean? = null,
)

@Serializable
data class UserPreferencesDto(
    @SerialName("daily_goal_kg_co2e")         val dailyGoalKgCo2e: Double? = null,
    @SerialName("push_notifications_enabled") val pushNotificationsEnabled: Boolean? = null,
    @SerialName("weekly_report_enabled")      val weeklyReportEnabled: Boolean? = null,
    @SerialName("data_sharing_enabled")       val dataSharingEnabled: Boolean? = null,
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

    suspend fun deleteMe(password: String = "", confirmation: String = "delete my account"): Result<Unit> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.delete("$ATMOS_BASE_URL/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(DeleteAccountRequest(password = password, confirmation = confirmation))
        }
        if (response.status.value !in 200..299) {
            val errorMsg = try { response.body<ApiEnvelope<Unit>>().error } catch (_: Exception) { null }
            throw Exception(errorMsg ?: "Account deletion failed (${response.status.value})")
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

    suspend fun updatePreferences(
        dailyGoalKgCO2e: Double? = null,
        pushNotificationsEnabled: Boolean? = null,
        weeklyReportEnabled: Boolean? = null,
        dataSharingEnabled: Boolean? = null,
    ): Result<UserPreferencesDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.put("$ATMOS_BASE_URL/api/v1/users/me/preferences") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(UpdatePreferencesRequest(
                dailyGoalKgCo2e          = dailyGoalKgCO2e,
                pushNotificationsEnabled = pushNotificationsEnabled,
                weeklyReportEnabled      = weeklyReportEnabled,
                dataSharingEnabled       = dataSharingEnabled,
            ))
        }
        if (response.status.value !in 200..299) {
            throw Exception("Preferences update failed (${response.status.value})")
        }
        response.body<ApiEnvelope<UserPreferencesDto>>().data
            ?: throw Exception("Empty response from server")
    }.also { result ->
        // Propagate cancellation so a cancelled toggle job doesn't trigger the .onFailure revert.
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }
}
