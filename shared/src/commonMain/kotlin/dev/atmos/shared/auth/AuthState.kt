package dev.atmos.shared.auth

import kotlinx.coroutines.flow.MutableStateFlow
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

    val isLoggedIn: Boolean get() = _currentUser.value != null

    /**
     * Called after a successful sign-in (token exchange + persistence already done).
     */
    fun onSignedIn(user: AuthUser) {
        _currentUser.value = user
    }

    /**
     * Called on explicit sign-out or account deletion.
     */
    fun onSignedOut() {
        _currentUser.value = null
    }
}
