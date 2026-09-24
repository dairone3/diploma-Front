package com.example.dimadyplom2.map.repository

import android.util.Log
import com.example.dimadyplom2.map.model.MapPoint
import org.json.JSONObject
import java.io.File

class FilePointRepository(private val file: File) : PointRepository {

    private companion object {
        const val TAG = "FilePointRepository"
    }

    private val pointsById: MutableMap<String, MapPoint> by lazy { loadFromFile() }

    override fun getAll(): List<MapPoint> {
        val list = pointsById.values.toList()
        Log.d(TAG, "getAll: ${list.size} points → ${list.map { "${it.id.take(8)}:'${it.title}'" }}")
        return list
    }

    override fun getById(id: String): MapPoint? {
        return pointsById[id]
    }

    override fun upsert(point: MapPoint) {
        Log.d(TAG, "upsert: id=${point.id.take(8)} title='${point.title}'")
        pointsById[point.id] = point
        persist()
    }

    override fun deleteById(id: String) {
        Log.d(TAG, "deleteById: id=${id.take(8)} exists=${pointsById.containsKey(id)}")
        if (pointsById.remove(id) != null) {
            persist()
        }
    }

    override fun clearAll() {
        Log.d(TAG, "clearAll")
        pointsById.clear()
        ensureFileExists()
        file.writeText("{}")
    }

    private fun loadFromFile(): MutableMap<String, MapPoint> {
        ensureFileExists()
        val content = file.readText().trim()
        if (content.isEmpty()) return mutableMapOf()

        val root = if (content.startsWith("{")) JSONObject(content) else JSONObject()
        val result = mutableMapOf<String, MapPoint>()
        val ids = root.keys()
        while (ids.hasNext()) {
            val id = ids.next()
            val json = root.optJSONObject(id) ?: continue
            val point = toPoint(id, json)
            Log.d(TAG, "loadFromFile: loaded id=${id.take(8)} title='${point.title}'")
            result[id] = point
        }
        Log.d(TAG, "loadFromFile: total ${result.size} points from ${file.absolutePath}")
        return result
    }


    private fun persist() {
        ensureFileExists()
        val content = serialize(pointsById)
        file.writeText(content)
        Log.d(TAG, "persist: wrote ${pointsById.size} points → $content")
    }

    private fun ensureFileExists() {
        file.parentFile?.mkdirs()
        if (file.exists()) return

        file.writeText("{}")
        Log.i(TAG, "Created missing points file: ${file.absolutePath}")
    }

    private fun serialize(points: Map<String, MapPoint>): String {
        val root = JSONObject()
        points.forEach { (id, point) ->
            root.put(id, toJson(point))
        }
        return root.toString()
    }

    private fun toPoint(id: String, json: JSONObject): MapPoint {
        return MapPoint(
            latitude = json.getDouble("latitude"),
            longitude = json.getDouble("longitude"),
            title = json.optString("title", ""),
            isHighlighted = json.optBoolean("isHighlighted", false),
            id = id
        )
    }

    private fun toJson(point: MapPoint) = JSONObject().apply {
        put("latitude", point.latitude)
        put("longitude", point.longitude)
        put("title", point.title)
        put("isHighlighted", point.isHighlighted)
    }
}

/**
 * Builds a [PointRepository] whose storage file is scoped to a single trip,
 * so each trip keeps its own independent set of points. The [tripKey] should be a
 * stable, unique trip id.
 */
fun cityPointRepository(filesDir: File, tripKey: String): PointRepository {
    val file = File(tripStorageDir(filesDir, tripKey), "points.json")
    return FilePointRepository(file)
}

/** The folder that holds all stored data (e.g. points) for a single trip. */
fun tripStorageDir(filesDir: File, tripKey: String): File {
    val safeKey = tripKey
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "default" }

    return File(filesDir, "trips/$safeKey")
}

/** Permanently deletes all stored data for a single trip. */
fun deleteTripStorage(filesDir: File, tripKey: String) {
    tripStorageDir(filesDir, tripKey).deleteRecursively()
}

