package dev.atmos.shared.auth

import dev.atmos.shared.network.AuthResponseDto
import platform.Foundation.NSUserDefaults

actual fun createTokenStore(): TokenStore = IosTokenStore()

// ── iOS implementation ────────────────────────────────────────────────────────

private const val KEY_ACCESS_TOKEN  = "atmos_access_token"
private const val KEY_REFRESH_TOKEN = "atmos_refresh_token"
private const val KEY_USER_ID       = "atmos_user_id"
private const val KEY_USER_EMAIL    = "atmos_user_email"
private const val KEY_DISPLAY_NAME  = "atmos_display_name"
private const val KEY_AVATAR_URL    = "atmos_avatar_url"

internal class IosTokenStore : TokenStore {

    private val defaults = NSUserDefaults.standardUserDefaults()

    override val isLoggedIn: Boolean
        get() = defaults.stringForKey(KEY_ACCESS_TOKEN) != null

    override fun getAccessToken(): String?  = defaults.stringForKey(KEY_ACCESS_TOKEN)
    override fun getRefreshToken(): String? = defaults.stringForKey(KEY_REFRESH_TOKEN)
    override fun getUserId(): String?       = defaults.stringForKey(KEY_USER_ID)
    override fun getUserEmail(): String?    = defaults.stringForKey(KEY_USER_EMAIL)
    override fun getDisplayName(): String?  = defaults.stringForKey(KEY_DISPLAY_NAME)
    override fun getAvatarUrl(): String?    = defaults.stringForKey(KEY_AVATAR_URL)

    override fun save(response: AuthResponseDto) {
        defaults.setObject(response.accessToken,    forKey = KEY_ACCESS_TOKEN)
        defaults.setObject(response.refreshToken,   forKey = KEY_REFRESH_TOKEN)
        defaults.setObject(response.user.id,        forKey = KEY_USER_ID)
        defaults.setObject(response.user.email,     forKey = KEY_USER_EMAIL)
        defaults.setObject(response.user.displayName, forKey = KEY_DISPLAY_NAME)
        defaults.setObject(response.user.avatarUrl, forKey = KEY_AVATAR_URL)
    }

    override fun clear() {
        listOf(
            KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_USER_ID,
            KEY_USER_EMAIL, KEY_DISPLAY_NAME, KEY_AVATAR_URL,
        ).forEach { defaults.removeObjectForKey(it) }
    }
}
