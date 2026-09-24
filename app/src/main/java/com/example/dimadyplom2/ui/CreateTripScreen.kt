package com.example.dimadyplom2.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dimadyplom2.geocoding.CityGeocoder
import com.example.dimadyplom2.geocoding.CitySuggestion
import com.example.dimadyplom2.trip.Destination
import com.example.dimadyplom2.trip.Trip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 350L
private const val MIN_QUERY_LENGTH = 2
private const val MAX_SUGGESTIONS = 3

/**
 * Screen for creating a new trip. The user fills in a required destination and an
 * editable trip name that defaults to the destination. Pressing "Create trip"
 * geocodes the destination for map coordinates and hands the built [Trip] back via
 * [onCreateTrip].
 */
@Composable
fun CreateTripScreen(
    initialDestination: String = "",
    onCreateTrip: (Trip) -> Unit
) {
    val context = LocalContext.current
    val geocoder = remember { CityGeocoder(context) }
    val scope = rememberCoroutineScope()

    var destination by rememberSaveable { mutableStateOf(initialDestination) }
    var name by rememberSaveable { mutableStateOf(initialDestination) }
    // Once the user edits the name manually, stop mirroring the destination into it.
    var nameEdited by rememberSaveable { mutableStateOf(false) }
    // Optional trip start / end dates (UTC midnight millis from the date picker).
    var startDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var endDate by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameEdited = true
            },
            label = { Text("Trip name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))


        CityAutocompleteField(
            value = destination,
            onValueChange = {
                destination = it
                if (!nameEdited) name = it
            },
            label = "Destination",
            geocoder = geocoder,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TripDateRangeField(
            startMillis = startDate,
            endMillis = endDate,
            onPick = { start, end -> startDate = start; endDate = end },
            onClear = { startDate = null; endDate = null }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch {
                    val top = geocoder.search(destination, maxResults = 1).firstOrNull()
                    val trip = Trip(
                        name = name.ifBlank { destination },
                        destinations = listOf(
                            Destination(
                                name = destination,
                                latitude = top?.latitude ?: 0.0,
                                longitude = top?.longitude ?: 0.0,
                                order = 0
                            )
                        ),
                        startDate = startDate,
                        endDate = endDate
                    )
                    // Clear the fields so the screen is empty next time it's opened.
                    name = ""
                    destination = ""
                    nameEdited = false
                    startDate = null
                    endDate = null
                    onCreateTrip(trip)
                }
            },
            enabled = destination.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create trip")
        }
    }
}

/**
 * A labeled button showing the picked trip date range (or a hint when empty) that opens
 * a Material date-range picker. A trailing clear button appears once a range is set.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripDateRangeField(
    startMillis: Long?,
    endMillis: Long?,
    onPick: (Long?, Long?) -> Unit,
    onClear: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val label = when {
        startMillis != null && endMillis != null ->
            "${formatTripDate(startMillis)} – ${formatTripDate(endMillis)}"
        startMillis != null -> "From ${formatTripDate(startMillis)}"
        else -> "Trip dates (optional)"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.weight(1f)
        ) {
            Text(label)
        }
        if (startMillis != null || endMillis != null) {
            IconButton(onClick = onClear) {
                Icon(Icons.Filled.Clear, contentDescription = "Clear trip dates")
            }
        }
    }

    if (showPicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startMillis,
            initialSelectedEndDateMillis = endMillis
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onPick(state.selectedStartDateMillis, state.selectedEndDateMillis)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Outlined text field with city suggestions shown in a dropdown overlay below it.
 * The dropdown floats above other content (it does not shift the layout) and closes
 * when a suggestion is chosen or the user taps elsewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    geocoder: CityGeocoder,
    modifier: Modifier = Modifier
) {
    var suggestions by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    // Skip the search that follows a suggestion selection, so picking one doesn't reopen.
    var suppressSearch by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        if (suppressSearch) {
            suppressSearch = false
            expanded = false
            return@LaunchedEffect
        }
        if (value.trim().length < MIN_QUERY_LENGTH) {
            suggestions = emptyList()
            expanded = false
            return@LaunchedEffect
        }
        delay(SEARCH_DEBOUNCE_MS)
        suggestions = geocoder.search(value, maxResults = MAX_SUGGESTIONS)
        expanded = suggestions.isNotEmpty()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion.name) },
                    onClick = {
                        suppressSearch = true
                        onValueChange(suggestion.name)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}




