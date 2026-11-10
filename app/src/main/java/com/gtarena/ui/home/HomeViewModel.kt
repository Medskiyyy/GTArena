package com.gtarena.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gtarena.data.GameRepository
import com.gtarena.data.RootShell
import com.gtarena.data.TelemetryRepository
import com.gtarena.model.DeviceStats
import com.gtarena.model.GameEntry
import com.gtarena.model.HomeState
import com.gtarena.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val telemetry = TelemetryRepository(app)
    private val gameRepo = GameRepository(app)

    private val profile = MutableStateFlow(Profile.SEIMBANG)
    private val games = MutableStateFlow(emptyList<GameEntry>())
    private val rootOk = MutableStateFlow(true)

    val state: StateFlow<HomeState> =
        combine(rootOk, profile, telemetry.stats(), games, ::HomeState).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeState(true, Profile.SEIMBANG, DeviceStats(0, 0, 0, 0, 0f, 0f, 0, false), emptyList())
        )

    init {
        viewModelScope.launch { rootOk.value = RootShell.isAvailable() }
        viewModelScope.launch { games.value = gameRepo.installedGames() }
    }

    fun selectProfile(p: Profile) {
        profile.value = p
    }

    fun launch(game: GameEntry) {
        gameRepo.launch(game.pkg)
    }
}
