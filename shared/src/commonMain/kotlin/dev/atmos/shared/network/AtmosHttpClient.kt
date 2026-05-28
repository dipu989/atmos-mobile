package dev.atmos.shared.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

internal const val ATMOS_BASE_URL = "https://api.atmosapp.dev"

/**
 * Lazily-created shared [HttpClient].
 *
 * Engine is resolved automatically at compile time:
 *   Android → OkHttp (ktor-client-okhttp in androidMain deps)
 *   iOS     → Darwin (ktor-client-darwin in iosMain deps)
 *
 * Access via [AtmosHttpClient.instance].
 */
object AtmosHttpClient {
    val instance: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true   // backend may add fields we haven't modelled yet
                    isLenient = true
                })
            }
        }
    }
}
