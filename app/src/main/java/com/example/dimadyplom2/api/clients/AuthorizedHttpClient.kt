package com.example.dimadyplom2.api.clients

import com.example.dimadyplom2.api.BackendConfig
import com.example.dimadyplom2.services.SessionUserStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * [IHttpClient] for requests that require a signed-in user. Before every request it reads the
 * token from [SessionUserStorage] (throwing if the user isn't logged in) and attaches it as a
 * `Authorization: Bearer <token>` header. Owns a Ktor [HttpClient] created once and reused.
 */
class AuthorizedHttpClient(
    private val baseUrl: String = BackendConfig.baseUrl,
    private val client: HttpClient = defaultClient(),
) : IHttpClient {

    override suspend fun get(path: String): HttpResponse =
        client.get("$baseUrl$path") { auth() }

    override suspend fun post(path: String, body: Any?): HttpResponse =
        client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            auth()
            if (body != null) setBody(body)
        }

    override suspend fun put(path: String, body: Any?): HttpResponse =
        client.put("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            auth()
            if (body != null) setBody(body)
        }

    override suspend fun patch(path: String, body: Any?): HttpResponse =
        client.patch("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            auth()
            if (body != null) setBody(body)
        }

    override suspend fun delete(path: String): HttpResponse =
        client.delete("$baseUrl$path") { auth() }

    /** Checks the user is signed in and attaches the Bearer token. */
    private fun HttpRequestBuilder.auth() {
        header(HttpHeaders.Authorization, "Bearer ${SessionUserStorage.getOrThrow().token}")
    }

    companion object {
        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                    }
                )
            }
        }
    }
}





