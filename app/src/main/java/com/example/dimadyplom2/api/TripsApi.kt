package com.example.dimadyplom2.api

import com.example.dimadyplom2.api.clients.IHttpClient
import com.example.dimadyplom2.util.logger
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Thrown on trip failures the user can act on. [message] is safe to show in the UI. */
sealed class TripException(message: String) : Exception(message) {
    class NotFound : TripException("Trip not found")
    class Forbidden : TripException("You don't have access to this trip")
    class Unauthorized : TripException("Please sign in again")
}

/**
 * Calls the trips endpoints via the authorized client (the token is attached automatically).
 *
 * Shapes: [list] returns lightweight [TripSummaryDto]s (no coordinates); [get]/[create]/[update]
 * return the full [TripDto]. [delete] returns nothing (204 No Content).
 */
class TripsApi(
    private val client: IHttpClient
) {
    private val log = logger()

    /** GET /api/trips — the caller's trips, newest first (summaries, no coordinates). */
    suspend fun list(): List<TripSummaryDto> {
        val res = client.get("/trips")
        if (!res.status.isSuccess()) failure("list trips", res)
        return res.body()
    }

    /** GET /api/trips/{id} — one full trip (with destinations/points). */
    suspend fun get(tripId: Int): TripDto {
        val res = client.get("/trips/$tripId")
        if (!res.status.isSuccess()) failure("get trip $tripId", res)
        return res.body()
    }

    /** POST /api/trips — create a trip; returns the created full trip (with its new id). */
    suspend fun create(request: CreateTripRequest): TripDto {
        val res = client.post("/trips", request)
        if (!res.status.isSuccess()) failure("create trip", res)
        return res.body()
    }

    /**
     * PATCH /api/trips/{id} — partial update of the given fields; returns the full updated trip.
     * Only non-null arguments are sent, so omitted fields are left unchanged. (Clearing a date
     * to null isn't supported through this path yet.)
     */
    suspend fun update(
        tripId: Int,
        name: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): TripDto {
        val body = buildJsonObject {
            if (name != null) put("name", name)
            if (startDate != null) put("startDate", startDate)
            if (endDate != null) put("endDate", endDate)
        }
        val res = client.patch("/trips/$tripId", body)
        if (!res.status.isSuccess()) failure("update trip $tripId", res)
        return res.body()
    }

    /** DELETE /api/trips/{id} — 204 on success (no body). */
    suspend fun delete(tripId: Int) {
        val res = client.delete("/trips/$tripId")
        if (!res.status.isSuccess()) failure("delete trip $tripId", res)
    }

    /** Logs and throws a typed exception for a failed response. Never returns normally. */
    private suspend fun failure(action: String, res: HttpResponse): Nothing {
        val body = res.bodyAsText()
        when (res.status) {
            HttpStatusCode.Unauthorized -> {
                log.i("$action: unauthorized (token missing/expired)")
                throw TripException.Unauthorized()
            }
            HttpStatusCode.Forbidden -> {
                log.i("$action: forbidden (not the caller's trip)")
                throw TripException.Forbidden()
            }
            HttpStatusCode.NotFound -> {
                log.i("$action: not found")
                throw TripException.NotFound()
            }
            else -> {
                log.e("$action failed unexpectedly: ${res.status} $body")
                error("$action failed: ${res.status} $body")
            }
        }
    }
}

