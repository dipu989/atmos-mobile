package dev.atmos.shared.auth

import dev.atmos.shared.network.AuthResponseDto

// ── Interface ─────────────────────────────────────────────────────────────────

/**
 * Persistent, platform-specific storage for auth tokens and basic user info.
 * Android: SharedPreferences; iOS: NSUserDefaults.
 *
 * Obtain via [AppTokenStore.instance].
 */
interface TokenStore {
    val isLoggedIn: Boolean
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun getUserId(): String?
    fun getUserEmail(): String?
    fun getDisplayName(): String?
    fun getAvatarUrl(): String?

    /** Persist tokens + user info from a successful auth response. */
    fun save(response: AuthResponseDto)

    /** Wipe all stored tokens (sign-out / delete account). */
    fun clear()
}

// ── Singleton holder ──────────────────────────────────────────────────────────

/**
 * App-wide singleton [TokenStore].
 * The platform-specific instance is created lazily on first access.
 *
 * Requirements:
 *   Android: [dev.atmos.shared.location.TripDetectorHolder.init] must be called
 *            in MainActivity.onCreate() before this is first accessed.
 *   iOS: No setup required — NSUserDefaults doesn't need a context.
 */
object AppTokenStore {
    val instance: TokenStore by lazy { createTokenStore() }
}

/** Platform factory — implemented in androidMain and iosMain. */
expect fun createTokenStore(): TokenStore
