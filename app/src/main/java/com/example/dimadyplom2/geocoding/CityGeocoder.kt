package com.example.dimadyplom2.geocoding

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class CitySuggestion(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

class CityGeocoder(context: Context) {

    private val geocoder = Geocoder(context.applicationContext, Locale.getDefault())

    @Suppress("DEPRECATION")
    suspend fun search(query: String, maxResults: Int = 6): List<CitySuggestion> {
        if (query.isBlank() || !Geocoder.isPresent()) return emptyList()

        return withContext(Dispatchers.IO) {
            runCatching {
                geocoder.getFromLocationName(query, maxResults)
                    .orEmpty()
                    .map { it.toSuggestion() }
            }.getOrDefault(emptyList())
        }
    }

    private fun Address.toSuggestion(): CitySuggestion =
        CitySuggestion(
            name = displayName(),
            latitude = latitude,
            longitude = longitude
        )

    private fun Address.displayName(): String {
        val city = locality ?: subAdminArea ?: adminArea ?: featureName
        return listOfNotNull(city, countryName)
            .joinToString(", ")
            .ifBlank { getAddressLine(0) ?: "Unknown location" }
    }
}

