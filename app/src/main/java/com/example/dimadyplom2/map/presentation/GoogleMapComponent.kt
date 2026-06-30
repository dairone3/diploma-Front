package com.example.dimadyplom2.map.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import android.util.Log
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.dimadyplom2.map.model.MapPoint
import com.example.dimadyplom2.map.repository.PointRepository

class GoogleMapComponent(
    private val repository: PointRepository,
    private val initialPosition: LatLng = LatLng(0.0, 0.0),
    private val initialZoom: Float = 2f
) : MapComponent {

    private companion object {
        const val TAG = "GoogleMapComponent"
    }

    @Composable
    override fun Render(modifier: Modifier) {
        val pointsById = remember {
            mutableStateMapOf<String, MapPoint>().apply {
                repository.getAll().forEach { point -> this[point.id] = point }
            }
        }
        var selectedId by remember { mutableStateOf<String?>(null) }
        var showClearDialog by remember { mutableStateOf(false) }
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialPosition, initialZoom)
        }

        Box(modifier = modifier) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomGesturesEnabled = true),
                onMapLongClick = { addPoint(pointsById, MapPoint(it.latitude, it.longitude, "New point")) }
            ) {
                pointsById.values.forEach { point ->
                    key(point.id) {
                        PointMarker(point) { selectedId = point.id }
                    }
                }
            }

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                onClick = { showClearDialog = true }
            ) {
                Text("CLR")
            }
        }

        selectedId?.let { id ->
            val point = pointsById[id] ?: return@let
            PointDialog(
                point = point,
                onRename = { newTitle -> renamePoint(pointsById, id, newTitle); selectedId = null },
                onDelete = { deletePoint(pointsById, id); selectedId = null },
                onDismiss = { selectedId = null }
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Delete all points") },
                text = { Text("Are you sure you want to delete all saved points?") },
                confirmButton = {
                    TextButton(onClick = {
                        clearAll(pointsById)
                        selectedId = null
                        showClearDialog = false
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @Composable
    private fun PointMarker(point: MapPoint, onClick: () -> Unit) {
        val markerState = remember(point.id) { MarkerState(LatLng(point.latitude, point.longitude)) }
        Marker(
            state = markerState,
            title = point.title,
            onClick = {
                Log.d(TAG, "Marker clicked: id=${point.id} title='${point.title}'")
                onClick()
                true
            }
        )
    }

    private fun addPoint(pointsById: MutableMap<String, MapPoint>, point: MapPoint) {
        Log.d(TAG, "addPoint: id=${point.id} title='${point.title}'")
        pointsById[point.id] = point
        repository.upsert(point)
    }

    private fun deletePoint(pointsById: MutableMap<String, MapPoint>, id: String) {
        Log.d(TAG, "deletePoint: id=$id title='${pointsById[id]?.title}'")
        pointsById.remove(id)
        repository.deleteById(id)
    }

    private fun renamePoint(pointsById: MutableMap<String, MapPoint>, id: String, title: String) {
        val point = pointsById[id] ?: return
        Log.d(TAG, "renamePoint: id=$id oldTitle='${point.title}' newTitle='$title'")
        val renamedPoint = point.copy(title = title)
        pointsById[id] = renamedPoint
        repository.upsert(renamedPoint)
    }

    private fun clearAll(pointsById: MutableMap<String, MapPoint>) {
        pointsById.clear()
        repository.clearAll()
    }
}


