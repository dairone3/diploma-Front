package com.example.dimadyplom2.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.dimadyplom2.data.AppStorage
import com.example.dimadyplom2.trip.Trip

enum class TripSort { CREATED, START_DATE }

/**
 * State holder for the My Trips screen. Owns loading, sorting and deletion so the
 * navigation layer stays free of screen logic. Depends on the [AppStorage] abstraction
 * rather than a concrete storage location.
 */
class MyTripsState(
    private val storage: AppStorage
) {
    private val trips = mutableStateListOf<Trip>()
    var sortBy by mutableStateOf(TripSort.CREATED)

    val sortedTrips: List<Trip>
        get() = when (sortBy) {
            TripSort.CREATED -> trips.sortedByDescending { it.createdAt }
            TripSort.START_DATE -> trips.sortedWith(compareBy(nullsLast()) { it.startDate })
        }

    /** Reloads the list from storage (call when entering the screen). */
    fun load() {
        trips.clear()
        trips.addAll(storage.trips.getAll())
    }

    /** Deletes a trip and its associated data, then updates the list. */
    fun delete(trip: Trip) {
        storage.deleteTrip(trip.id)
        trips.remove(trip)
    }
}


