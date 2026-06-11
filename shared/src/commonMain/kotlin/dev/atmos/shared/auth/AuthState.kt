package dev.atmos.shared.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// ── Domain model ──────────────────────────────────────────────────────────────

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String,
)

// ── App-wide auth state ───────────────────────────────────────────────────────

/**
 * Observable singleton for authentication state.
 *
 * [currentUser] is null when signed out, non-null when signed in.
 * Collect from Compose via `AuthState.currentUser.collectAsState()`.
 */
object AuthState {

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser

    // Emits Unit when the token refresher forces a sign-out (expired refresh token).
    // AtmosApp collects this and calls handleSignOut() to navigate to Onboarding.
    private val _forceSignOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceSignOut: SharedFlow<Unit> = _forceSignOut

    val isLoggedIn: Boolean get() = _currentUser.value != null

    fun onSignedIn(user: AuthUser) {
        _currentUser.value = user
    }

    fun onSignedOut() {
        _currentUser.value = null
    }

    /** Called by [dev.atmos.shared.auth.TokenRefresher] when the refresh token is expired or revoked. */
    internal fun onForceSignOut() {
        _currentUser.value = null
        _forceSignOut.tryEmit(Unit)
    }
}
