package com.gtarena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gtarena.ui.home.HomeScreen
import com.gtarena.ui.home.HomeViewModel
import com.gtarena.ui.theme.GTArenaTheme
import com.gtarena.ui.theme.Ground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GTArenaTheme {
                val vm: HomeViewModel = viewModel()
                val state by vm.state.collectAsStateWithLifecycle()
                HomeScreen(
                    state = state,
                    onProfileSelected = vm::selectProfile,
                    onLaunchGame = vm::launch,
                    onOpenSettings = {},
                    onSeeAllGames = {},
                    modifier = Modifier.fillMaxSize().background(Ground)
                )
            }
        }
    }
}
