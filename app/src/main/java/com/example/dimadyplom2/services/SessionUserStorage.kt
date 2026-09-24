package com.example.dimadyplom2.services

import com.example.dimadyplom2.models.SessionUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Holds the auth token for the signed-in user. Set after a successful login, cleared on
 * logout. A single shared instance (`object`) because there is only ever one current user.
 *
 * [user] is `@Volatile` so writes from one thread are immediately visible to request
 * coroutines running on other threads.
 *
 * Two one-shot events are exposed: [onSet] fires with the user on login, [onClear] fires on
 * logout. Both are buffered (`extraBufferCapacity`) so [set]/[clear] can emit without
 * suspending or needing a coroutine scope.
 *
 * Limitation: the token is kept in memory only, so it is lost when the app process is
 * killed and the user must log in again. To persist login across restarts, save the token
 * (e.g. to DataStore) and restore it via [set] on startup.
 */
object SessionUserStorage {

    @Volatile
    var user: SessionUser? = null
        private set

    private val _onSet = MutableSharedFlow<SessionUser>(extraBufferCapacity = 1)

    /** Fires with the signed-in user whenever [set] stores a non-null user (login). */
    val onSet: SharedFlow<SessionUser> = _onSet.asSharedFlow()

    /** Store the token after login/registration, then emit [onSet] with the user. */
    fun set(user: SessionUser?) {
        this.user = user
        if (user != null) _onSet.tryEmit(user)
    }
    private val _onClear = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Fires whenever [clear] drops the session (logout). */
    val onClear: SharedFlow<Unit> = _onClear.asSharedFlow()

    /** Drop the token on logout, then emit [onClear]. */
    fun clear() {
        user = null
        _onClear.tryEmit(Unit)
    }

    /** True once a token has been stored. */
    val userHasToken: Boolean get() = user?.token != null

    /** Returns the token or throws if the user isn't authenticated. */
    fun getOrThrow(): SessionUser = user ?: error("Not authenticated: no auth token")
}
