package com.example.dimadyplom2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

private const val WORLD_ZOOM = 2f
private val TOP_BAR_HEIGHT = 56.dp
private val CARD_HEIGHT = 200.dp
private const val CARD_COUNT = 6
private val MAP_SIDE_PADDING = 40.dp

@Composable
fun HomeScreen(onCreateTrip: () -> Unit) {
    val previewCamera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.0, 0.0), WORLD_ZOOM)
    }
    val scrollState = rememberScrollState()
    // While the map is being touched, disable page scrolling so up/down drags pan
    // the map instead of moving the screen.
    var mapTouched by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState, enabled = !mapTouched)
        ) {
            TopBar()

            Spacer(modifier = Modifier.height(16.dp))
            HighlightsCarousel()

            // Extra gap so the map sits noticeably lower than the cards.
            Spacer(modifier = Modifier.height(48.dp))
            // Inset map, a little taller than a square (grows downward, keeping the
            // gap above it unchanged). While touched, the page scroll is disabled so
            // the map handles up/down drags itself.
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MAP_SIDE_PADDING)
                    .aspectRatio(0.85f)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            mapTouched = true
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                            mapTouched = false
                        }
                    },
                cameraPositionState = previewCamera,
                uiSettings = MapUiSettings(scrollGesturesEnabled = true)
            )

            Spacer(modifier = Modifier.height(96.dp))
        }

        Button(
            onClick = onCreateTrip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text("Create trip")
        }
    }
}

@Composable
private fun TopBar() {
    // Placeholder for a future top bar (settings / profile). For now just a labeled
    // strip with a divider line underneath.
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TOP_BAR_HEIGHT),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Home", style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider()
    }
}

@Composable
private fun HighlightsCarousel() {
    // One large card per swipe (snaps). Placeholder content for now.
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Highlights",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        val pagerState = rememberPagerState(pageCount = { CARD_COUNT })
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CARD_HEIGHT)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        // Page indicator dots: the current card's dot is highlighted.
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(CARD_COUNT) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }
    }
}


