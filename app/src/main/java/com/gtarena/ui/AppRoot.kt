package com.gtarena.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gtarena.ui.games.GameTab
import com.gtarena.ui.home.HomeScreen
import com.gtarena.ui.home.HomeViewModel
import com.gtarena.ui.home.components.BottomNav
import com.gtarena.ui.intro.IntroGate
import com.gtarena.ui.intro.IntroOverlay
import com.gtarena.ui.intro.IntroState
import com.gtarena.ui.intro.LocalIntro
import com.gtarena.ui.overlay.OverlayTab
import com.gtarena.ui.settings.SettingsTab
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.color
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(AppTab.ARENA) }
    var editingPkg by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val accent = state.profile.color
    val intro = remember { IntroState(IntroGate.claim(tab == AppTab.ARENA)) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(intro) { intro.play() }

    BackHandler(enabled = tab != AppTab.ARENA) {
        when {
            tab == AppTab.GAME && adding -> adding = false
            tab == AppTab.GAME && editingPkg != null -> editingPkg = null
            else -> tab = AppTab.ARENA
        }
    }

    CompositionLocalProvider(LocalIntro provides intro) {
        Box(
            modifier = Modifier.fillMaxSize().pointerInput(intro) {
                if (!intro.enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.any { it.changedToDown() }) scope.launch { intro.skip() }
                    }
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Ground)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        AppTab.ARENA -> HomeScreen(
                            state = state,
                            onProfileSelected = vm::selectProfile,
                            onLaunchGame = vm::launch,
                            onOpenSettings = { tab = AppTab.SETELAN },
                            onSeeAllGames = { tab = AppTab.GAME }
                        )
                        AppTab.GAME -> GameTab(
                            games = state.games,
                            accent = accent,
                            editingPkg = editingPkg,
                            onEdit = { editingPkg = it },
                            adding = adding,
                            onAdding = { adding = it },
                            onLaunch = vm::launch,
                            loadOtherApps = vm::otherApps
                        )
                        AppTab.OVERLAY -> Box(Modifier.tabInsets()) { OverlayTab() }
                        AppTab.SETELAN -> SettingsTab(accent = accent)
                    }
                }
                BottomNav(
                    selected = tab,
                    accent = accent,
                    onSelect = {
                        if (it == AppTab.GAME && tab == AppTab.GAME) {
                            editingPkg = null
                            adding = false
                        }
                        tab = it
                    }
                )
            }
            if (intro.enabled) IntroOverlay(intro, accent)
        }
    }
}
