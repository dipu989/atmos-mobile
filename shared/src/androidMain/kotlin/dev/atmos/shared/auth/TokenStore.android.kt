package dev.atmos.shared.auth

import android.content.Context
import android.content.SharedPreferences
import dev.atmos.shared.location.TripDetectorHolder
import dev.atmos.shared.network.AuthResponseDto

actual fun createTokenStore(): TokenStore {
    val ctx = requireNotNull(TripDetectorHolder.appContext) {
        "TripDetectorHolder.init(context) must be called in MainActivity.onCreate() before TokenStore is accessed"
    }
    return AndroidTokenStore(
        ctx.getSharedPreferences("atmos_token", Context.MODE_PRIVATE)
    )
}

// ── Android implementation ────────────────────────────────────────────────────

private const val KEY_ACCESS_TOKEN  = "access_token"
private const val KEY_REFRESH_TOKEN = "refresh_token"
private const val KEY_USER_ID       = "user_id"
private const val KEY_USER_EMAIL    = "user_email"
private const val KEY_DISPLAY_NAME  = "display_name"
private const val KEY_AVATAR_URL    = "avatar_url"

internal class AndroidTokenStore(
    private val prefs: SharedPreferences,
) : TokenStore {

    override val isLoggedIn: Boolean
        get() = prefs.getString(KEY_ACCESS_TOKEN, null) != null

    override fun getAccessToken(): String?  = prefs.getString(KEY_ACCESS_TOKEN, null)
    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    override fun getUserId(): String?       = prefs.getString(KEY_USER_ID, null)
    override fun getUserEmail(): String?    = prefs.getString(KEY_USER_EMAIL, null)
    override fun getDisplayName(): String?  = prefs.getString(KEY_DISPLAY_NAME, null)
    override fun getAvatarUrl(): String?    = prefs.getString(KEY_AVATAR_URL, null)

    override fun save(response: AuthResponseDto) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN,  response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putString(KEY_USER_ID,       response.user.id)
            .putString(KEY_USER_EMAIL,    response.user.email)
            .putString(KEY_DISPLAY_NAME,  response.user.displayName)
            .putString(KEY_AVATAR_URL,    response.user.avatarUrl)
            .apply()
    }

    override fun clear() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_AVATAR_URL)
            .apply()
    }
}
