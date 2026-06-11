package dev.atmos.shared.network

import dev.atmos.shared.auth.TokenRefresher
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.plugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal const val ATMOS_BASE_URL = "https://api.atmosapp.dev"

// Engine is resolved at compile time: OkHttp on Android, Darwin on iOS.
object AtmosHttpClient {
    val instance: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }.also { client ->
            // Intercept 401 responses: silently refresh the access token and retry
            // the original request once. Auth endpoints are excluded to prevent
            // infinite loops if the refresh token itself is rejected.
            client.plugin(HttpSend).intercept { request ->
                val call = execute(request)
                if (call.response.status != HttpStatusCode.Unauthorized) {
                    return@intercept call
                }
                if (request.url.buildString().startsWith("$ATMOS_BASE_URL/api/v1/auth/")) {
                    return@intercept call
                }
                val failedToken = request.headers[HttpHeaders.Authorization]
                    ?.removePrefix("Bearer ")
                val newToken = TokenRefresher.tryRefresh(failedToken)
                    ?: return@intercept call
                request.headers[HttpHeaders.Authorization] = "Bearer $newToken"
                execute(request)
            }
        }
    }
}
