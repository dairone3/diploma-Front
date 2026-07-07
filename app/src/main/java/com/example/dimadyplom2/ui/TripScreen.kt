package com.example.dimadyplom2.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dimadyplom2.data.AppStorage
import com.example.dimadyplom2.map.model.MapPoint
import com.example.dimadyplom2.map.presentation.GoogleMapComponent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val CITY_ZOOM = 12f
private const val POINT_ZOOM = 15f
private val SHEET_PEEK_HEIGHT = 96.dp

// Fractions of the screen height the sheet can snap to (in addition to the peek/closed height).
private val SHEET_STOPS = listOf(0.25f, 0.5f, 0.75f, 1.0f)

@Composable
fun TripScreen(
    tripId: String,
    storage: AppStorage
) {
    val repository = remember(tripId) { storage.pointsFor(tripId) }
    val points = remember(tripId) {
        mutableStateMapOf<String, MapPoint>().apply {
            repository.getAll().forEach { point -> this[point.id] = point }
        }
    }
    val trip = remember(tripId) { storage.trips.getById(tripId) }
    val mapComponent = remember(tripId) { GoogleMapComponent(repository) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(trip?.latitude ?: 0.0, trip?.longitude ?: 0.0),
            CITY_ZOOM
        )
    }

    fun renamePoint(id: String, title: String) {
        val point = points[id] ?: return
        val updated = point.copy(title = title)
        points[id] = updated
        repository.upsert(updated)
    }

    fun deletePoint(id: String) {
        points.remove(id)
        repository.deleteById(id)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxHeightDp = maxHeight
        val maxHeightPx = with(density) { maxHeightDp.toPx() }
        val peekPx = with(density) { SHEET_PEEK_HEIGHT.toPx() }
        // Snap heights (px): closed/peek first, then 25/50/75/100% of the screen.
        val anchorsPx = remember(maxHeightPx, peekPx) {
            (listOf(peekPx) + SHEET_STOPS.map { it * maxHeightPx }).sorted()
        }
        val quarterPx = 0.25f * maxHeightPx
        val threeQuarterPx = 0.75f * maxHeightPx
        val scope = rememberCoroutineScope()
        val sheetHeight = remember { Animatable(peekPx) }

        fun focusPoint(point: MapPoint) {
            scope.launch { sheetHeight.animateTo(quarterPx) }
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(point.latitude, point.longitude),
                        POINT_ZOOM
                    )
                )
            }
        }

        mapComponent.Render(
            modifier = Modifier.fillMaxSize(),
            points = points,
            cameraPositionState = cameraPositionState
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(with(density) { sheetHeight.value.toDp() }),
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DragHandle(
                    modifier = Modifier
                        .clickable { scope.launch { sheetHeight.animateTo(threeQuarterPx) } }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                scope.launch {
                                    // Dragging up (negative delta) grows the sheet.
                                    val newHeight = (sheetHeight.value - delta).coerceIn(peekPx, maxHeightPx)
                                    sheetHeight.snapTo(newHeight)
                                }
                            },
                            onDragStopped = {
                                val target = anchorsPx.minByOrNull { abs(it - sheetHeight.value) } ?: peekPx
                                sheetHeight.animateTo(target)
                            }
                        )
                )
                TripPointsList(
                    points = points.values.toList(),
                    onPointClick = { point -> focusPoint(point) },
                    onRename = { id, title -> renamePoint(id, title) },
                    onDelete = { id -> deletePoint(id) }
                )
            }
        }
    }
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun TripPointsList(
    points: List<MapPoint>,
    onPointClick: (MapPoint) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Points (${points.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (points.isEmpty()) {
            Text(
                text = "No points yet. Long-press the map to add one.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(points) { point ->
                    val isEditing = editingId == point.id

                    fun commitRename() {
                        onRename(point.id, editText)
                        editingId = null
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPointClick(point) }
                    ) {
                        if (isEditing) {
                            OutlinedTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { commitRename() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            IconButton(onClick = { commitRename() }) {
                                Icon(Icons.Filled.Check, contentDescription = "Save name")
                            }
                        } else {
                            AutoResizeText(
                                text = point.title.ifBlank { "Untitled point" },
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .clickable {
                                        editingId = point.id
                                        editText = point.title
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                            // Any space that isn't the name text focuses the point on the map.
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDelete(point.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete point")
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/**
 * Text that shrinks its font size to fit the available width, wrapping to up to
 * [maxLines] lines once it reaches [minFontSize]. Keeps a long name from covering
 * the whole row.
 */
@Composable
private fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit = 18.sp,
    minFontSize: TextUnit = 10.sp,
    maxLines: Int = 2
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        fontSize = fontSize,
        maxLines = maxLines,
        softWrap = true,
        overflow = TextOverflow.Clip,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize) {
                fontSize = (fontSize.value - 1f).coerceAtLeast(minFontSize.value).sp
            } else {
                readyToDraw = true
            }
        }
    )
}


