package com.example.dimadyplom2.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.dimadyplom2.navigation.AppNavigation
import com.example.dimadyplom2.services.SessionUserStorage

/**
 * Auth gate: decides what the user sees based on the session. Signed out -> [LoginScreen];
 * signed in -> the app ([AppNavigation]). Session state comes from [SessionUserStorage]'s
 * login/logout events, so signing in or out automatically swaps the screen.
 */
@Composable
fun AuthGate(modifier: Modifier = Modifier) {
    var signedInId by remember { mutableStateOf(SessionUserStorage.user?.id) }

    // React to login/logout events from SessionUserStorage.
    LaunchedEffect(Unit) {
        SessionUserStorage.onSet.collect { user -> signedInId = user.id }
    }
    LaunchedEffect(Unit) {
        SessionUserStorage.onClear.collect { signedInId = null }
    }

    if (signedInId == null) {
        LoginScreen(modifier)
    } else {
        AppNavigation(modifier)
    }
}



