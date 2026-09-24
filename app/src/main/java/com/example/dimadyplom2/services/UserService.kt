package com.example.dimadyplom2.services

import com.example.dimadyplom2.api.AuthApi
import com.example.dimadyplom2.api.UserResponse
import com.example.dimadyplom2.models.SessionUser
import com.example.dimadyplom2.util.logger

// Dependency concept:
// panel -> [(userService -> authApi -> httpClient), sessionUserStorage]

class UserService(
    private val authApi: AuthApi,
) {
    private val log = logger()

    /** Registers a new user, then creates and stores the session. */
    suspend fun signUp(login: String, password: String) {
        val userResponse: UserResponse = authApi.register(login, password)
        val user = SessionUser(userResponse.token, userResponse.user.id, userResponse.user.login)
        SessionUserStorage.set(user)
        log.i("signed up and signed in as id ${user.id}")
    }

    /** Logs in an existing user, then creates and stores the session. */
    suspend fun signIn(login: String, password: String) {
        val userResponse: UserResponse = authApi.login(login, password)
        val user = SessionUser(userResponse.token, userResponse.user.id, userResponse.user.login)
        SessionUserStorage.set(user)
        log.i("signed in as id ${user.id}")
    }

    /** Clears the stored session. */
    fun signOut() {
        SessionUserStorage.clear()
        log.i("signed out")
    }

    // update user info
}