package com.example.dimadyplom2.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dimadyplom2.trip.Trip
import com.example.dimadyplom2.trip.tripRepository
import com.example.dimadyplom2.map.repository.deleteTripStorage
import com.example.dimadyplom2.ui.CitySearchScreen
import com.example.dimadyplom2.ui.MyTripsScreen
import com.example.dimadyplom2.ui.TripScreen

private const val CITY_SEARCH_ROUTE = "city_search"
private const val MY_TRIPS_GRAPH_ROUTE = "my_trips_graph"
private const val MY_TRIPS_ROUTE = "my_trips"
private const val TRIP_ROUTE = "trip"
private const val ARG_ID = "id"
private const val ARG_NAME = "name"
private const val ARG_LAT = "lat"
private const val ARG_LNG = "lng"

private data class BottomTab(
    /**
     * Route used to determine whether this tab is selected.
     *
     * For graph tabs, this can be the parent graph route so child screens keep the tab highlighted.
     */
    val route: String,
    /**
     * Route navigated to when the user taps this tab.
     *
     * This is usually the start destination screen for the tab.
     */
    val openRoute: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(CITY_SEARCH_ROUTE, CITY_SEARCH_ROUTE, "Search", Icons.Filled.Search),
    BottomTab(MY_TRIPS_GRAPH_ROUTE, MY_TRIPS_ROUTE, "My Trips", Icons.AutoMirrored.Filled.List)
)

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val trips = remember { tripRepository(context.filesDir) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun tripRoute(trip: Trip): String {
        val encodedName = Uri.encode(trip.name)
        return "$TRIP_ROUTE/${trip.id}/$encodedName/${trip.latitude}/${trip.longitude}"
    }

    // Opens a trip within the My Trips back stack (used from the trips list).
    fun openTrip(trip: Trip) {
        trips.add(trip)
        navController.navigate(tripRoute(trip))
    }

    // Creates a trip from Search: switches to the My Trips tab (saving the Search
    // screen state so it stays as it was) and opens the new trip's map there.
    fun createTripFromSearch(trip: Trip) {
        trips.add(trip)
        navController.navigate(MY_TRIPS_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        navController.navigate(tripRoute(trip))
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.openRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CITY_SEARCH_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(CITY_SEARCH_ROUTE) {
                CitySearchScreen(
                    onCitySelected = { city ->
                        createTripFromSearch(
                            Trip(
                                name = city.name,
                                latitude = city.latitude,
                                longitude = city.longitude
                            )
                        )
                    }
                )
            }

            navigation(
                startDestination = MY_TRIPS_ROUTE,
                route = MY_TRIPS_GRAPH_ROUTE
            ) {
                composable(MY_TRIPS_ROUTE) { backStackEntry ->
                    var savedTrips by remember(backStackEntry) {
                        mutableStateOf(trips.getAll())
                    }
                    MyTripsScreen(
                        trips = savedTrips,
                        onTripSelected = { trip -> openTrip(trip) },
                        onTripDeleted = { trip ->
                            trips.delete(trip.id)
                            deleteTripStorage(context.filesDir, trip.id)
                            savedTrips = trips.getAll()
                        }
                    )
                }

                composable(
                    route = "$TRIP_ROUTE/{$ARG_ID}/{$ARG_NAME}/{$ARG_LAT}/{$ARG_LNG}",
                    arguments = listOf(
                        navArgument(ARG_ID) { type = NavType.StringType },
                        navArgument(ARG_NAME) { type = NavType.StringType },
                        navArgument(ARG_LAT) { type = NavType.FloatType },
                        navArgument(ARG_LNG) { type = NavType.FloatType }
                    )
                ) { backStackEntry ->
                    val args = backStackEntry.arguments
                    TripScreen(
                        tripId = args?.getString(ARG_ID).orEmpty(),
                        cityName = args?.getString(ARG_NAME).orEmpty(),
                        latitude = (args?.getFloat(ARG_LAT) ?: 0f).toDouble(),
                        longitude = (args?.getFloat(ARG_LNG) ?: 0f).toDouble()
                    )
                }
            }
        }
    }
}
