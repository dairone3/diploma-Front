package com.example.dimadyplom2.trip

import java.util.UUID

data class Trip(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val startingCity: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val id: String = UUID.randomUUID().toString()
)

