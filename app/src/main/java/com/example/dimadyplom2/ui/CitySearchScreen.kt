package com.example.dimadyplom2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dimadyplom2.geocoding.CityGeocoder
import com.example.dimadyplom2.geocoding.CitySuggestion
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay

private const val SEARCH_DEBOUNCE_MS = 350L
private const val WORLD_ZOOM = 1f
private const val PREVIEW_CITY_ZOOM = 10f
private const val MIN_QUERY_LENGTH = 2
private const val MAX_SUGGESTIONS = 3

// Height reserved for the suggestion list: just enough for MAX_SUGGESTIONS rows.
private val SUGGESTIONS_AREA_HEIGHT = 168.dp

private val citySuggestionListSaver = listSaver<List<CitySuggestion>, Any>(
    save = { suggestions ->
        suggestions.flatMap { suggestion ->
            listOf(suggestion.name, suggestion.latitude, suggestion.longitude)
        }
    },
    restore = { flat ->
        flat.chunked(3).mapNotNull { values ->
            if (values.size < 3) return@mapNotNull null
            val name = values[0] as? String ?: return@mapNotNull null
            val latitude = (values[1] as? Number)?.toDouble() ?: return@mapNotNull null
            val longitude = (values[2] as? Number)?.toDouble() ?: return@mapNotNull null
            CitySuggestion(name = name, latitude = latitude, longitude = longitude)
        }
    }
)

@Composable
fun CitySearchScreen(onCitySelected: (CitySuggestion) -> Unit) {
    val context = LocalContext.current
    val geocoder = remember { CityGeocoder(context) }

    var query by rememberSaveable { mutableStateOf("") }
    var suggestions by rememberSaveable(stateSaver = citySuggestionListSaver) {
        mutableStateOf<List<CitySuggestion>>(emptyList())
    }

    val previewCamera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), WORLD_ZOOM)
    }

    LaunchedEffect(query) {
        if (query.trim().length < MIN_QUERY_LENGTH) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MS)
        suggestions = geocoder.search(query, maxResults = MAX_SUGGESTIONS)
        suggestions.firstOrNull()?.let { top ->
            previewCamera.position =
                CameraPosition.fromLatLngZoom(LatLng(top.latitude, top.longitude), PREVIEW_CITY_ZOOM)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search a city to start your trip") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        CitySuggestionList(
            suggestions = suggestions,
            onCitySelected = onCitySelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(SUGGESTIONS_AREA_HEIGHT)
        )

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            cameraPositionState = previewCamera
        )
    }
}

@Composable
private fun CitySuggestionList(
    suggestions: List<CitySuggestion>,
    onCitySelected: (CitySuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(suggestions) { suggestion ->
            Text(
                text = suggestion.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCitySelected(suggestion) }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )
            HorizontalDivider()
        }
    }
}

