package com.example.dimadyplom2.map.model

import java.util.UUID

data class MapPoint(
    val latitude: Double,
    val longitude: Double,
    val title: String = "",
    val id: String = UUID.randomUUID().toString()
)

