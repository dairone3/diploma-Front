package com.example.dimadyplom2.api.clients

import io.ktor.client.statement.HttpResponse

/**
 * Contract for an HTTP client. Implemented by [PublicHttpClient] (no auth) and
 * [AuthorizedHttpClient] (attaches the signed-in user's token).
 */
interface IHttpClient {
    suspend fun get(path: String): HttpResponse
    suspend fun post(path: String, body: Any? = null): HttpResponse
    suspend fun put(path: String, body: Any? = null): HttpResponse
    suspend fun patch(path: String, body: Any? = null): HttpResponse
    suspend fun delete(path: String): HttpResponse
}

interface IMetaHttpClient {
    suspend fun options(path: String): HttpResponse
    suspend fun head(path: String): HttpResponse
}


