package com.example.dimadyplom2.api.clients

import com.example.dimadyplom2.api.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * [IHttpClient] for unauthenticated requests. Owns a Ktor [HttpClient] created once and
 * reused. Adds no auth header.
 */
class PublicHttpClient(
    private val baseUrl: String = BackendConfig.baseUrl,
    private val client: HttpClient = defaultClient(),
) : IHttpClient {

    override suspend fun get(path: String): HttpResponse =
        client.get("$baseUrl$path")

    override suspend fun post(path: String, body: Any?): HttpResponse =
        client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            if (body != null) setBody(body)
        }

    override suspend fun put(path: String, body: Any?): HttpResponse =
        client.put("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            if (body != null) setBody(body)
        }

    override suspend fun patch(path: String, body: Any?): HttpResponse =
        client.patch("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            if (body != null) setBody(body)
        }

    override suspend fun delete(path: String): HttpResponse =
        client.delete("$baseUrl$path")


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


