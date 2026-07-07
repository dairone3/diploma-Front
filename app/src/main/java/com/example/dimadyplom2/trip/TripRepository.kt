package com.example.dimadyplom2.trip
interface TripRepository {
    fun getAll(): List<Trip>
    fun getById(id: String): Trip?
    fun upsert(trip: Trip)
    fun deleteById(id: String)
}
