package com.example.dimadyplom2.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.dimadyplom2.map.repository.cityPointRepository
import com.example.dimadyplom2.map.presentation.GoogleMapComponent
import com.google.android.gms.maps.model.LatLng

private const val CITY_ZOOM = 12f

@Composable
fun TripScreen(
    tripId: String,
    cityName: String,
    latitude: Double,
    longitude: Double
) {
    val context = LocalContext.current
    val mapComponent = remember(tripId, latitude, longitude) {
        val repository = cityPointRepository(context.filesDir, tripId)
        GoogleMapComponent(
            repository = repository,
            initialPosition = LatLng(latitude, longitude),
            initialZoom = CITY_ZOOM
        )
    }

    mapComponent.Render(modifier = Modifier.fillMaxSize())
}


