package com.example.dimadyplom2.map.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import com.example.dimadyplom2.map.model.MapPoint
import com.google.maps.android.compose.CameraPositionState

interface MapComponent {

    @Composable
    fun Render(
        modifier: Modifier,
        points: SnapshotStateMap<String, MapPoint>,
        cameraPositionState: CameraPositionState
    )
}


