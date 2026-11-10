package com.gtarena.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gtarena.data.AppItem
import com.gtarena.data.GameRepository
import com.gtarena.data.RootShell
import com.gtarena.data.SettingsStore
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
    private val store = SettingsStore.get(app)

    private val baseGames = MutableStateFlow(emptyList<GameEntry>())
    private val rootOk = MutableStateFlow(true)

    private val games = combine(baseGames, store.gameConfigs) { list, configs ->
        list.map { game ->
            val cfg = configs[game.pkg] ?: return@map game
            game.copy(profile = cfg.profile, refreshHz = cfg.refreshHz, focus = cfg.focus, bypassCharge = cfg.bypassCharge)
        }
    }

    val state: StateFlow<HomeState> =
        combine(rootOk, store.profile, telemetry.stats(), games, ::HomeState).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeState(true, Profile.SEIMBANG, DeviceStats(0, 0, 0, 0, 0f, 0f, 0, false), emptyList())
        )

    init {
        viewModelScope.launch { rootOk.value = RootShell.isAvailable() }
        viewModelScope.launch {
            store.extraGames.collect { baseGames.value = gameRepo.installedGames() }
        }
    }

    fun selectProfile(p: Profile) {
        store.setProfile(p)
    }

    fun launch(game: GameEntry) {
        gameRepo.launch(game.pkg)
    }

    suspend fun otherApps(): List<AppItem> = gameRepo.otherApps()
}
