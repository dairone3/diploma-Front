package com.example.dimadyplom2.api

import kotlinx.serialization.Serializable

/**
 * Wire models for the trips endpoints. Dates are kept as raw ISO-8601 strings here (the
 * backend's format); converting them to epoch millis happens in the mapping layer, not here.
 *
 * There are two response shapes:
 *  - [TripSummaryDto] — items of `GET /api/trips` (list): counts only, NO coordinates.
 *  - [TripDto] — `GET/POST/PATCH /api/trips/{id}` (single): full, with nested arrays.
 */

/** One item of `GET /api/trips` (list). Lightweight summary — no coordinates. */
@Serializable
data class TripSummaryDto(
    val id: Int,
    val name: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val destinationsCount: Int,
    val usersCount: Int,
    val createdAt: String
)

/** Full trip from `GET/POST/PATCH /api/trips/{id}`. */
@Serializable
data class TripDto(
    val id: Int,
    val name: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val destination: List<DestinationDto> = emptyList(),
    val users: List<TripUserDto> = emptyList(),
    val points: List<PointDto> = emptyList(),
    val createdAt: String
)

/** A trip's ordered stop (from `trips_cities`). */
@Serializable
data class DestinationDto(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int,
    val createdAt: String
)

/** A trip member. Only the id is exposed. */
@Serializable
data class TripUserDto(
    val userId: Int
)

/** A map pin on a trip (from `points`). No `order`. */
@Serializable
data class PointDto(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String
)

// --- request bodies ---

/** Body for `POST /api/trips`. Dates are ISO-8601 strings; destinations get their order by position. */
@Serializable
data class CreateTripRequest(
    val name: String,
    val startDate: String? = null,
    val endDate: String? = null,
    val destination: List<NewDestination> = emptyList(),
    val users: List<TripUserDto> = emptyList()
)

/** A new stop to create with a trip (no id/order — the server assigns those). */
@Serializable
data class NewDestination(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

