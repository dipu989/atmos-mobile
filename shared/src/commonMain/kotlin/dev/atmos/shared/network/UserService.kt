package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
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

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class UpdateUserRequest(
    @SerialName("display_name") @EncodeDefault(EncodeDefault.Mode.NEVER) val displayName: String? = null,
    @SerialName("avatar_url")   @EncodeDefault(EncodeDefault.Mode.NEVER) val avatarUrl: String? = null,
)

@Serializable
private data class DeleteAccountRequest(
    val confirmation: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
private data class UpdatePreferencesRequest(
    @SerialName("daily_goal_kg_co2e")         @EncodeDefault(EncodeDefault.Mode.NEVER) val dailyGoalKgCo2e: Double? = null,
    @SerialName("push_notifications_enabled") @EncodeDefault(EncodeDefault.Mode.NEVER) val pushNotificationsEnabled: Boolean? = null,
    @SerialName("distance_unit")              @EncodeDefault(EncodeDefault.Mode.NEVER) val distanceUnit: String? = null,
    @SerialName("home_address")               @EncodeDefault(EncodeDefault.Mode.NEVER) val homeAddress: String? = null,
    @SerialName("home_lat")                   @EncodeDefault(EncodeDefault.Mode.NEVER) val homeLat: Double? = null,
    @SerialName("home_lng")                   @EncodeDefault(EncodeDefault.Mode.NEVER) val homeLng: Double? = null,
    @SerialName("work_address")               @EncodeDefault(EncodeDefault.Mode.NEVER) val workAddress: String? = null,
    @SerialName("work_lat")                   @EncodeDefault(EncodeDefault.Mode.NEVER) val workLat: Double? = null,
    @SerialName("work_lng")                   @EncodeDefault(EncodeDefault.Mode.NEVER) val workLng: Double? = null,
    @SerialName("default_transport")          @EncodeDefault(EncodeDefault.Mode.NEVER) val defaultTransport: String? = null,
)

@Serializable
data class UserPreferencesDto(
    @SerialName("daily_goal_kg_co2e")         val dailyGoalKgCo2e: Double? = null,
    @SerialName("push_notifications_enabled") val pushNotificationsEnabled: Boolean? = null,
    @SerialName("distance_unit")              val distanceUnit: String? = null,
    @SerialName("home_address")               val homeAddress: String? = null,
    @SerialName("home_lat")                   val homeLat: Double? = null,
    @SerialName("home_lng")                   val homeLng: Double? = null,
    @SerialName("work_address")               val workAddress: String? = null,
    @SerialName("work_lat")                   val workLat: Double? = null,
    @SerialName("work_lng")                   val workLng: Double? = null,
    @SerialName("default_transport")          val defaultTransport: String? = null,
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

        val response = httpClient.put("$ATMOS_BASE_URL/api/v1/users/me") {
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

    suspend fun updateAvatarUrl(url: String): Result<UserDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.put("$ATMOS_BASE_URL/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(UpdateUserRequest(avatarUrl = url))
        }
        if (response.status.value !in 200..299) {
            throw Exception("Avatar update failed (${response.status.value})")
        }
        response.body<ApiEnvelope<UserDto>>().data
            ?: throw Exception("Empty response from server")
    }

    suspend fun deleteMe(confirmation: String): Result<Unit> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.delete("$ATMOS_BASE_URL/api/v1/users/me") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(DeleteAccountRequest(confirmation = confirmation))
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
        distanceUnit: String? = null,
        homeAddress: String? = null,
        homeLat: Double? = null,
        homeLng: Double? = null,
        workAddress: String? = null,
        workLat: Double? = null,
        workLng: Double? = null,
        defaultTransport: String? = null,
    ): Result<UserPreferencesDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.put("$ATMOS_BASE_URL/api/v1/users/me/preferences") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(UpdatePreferencesRequest(
                dailyGoalKgCo2e          = dailyGoalKgCO2e,
                pushNotificationsEnabled = pushNotificationsEnabled,
                distanceUnit             = distanceUnit,
                homeAddress              = homeAddress,
                homeLat                  = homeLat,
                homeLng                  = homeLng,
                workAddress              = workAddress,
                workLat                  = workLat,
                workLng                  = workLng,
                defaultTransport         = defaultTransport,
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
