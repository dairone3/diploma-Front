package com.example.dimadyplom2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.dimadyplom2.ui.AuthGate
import com.example.dimadyplom2.ui.theme.DimaDyplom2Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DimaDyplom2Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Auth gate is shown over everything else; the rest of the app
                    // (AppNavigation) stays inaccessible until auth is wired through.
                    AuthGate(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}