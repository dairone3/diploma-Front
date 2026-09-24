package com.example.dimadyplom2.api

import com.example.dimadyplom2.api.clients.AuthorizedHttpClient
import com.example.dimadyplom2.api.clients.IHttpClient
import com.example.dimadyplom2.api.clients.PublicHttpClient

/**
 * Single place that wires the networking layer together. Owns one shared instance of each
 * HTTP client (they each hold a Ktor engine, so they should be reused) and builds the
 * services/APIs with the right client:
 *  - [publicClient] — no token, for auth (login/register).
 *  - [authorizedClient] — attaches the signed-in user's token, for everything else.
 */
object ApiFactory {

    private val publicClient: IHttpClient by lazy { PublicHttpClient() }
    private val authorizedClient: IHttpClient by lazy { AuthorizedHttpClient() }

    /** Auth (login/register) — uses the public client since there's no token yet. */
    fun createAuthApi(): AuthApi = AuthApi(publicClient)

    /** Trips — uses the authorized client so the token is attached automatically. */
    fun createTripsApi(): TripsApi = TripsApi(authorizedClient)


    // Future authenticated APIs are built the same way, e.g.:
    // fun pointsApi(): PointsApi = PointsApi(authorizedClient)
}

