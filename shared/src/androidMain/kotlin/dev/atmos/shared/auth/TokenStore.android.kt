package dev.atmos.shared.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.atmos.shared.location.TripDetectorHolder
import dev.atmos.shared.network.AuthResponseDto

// Renamed from the legacy plaintext "atmos_token" file — EncryptedSharedPreferences
// would throw trying to AEAD-decrypt data that file holds in cleartext, so the new
// store needs its own file rather than reopening the old name.
private const val ENCRYPTED_PREFS_FILE = "atmos_token_secure"
private const val LEGACY_PLAINTEXT_PREFS_FILE = "atmos_token"

actual fun createTokenStore(): TokenStore {
    val ctx = requireNotNull(TripDetectorHolder.appContext) {
        "TripDetectorHolder.init(context) must be called in MainActivity.onCreate() before TokenStore is accessed"
    }

    // One-time cleanup: delete the old plaintext file so stale tokens don't sit
    // unencrypted on disk forever. Safe to call every launch — no-op once deleted.
    ctx.getSharedPreferences(LEGACY_PLAINTEXT_PREFS_FILE, Context.MODE_PRIVATE)
        .edit().clear().apply()
    ctx.deleteSharedPreferences(LEGACY_PLAINTEXT_PREFS_FILE)

    val masterKey = MasterKey.Builder(ctx)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val prefs = EncryptedSharedPreferences.create(
        ctx,
        ENCRYPTED_PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    return AndroidTokenStore(prefs)
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

    override fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN,  accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
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
