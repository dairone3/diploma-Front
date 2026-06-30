package com.example.dimadyplom2.map.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.dimadyplom2.map.model.MapPoint

@Composable
fun PointDialog(
    point: MapPoint,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(point.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit point") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") }
            )
        },
        confirmButton = { TextButton(onClick = { onRename(title) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDelete) { Text("Delete") } }
    )
}

