package com.example.dimadyplom2.map.repository

import com.example.dimadyplom2.map.model.MapPoint

interface PointRepository {

    fun getAll(): List<MapPoint>

    fun getById(id: String): MapPoint?

    fun upsert(point: MapPoint)

    fun deleteById(id: String)

    fun clearAll()
}


