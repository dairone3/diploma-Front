package com.example.dimadyplom2.data

import com.example.dimadyplom2.map.repository.PointRepository
import com.example.dimadyplom2.map.repository.cityPointRepository
import com.example.dimadyplom2.map.repository.deleteTripStorage
import com.example.dimadyplom2.trip.TripRepository
import com.example.dimadyplom2.trip.tripRepository
import java.io.File

/**
 * Single entry point to the app's persisted data. Screens depend only on this
 * interface, so switching the underlying storage (files, Room, remote, …) means
 * providing a different implementation — no screen changes required.
 */
interface AppStorage {

    /** Repository for the list of trips. */
    val trips: TripRepository

    /** Repository for the points belonging to a single trip. */
    fun pointsFor(tripId: String): PointRepository

    /** Deletes a trip and all data associated with it (e.g. its points). */
    fun deleteTrip(tripId: String)
}

/**
 * File-backed [AppStorage]. This is the only place that knows about [filesDir];
 * a database-backed implementation would replace just this class.
 */
class FileAppStorage(private val filesDir: File) : AppStorage {

    // Single shared repository instance = single source of truth for the trip cache.
    override val trips: TripRepository = tripRepository(filesDir)

    override fun pointsFor(tripId: String): PointRepository =
        cityPointRepository(filesDir, tripId)

    override fun deleteTrip(tripId: String) {
        trips.deleteById(tripId)
        deleteTripStorage(filesDir, tripId)
    }
}

