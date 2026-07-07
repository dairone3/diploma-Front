package com.example.dimadyplom2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dimadyplom2.trip.Trip

@Composable
fun MyTripsScreen(
    state: MyTripsState,
    onTripSelected: (Trip) -> Unit,
    modifier: Modifier = Modifier
) {
    val trips = state.sortedTrips

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.sortBy == TripSort.CREATED,
                onClick = { state.sortBy = TripSort.CREATED },
                label = { Text("Created date") }
            )
            FilterChip(
                selected = state.sortBy == TripSort.START_DATE,
                onClick = { state.sortBy = TripSort.START_DATE },
                label = { Text("Start date") }
            )
        }

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trips yet",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                items(trips) { trip ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTripSelected(trip) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = trip.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Created ${formatCreatedAt(trip.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            tripDatesLabel(trip)?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { state.delete(trip) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete ${trip.name}"
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

/** Human-readable label for a trip's planned dates, or null if none are set. */
private fun tripDatesLabel(trip: Trip): String? {
    val start = trip.startDate
    val end = trip.endDate
    return when {
        start != null && end != null -> "${formatTripDate(start)} – ${formatTripDate(end)}"
        start != null -> "From ${formatTripDate(start)}"
        end != null -> "Until ${formatTripDate(end)}"
        else -> null
    }
}

