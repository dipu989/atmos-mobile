package dev.atmos.shared.auth

import dev.atmos.shared.network.ATMOS_BASE_URL
import dev.atmos.shared.network.AtmosHttpClient
import dev.atmos.shared.network.AuthResponseDto
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

/**
 * Singleton that handles silent token refresh.
 *
 * Called by the [AtmosHttpClient] HttpSend interceptor whenever a request
 * returns 401. The [Mutex] ensures that concurrent 401 failures from parallel
 * requests only trigger one actual refresh call — subsequent waiters receive
 * the token that was already refreshed by the first caller.
 */
object TokenRefresher {

    private val mutex = Mutex()

    /**
     * Attempts a silent token refresh.
     *
     * @param tokenThatFailed The access token that produced the 401. If the
     *   store already holds a different (newer) token when the mutex is acquired,
     *   a concurrent caller already refreshed — the new token is returned immediately.
     * @return The new access token on success, or null if refresh failed
     *   (in which case [AuthState.onForceSignOut] has already been called).
     */
    suspend fun tryRefresh(tokenThatFailed: String?): String? = mutex.withLock {
        val store = AppTokenStore.instance

        // Another coroutine already refreshed while we were waiting — use the
        // new token directly rather than calling the refresh endpoint twice.
        val currentToken = store.getAccessToken()
        if (currentToken != null && currentToken != tokenThatFailed) {
            return@withLock currentToken
        }

        val refreshToken = store.getRefreshToken() ?: return@withLock null

        try {
            val response = AtmosHttpClient.instance.post("$ATMOS_BASE_URL/api/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(refreshToken))
            }
            if (response.status.value !in 200..299) {
                // Refresh token is expired or revoked — sign the user out
                store.clear()
                AuthState.onForceSignOut()
                return@withLock null
            }
            val dto = response.body<AuthResponseDto>()
            store.save(dto)
            AuthState.onSignedIn(
                AuthUser(
                    id          = dto.user.id,
                    email       = dto.user.email,
                    displayName = dto.user.displayName,
                    avatarUrl   = dto.user.avatarUrl ?: "",
                )
            )
            dto.accessToken
        } catch (_: Exception) {
            // Network error — don't force sign-out, let the original request fail
            // with its own error so the user sees a meaningful snackbar.
            null
        }
    }
}
