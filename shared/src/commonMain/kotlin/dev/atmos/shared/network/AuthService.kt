package dev.atmos.shared.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Request ───────────────────────────────────────────────────────────────────

@Serializable
private data class GoogleTokenRequest(
    @SerialName("id_token") val idToken: String,
)

// ── Response DTOs ─────────────────────────────────────────────────────────────

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class AuthResponseDto(
    val user: AuthUserDto,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("is_new_user") val isNewUser: Boolean,
)

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * Calls the Atmos backend auth endpoints.
 *
 * Endpoint: POST /api/v1/auth/google/token
 * Body:     { "id_token": "<raw Google ID token>" }
 * Returns:  [AuthResponseDto] with user info + JWT pair.
 */
class AuthService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    suspend fun signInWithGoogle(idToken: String): Result<AuthResponseDto> = runCatching {
        httpClient.post("$ATMOS_BASE_URL/api/v1/auth/google/token") {
            contentType(ContentType.Application.Json)
            setBody(GoogleTokenRequest(idToken))
        }.body<AuthResponseDto>()
    }
}
