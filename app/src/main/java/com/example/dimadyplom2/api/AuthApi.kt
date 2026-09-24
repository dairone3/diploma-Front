package com.example.dimadyplom2.api

import com.example.dimadyplom2.api.clients.IHttpClient
import com.example.dimadyplom2.util.logger
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Registration/login against the public endpoints. On success it stores the returned token
 * in [com.example.dimadyplom2.services.SessionUserStorage], so subsequent [com.example.dimadyplom2.api.clients.AuthorizedHttpClient] requests send it automatically.
 */

/** Thrown on auth failures the user can act on. [message] is safe to show in the UI. */
sealed class AuthException(message: String) : Exception(message) {
    class LoginAlreadyExists : AuthException("User with this login already exists")
    class InvalidCredentials : AuthException("Wrong login or password")
}

class AuthApi(
    private val client: IHttpClient
) {
    private val log = logger()

    /** Registers a new user and returns the token. Throws if the server rejects the request. */
    suspend fun register(login: String, password: String): UserResponse {
        val httpResponse: HttpResponse = try {
            client.post("/auth/register", Credentials(login, password))
        } catch (e: Exception) {
            log.e("register: cannot reach server (${describeNetworkError(e)}) for $login", e)
            throw e
        }
        if (!httpResponse.status.isSuccess()) {
            val body = httpResponse.bodyAsText()
            if (httpResponse.status == HttpStatusCode.Conflict) {
                log.i("register: login already exists: $login")
                throw AuthException.LoginAlreadyExists()
            }
            log.e("register failed unexpectedly: ${httpResponse.status} $body")
            error("register failed: ${httpResponse.status} $body")
        }
        return httpResponse.body<UserResponse>()
    }

    /** Logs in an existing user and returns the token. Throws if the server rejects the request. */
    suspend fun login(login: String, password: String): UserResponse {
        val httpResponse: HttpResponse = try {
            client.post("/auth/login", Credentials(login, password))
        } catch (e: Exception) {
            log.e("login: cannot reach server (${describeNetworkError(e)}) for $login", e)
            throw e
        }
        if (!httpResponse.status.isSuccess()) {
            val body = httpResponse.bodyAsText()
            if (httpResponse.status == HttpStatusCode.Unauthorized) {
                log.i("login: wrong login or password for: $login")
                throw AuthException.InvalidCredentials()
            }
            log.e("login failed unexpectedly: ${httpResponse.status} $body")
            error("login failed: ${httpResponse.status} $body")
        }
        return httpResponse.body<UserResponse>()
    }


    @Serializable
    private data class Credentials(val login: String, val password: String)

    /** Maps a request exception to a short, human reason for the logs. */
    private fun describeNetworkError(e: Throwable): String = when (e) {
        is UnknownHostException -> "no internet / server address can't be resolved"
        is HttpRequestTimeoutException, is SocketTimeoutException -> "request timed out"
        is ConnectException -> "server unreachable / connection refused"
        is SSLException -> "secure connection (TLS) failed"
        else -> "unexpected network error"
    }
}



