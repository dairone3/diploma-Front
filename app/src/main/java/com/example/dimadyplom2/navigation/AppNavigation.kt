package com.example.dimadyplom2.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dimadyplom2.api.ApiFactory
import com.example.dimadyplom2.data.AppStorage
import com.example.dimadyplom2.data.FileAppStorage
import com.example.dimadyplom2.services.UserService
import com.example.dimadyplom2.trip.Trip
import com.example.dimadyplom2.ui.AccountScreen
import com.example.dimadyplom2.ui.HomeScreen
import com.example.dimadyplom2.ui.MyTripsScreen
import com.example.dimadyplom2.ui.MyTripsState
import com.example.dimadyplom2.ui.CreateTripScreen
import com.example.dimadyplom2.ui.TripScreen

private const val HOME_ROUTE = "home"
private const val NEW_TRIP_ROUTE = "new_trip"
private const val MY_TRIPS_GRAPH_ROUTE = "my_trips_graph"
private const val MY_TRIPS_ROUTE = "my_trips"
private const val TRIP_ROUTE = "trip"
private const val ACCOUNT_ROUTE = "account"
private const val ARG_ID = "id"
private const val ARG_DESTINATION = "destination"

// Full New trip route pattern (the destination arg is optional).
private const val NEW_TRIP_ROUTE_PATTERN = "$NEW_TRIP_ROUTE?$ARG_DESTINATION={$ARG_DESTINATION}"

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
    BottomTab(HOME_ROUTE, HOME_ROUTE, "Home", Icons.Filled.Home),
    BottomTab(NEW_TRIP_ROUTE_PATTERN, NEW_TRIP_ROUTE, "Create", Icons.Filled.Add),
    BottomTab(MY_TRIPS_GRAPH_ROUTE, MY_TRIPS_ROUTE, "My Trips", Icons.AutoMirrored.Filled.List),
    BottomTab(ACCOUNT_ROUTE, ACCOUNT_ROUTE, "Account", Icons.Filled.Person)
)

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    // The only place tied to a concrete storage. Swap FileAppStorage for a
    // database-backed AppStorage here and nothing else changes.
    val storage: AppStorage = remember { FileAppStorage(context.filesDir) }
    val trips = storage.trips
    val userService = remember { UserService(ApiFactory.createAuthApi()) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun tripRoute(trip: Trip): String {
        return "$TRIP_ROUTE/${trip.id}"
    }

    // Opens an existing trip (from the trips list). The trip is already saved, so
    // this only navigates.
    fun openTrip(trip: Trip) {
        navController.navigate(tripRoute(trip))
    }

    // Creates a trip and opens its map, removing the New trip screen from the back
    // stack so Back returns to wherever the user started (e.g. Home).
    fun createTrip(trip: Trip) {
        trips.upsert(trip)
        navController.navigate(tripRoute(trip)) {
            popUpTo(NEW_TRIP_ROUTE_PATTERN) { inclusive = true }
            launchSingleTop = true
        }
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
                                // Reset to the tab's root; keep Home at the base of the
                                // back stack. No save/restore so tabs never reopen a
                                // previously created trip.
                                popUpTo(HOME_ROUTE)
                                launchSingleTop = true
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
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home is a scrollable landing screen; its "Create trip" button opens
            // the New trip screen.
            composable(HOME_ROUTE) {
                HomeScreen(
                    onCreateTrip = { navController.navigate(NEW_TRIP_ROUTE) }
                )
            }

            composable(ACCOUNT_ROUTE) {
                AccountScreen(onSignOut = { userService.signOut() })
            }

            composable(
                route = NEW_TRIP_ROUTE_PATTERN,
                arguments = listOf(
                    navArgument(ARG_DESTINATION) {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                CreateTripScreen(
                    initialDestination = backStackEntry.arguments?.getString(ARG_DESTINATION).orEmpty(),
                    onCreateTrip = { trip -> createTrip(trip) }
                )
            }

            navigation(
                startDestination = MY_TRIPS_ROUTE,
                route = MY_TRIPS_GRAPH_ROUTE
            ) {
                composable(MY_TRIPS_ROUTE) { backStackEntry ->
                    // Screen logic lives in MyTripsState; reload when entering the screen.
                    val myTripsState = remember(backStackEntry) { MyTripsState(storage) }
                    LaunchedEffect(backStackEntry) { myTripsState.load() }
                    MyTripsScreen(
                        state = myTripsState,
                        onTripSelected = { trip -> openTrip(trip) }
                    )
                }

                // Trip detail is addressed by id only; TripScreen looks up the trip's
                // name/coordinates from the repository.
                composable(
                    route = "$TRIP_ROUTE/{$ARG_ID}",
                    arguments = listOf(
                        navArgument(ARG_ID) { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val args = backStackEntry.arguments
                    TripScreen(
                        tripId = args?.getString(ARG_ID).orEmpty(),
                        storage = storage
                    )
                }
            }
        }
    }
}
