package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Response DTO ──────────────────────────────────────────────────────────────

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

    suspend fun getMe(): Result<UserDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/users/me") {
            bearerAuth(token)
        }
        if (response.status.value in 200..299) {
            response.body<UserDto>()
        } else {
            throw Exception("User fetch failed (${response.status.value})")
        }
    }
}
