package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
data class GmailConnectionStatus(
    val connected: Boolean = false,
    val email: String? = null,
    @SerialName("connected_at") val connectedAt: String? = null,
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
)

@Serializable
private data class GmailAuthUrlResponse(
    val url: String,
)

// ── Service ───────────────────────────────────────────────────────────────────

class GmailService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    suspend fun getStatus(): Result<GmailConnectionStatus> = runCatching {
        val token = AppTokenStore.instance.getAccessToken() ?: error("Not authenticated")
        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/gmail/status") {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Gmail status fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<GmailConnectionStatus>>().data
            ?: throw Exception("Empty response from server")
    }.also { result ->
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

    suspend fun getAuthUrl(): Result<String> = runCatching {
        val token = AppTokenStore.instance.getAccessToken() ?: error("Not authenticated")
        // platform=mobile tells the backend to redirect to atmos://gmail/connected on success
        // instead of the web frontend, so the app can re-enter the foreground confirmed.
        val response = httpClient.get("$ATMOS_BASE_URL/api/v1/gmail/auth-url?platform=mobile") {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Gmail auth URL fetch failed (${response.status.value})")
        }
        response.body<ApiEnvelope<GmailAuthUrlResponse>>().data?.url
            ?: throw Exception("Empty auth URL from server")
    }.also { result ->
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

    suspend fun disconnect(): Result<Unit> = runCatching {
        val token = AppTokenStore.instance.getAccessToken() ?: error("Not authenticated")
        val response = httpClient.delete("$ATMOS_BASE_URL/api/v1/gmail/disconnect") {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Gmail disconnect failed (${response.status.value})")
        }
    }.also { result ->
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }
}
