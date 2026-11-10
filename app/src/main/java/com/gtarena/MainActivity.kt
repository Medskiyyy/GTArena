package com.gtarena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gtarena.model.sampleHomeState
import com.gtarena.ui.home.HomeScreen
import com.gtarena.ui.theme.GTArenaTheme
import com.gtarena.ui.theme.Ground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GTArenaTheme {
                var state by remember { mutableStateOf(sampleHomeState) }
                HomeScreen(
                    state = state,
                    onProfileSelected = { state = state.copy(profile = it) },
                    onLaunchGame = {},
                    onOpenSettings = {},
                    onSeeAllGames = {},
                    modifier = Modifier.fillMaxSize().background(Ground)
                )
            }
        }
    }
}
