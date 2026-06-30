 package com.example.dimadyplom2.trip

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

interface TripStore {
    fun getAll(): List<Trip>
    fun add(trip: Trip)
    fun delete(id: String)
}

/**
 * Stores the list of trips the user has created (city name + coordinates) so they can be
 * listed on the "My Trips" screen and re-opened on the map.
 */
class TripRepository(private val file: File) : TripStore {
    private companion object {
        const val TAG = "TripRepository"
    }

    private val tripsById: MutableMap<String, Trip> by lazy { loadFromFile() }

    override fun getAll(): List<Trip> {
        val list = tripsById.values.toList()
        Log.d(TAG, "getAll: ${list.size} trips → ${list.map { "${it.id.take(8)}:'${it.name}'" }}")
        return list
    }

    /** Adds a trip, replacing any existing entry with the same id. */
    override fun add(trip: Trip) {
        tripsById[trip.id] = trip
        persist()
        Log.d(TAG, "add: id=${trip.id.take(8)} '${trip.name}' → total ${tripsById.size}")
    }

    /** Removes the trip with the given id, if present. */
    override fun delete(id: String) {
        if (tripsById.remove(id) != null) {
            persist()
            Log.d(TAG, "delete: id=${id.take(8)} → total ${tripsById.size}")
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
            latitude = json.optDouble("latitude"),
            longitude = json.optDouble("longitude"),
            id = id
        )
    }

    private fun toJson(trip: Trip) = JSONObject().apply {
        put("id", trip.id)
        put("name", trip.name)
        put("latitude", trip.latitude)
        put("longitude", trip.longitude)
    }

    private fun ensureFileExists() {
        file.parentFile?.mkdirs()
        if (!file.exists()) file.writeText("[]")
    }
}

/** Builds the shared trips repository backed by a single index file. */
fun tripRepository(filesDir: File): TripStore {
    val file = File(filesDir, "trips/index.json")
    return TripRepository(file)
}
