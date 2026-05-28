package dev.atmos.shared.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ── !! CONFIGURATION REQUIRED !! ─────────────────────────────────────────────
//
// Replace this with the Client ID of the "atoms-web" (Web application type)
// OAuth 2.0 credential from Google Cloud Console → APIs & Services → Credentials.
//
// Example: "123456789-abc123def456.apps.googleusercontent.com"
//
private const val WEB_CLIENT_ID = "778685057864-2l30rul0rgv654jgdlhvkrhu7gg0nq7e.apps.googleusercontent.com"

// ── Holder ────────────────────────────────────────────────────────────────────

/**
 * Stores the Activity reference required by Credential Manager's bottom sheet.
 * Call [init] from MainActivity.onCreate() before setContent {}.
 */
object GoogleSignInHolder {
    private var instance: AndroidGoogleSignIn? = null

    fun init(activity: Activity) {
        instance = AndroidGoogleSignIn(activity)
    }

    fun get(): GoogleSignInLauncher = instance ?: NoOpGoogleSignInLauncher()
}

// ── actual factory ────────────────────────────────────────────────────────────

actual fun createGoogleSignInLauncher(): GoogleSignInLauncher = GoogleSignInHolder.get()

// ── Android implementation ────────────────────────────────────────────────────

/**
 * Launches the Android Credential Manager Google Sign-In bottom sheet.
 * The [activity] must be the foreground Activity — [ApplicationContext] will not work.
 */
class AndroidGoogleSignIn(private val activity: Activity) : GoogleSignInLauncher {

    override fun launch(callback: GoogleSignInCallback) {
        val credentialManager = CredentialManager.create(activity)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)  // show all accounts, not just previously used
            .setServerClientId(WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)           // always show account picker
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity,
                )
                val googleIdToken = GoogleIdTokenCredential
                    .createFrom(result.credential.data)
                callback.onResult(googleIdToken.idToken, null, false)
            } catch (e: GetCredentialCancellationException) {
                callback.onResult(null, null, true)   // user dismissed the picker
            } catch (e: GetCredentialException) {
                callback.onResult(null, e.message ?: "Credential error", false)
            } catch (e: Exception) {
                callback.onResult(null, e.message ?: "Sign-in failed", false)
            }
        }
    }
}
