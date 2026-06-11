package dev.atmos.shared.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Requests ─────────────────────────────────────────────────────────────────

@Serializable
private data class GoogleTokenRequest(
    @SerialName("id_token") val idToken: String,
)

@Serializable
private data class EmailSignInRequest(
    val email: String,
    val password: String,
)

@Serializable
private data class EmailSignUpRequest(
    @SerialName("display_name") val displayName: String,
    val email: String,
    val password: String,
)

@Serializable
private data class ForgotPasswordRequest(val email: String)

// ── Response DTOs ─────────────────────────────────────────────────────────────

// All Atmos backend responses are wrapped in this envelope.
@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class AuthResponseDto(
    val user: AuthUserDto,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("is_new_user") val isNewUser: Boolean = false,
)

// Used only by the token-refresh endpoint which returns tokens only (no user info).
@Serializable
data class TokenPairDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * Calls the Atmos backend auth endpoints.
 *
 * Endpoint: POST /api/v1/auth/google/token
 * Body:     { "id_token": "<raw Google ID token>" }
 * Returns:  [AuthResponseDto] with user info + JWT pair on success;
 *           a [Result.Failure] with a human-readable message otherwise.
 */
class AuthService(
    private val httpClient: HttpClient = AtmosHttpClient.instance,
) {

    suspend fun signInWithGoogle(idToken: String): Result<AuthResponseDto> = runCatching {
        val response = httpClient.post("$ATMOS_BASE_URL/api/v1/auth/google/token") {
            contentType(ContentType.Application.Json)
            setBody(GoogleTokenRequest(idToken))
        }
        if (response.status.value !in 200..299) {
            throw Exception(googleErrorMessage(response.status))
        }
        response.body<ApiEnvelope<AuthResponseDto>>().data
            ?: throw Exception("Empty response from server")
    }

    /**
     * Sign in with email + password.
     * Endpoint: POST /api/v1/auth/login
     * Body:     { "email": "...", "password": "..." }
     */
    suspend fun signIn(email: String, password: String): Result<AuthResponseDto> = runCatching {
        val response = httpClient.post("$ATMOS_BASE_URL/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(EmailSignInRequest(email = email, password = password))
        }
        if (response.status.value !in 200..299) {
            throw Exception(emailSignInErrorMessage(response.status))
        }
        response.body<ApiEnvelope<AuthResponseDto>>().data
            ?: throw Exception("Empty response from server")
    }

    /**
     * Register a new account with email + password.
     * Endpoint: POST /api/v1/auth/register
     * Body:     { "display_name": "...", "email": "...", "password": "..." }
     */
    suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
    ): Result<AuthResponseDto> = runCatching {
        val response = httpClient.post("$ATMOS_BASE_URL/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(EmailSignUpRequest(displayName = displayName, email = email, password = password))
        }
        if (response.status.value !in 200..299) {
            throw Exception(emailSignUpErrorMessage(response.status))
        }
        response.body<ApiEnvelope<AuthResponseDto>>().data
            ?: throw Exception("Empty response from server")
    }

    /**
     * Resend the verification email to the currently-authenticated user.
     * Endpoint: POST /api/v1/auth/resend-verification (Bearer auth required)
     */
    suspend fun resendVerification(accessToken: String): Result<Unit> = runCatching {
        val response = httpClient.post("$ATMOS_BASE_URL/api/v1/auth/resend-verification") {
            bearerAuth(accessToken)
        }
        if (response.status.value !in 200..299) {
            throw Exception("Could not send verification email. Please try again.")
        }
    }

    /**
     * Request a password-reset email.
     * Endpoint: POST /api/v1/auth/forgot-password
     * Body:     { "email": "..." }
     * Returns Unit on success (2xx); a human-readable failure message otherwise.
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> = runCatching {
        val response = httpClient.post("$ATMOS_BASE_URL/api/v1/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email))
        }
        if (response.status.value !in 200..299) {
            throw Exception(forgotPasswordErrorMessage(response.status))
        }
    }

    // ── Error message helpers ─────────────────────────────────────────────────

    private fun googleErrorMessage(status: HttpStatusCode): String = when (status.value) {
        400 -> "Invalid request. Please try again."
        401 -> "Google sign-in token was rejected. Please try again."
        403 -> "Access denied."
        429 -> "Too many requests. Please wait a moment and try again."
        in 500..599 -> "Server error (${status.value}). Please try again later."
        else -> "Sign-in failed (${status.value}). Please try again."
    }

    private fun emailSignInErrorMessage(status: HttpStatusCode): String = when (status.value) {
        400 -> "Invalid email or password."
        401 -> "Incorrect email or password. Please try again."
        403 -> "Access denied."
        429 -> "Too many attempts. Please wait a moment and try again."
        in 500..599 -> "Server error (${status.value}). Please try again later."
        else -> "Sign-in failed (${status.value}). Please try again."
    }

    private fun forgotPasswordErrorMessage(status: HttpStatusCode): String = when (status.value) {
        400 -> "Please enter a valid email address."
        429 -> "Too many attempts. Please wait a moment and try again."
        in 500..599 -> "Server error (${status.value}). Please try again later."
        else -> "Failed to send reset link. Please try again."
    }

    private fun emailSignUpErrorMessage(status: HttpStatusCode): String = when (status.value) {
        400 -> "Please check your details and try again."
        409 -> "An account with this email already exists."
        429 -> "Too many attempts. Please wait a moment and try again."
        in 500..599 -> "Server error (${status.value}). Please try again later."
        else -> "Sign-up failed (${status.value}). Please try again."
    }
}
