package dev.atmos.shared.network

import dev.atmos.shared.auth.AppTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class RegisterDeviceRequest(
    @SerialName("device_token")  val deviceToken: String,
    @SerialName("platform")      val platform: String,
    @SerialName("push_provider") val pushProvider: String,
    @SerialName("push_token")    val pushToken: String? = null,
    @SerialName("app_version")   val appVersion: String? = null,
)

@Serializable
data class DeviceDto(
    val id: String = "",
    @SerialName("device_token") val deviceToken: String = "",
    @SerialName("push_token")   val pushToken: String? = null,
    @SerialName("platform")     val platform: String = "",
)

class DeviceService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    // Upsert-by-device_token — safe to call on every login and on FCM token rotation.
    // The backend updates the stored push_token when the same device_token is re-registered.
    suspend fun registerDevice(
        deviceToken: String,
        pushToken: String,
        platform: String,
        pushProvider: String = "fcm",
    ): Result<DeviceDto> = runCatching {
        val token = AppTokenStore.instance.getAccessToken()
            ?: error("Not authenticated")

        val response = httpClient.post("$ATMOS_BASE_URL/api/v1/devices/register") {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(RegisterDeviceRequest(
                deviceToken  = deviceToken,
                platform     = platform,
                pushProvider = pushProvider,
                pushToken    = pushToken,
            ))
        }
        if (response.status.value !in 200..299) {
            throw Exception("Device registration failed (${response.status.value})")
        }
        response.body<ApiEnvelope<DeviceDto>>().data ?: DeviceDto()
    }
}
