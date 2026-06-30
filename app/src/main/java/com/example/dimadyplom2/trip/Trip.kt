package com.example.dimadyplom2.trip

import java.util.UUID

data class Trip(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val id: String = UUID.randomUUID().toString()
)

