package com.example.dimadyplom2.trip

import java.util.UUID

data class Trip(
    val name: String,
    val destinations: List<Destination> = emptyList(),
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString()
) {
    /** The first stop (lowest [Destination.order]), used e.g. for centering the map. */
    val primaryDestination: Destination?
        get() = destinations.minByOrNull { it.order }
}

/**
 * A single stop on a trip. A trip has an ordered list of these (see [Trip.destinations]).
 * [order] is the 0-based position of the stop within the trip.
 */
data class Destination(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val order: Int = 0
)

