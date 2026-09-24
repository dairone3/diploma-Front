package com.example.dimadyplom2.trip

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Stores the list of trips the user has created (city name + coordinates) so they can be
 * listed on the "My Trips" screen and re-opened on the map.
 */
class FileTripRepository(private val file: File) : TripRepository {
    private companion object {
        const val TAG = "FileTripRepository"
    }

    private val tripsById: MutableMap<String, Trip> by lazy { loadFromFile() }

    override fun getAll(): List<Trip> {
        val list = tripsById.values.toList()
        Log.d(TAG, "getAll: ${list.size} trips → ${list.map { "${it.id.take(8)}:'${it.name}'" }}")
        return list
    }

    override fun getById(id: String): Trip? {
        return tripsById[id]
    }

    /** Adds a trip, replacing any existing entry with the same id. */
    override fun upsert(trip: Trip) {
        tripsById[trip.id] = trip
        persist()
        Log.d(TAG, "upsert: id=${trip.id.take(8)} '${trip.name}' → total ${tripsById.size}")
    }

    /** Removes the trip with the given id, if present. */
    override fun deleteById(id: String) {
        if (tripsById.remove(id) != null) {
            persist()
            Log.d(TAG, "deleteById: id=${id.take(8)} → total ${tripsById.size}")
        }
    }

    private fun loadFromFile(): MutableMap<String, Trip> {
        ensureFileExists()
        val content = file.readText().trim()
        if (content.isEmpty() || !content.startsWith("[")) return mutableMapOf()

        val array = JSONArray(content)
        val result = mutableMapOf<String, Trip>()
        for (i in 0 until array.length()) {
            val json = array.optJSONObject(i) ?: continue
            val trip = toTrip(json) ?: continue
            result[trip.id] = trip
        }
        Log.d(TAG, "loadFromFile: total ${result.size} trips from ${file.absolutePath}")
        return result
    }

    private fun persist() {
        ensureFileExists()
        val array = JSONArray()
        tripsById.values.forEach { trip ->
            array.put(toJson(trip))
        }
        file.writeText(array.toString())
    }

    private fun toTrip(json: JSONObject): Trip? {
        val id = json.optString("id").ifBlank { return null }
        return Trip(
            name = json.optString("name"),
            destinations = readDestinations(json),
            startDate = if (json.has("startDate")) json.optLong("startDate") else null,
            endDate = if (json.has("endDate")) json.optLong("endDate") else null,
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            id = id
        )
    }

    /**
     * Reads the trip's destinations. Supports the current `destinations` array and,
     * for older saved trips, falls back to the flat `latitude`/`longitude` fields.
     */
    private fun readDestinations(json: JSONObject): List<Destination> {
        val array = json.optJSONArray("destinations")
        if (array != null) {
            return buildList {
                for (i in 0 until array.length()) {
                    val d = array.optJSONObject(i) ?: continue
                    add(
                        Destination(
                            name = d.optString("name"),
                            latitude = d.optDouble("latitude"),
                            longitude = d.optDouble("longitude"),
                            order = d.optInt("order", i)
                        )
                    )
                }
            }
        }
        // Legacy single-destination trips.
        if (json.has("latitude") && json.has("longitude")) {
            return listOf(
                Destination(
                    name = json.optString("name"),
                    latitude = json.optDouble("latitude"),
                    longitude = json.optDouble("longitude"),
                    order = 0
                )
            )
        }
        return emptyList()
    }

    private fun toJson(trip: Trip) = JSONObject().apply {
        put("id", trip.id)
        put("name", trip.name)
        put("destinations", JSONArray().apply {
            trip.destinations.forEach { d ->
                put(JSONObject().apply {
                    put("name", d.name)
                    put("latitude", d.latitude)
                    put("longitude", d.longitude)
                    put("order", d.order)
                })
            }
        })
        trip.startDate?.let { put("startDate", it) }
        trip.endDate?.let { put("endDate", it) }
        put("createdAt", trip.createdAt)
    }

    private fun ensureFileExists() {
        file.parentFile?.mkdirs()
        if (!file.exists()) file.writeText("[]")
    }
}

/** Builds the shared trips repository backed by a single index file. */
fun tripRepository(filesDir: File): TripRepository {
    val file = File(filesDir, "trips/index.json")
    return FileTripRepository(file)
}

