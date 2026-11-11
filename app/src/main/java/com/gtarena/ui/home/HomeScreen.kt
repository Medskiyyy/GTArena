package com.gtarena.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.GameEntry
import com.gtarena.model.HomeState
import com.gtarena.model.Profile
import com.gtarena.model.sampleHomeState
import com.gtarena.ui.home.components.GameCard
import com.gtarena.ui.home.components.HeroPanel
import com.gtarena.ui.home.components.LoadRow
import com.gtarena.ui.home.components.ProfileSelector
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.intro.Emblem
import com.gtarena.ui.intro.IntroTiming
import com.gtarena.ui.intro.LocalIntro
import com.gtarena.ui.intro.Wordmark
import com.gtarena.ui.intro.introEnter
import com.gtarena.ui.theme.GTArenaTheme
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.ProfileGaming
import com.gtarena.ui.theme.ProfileHemat
import com.gtarena.ui.theme.ProfileSeimbang
import com.gtarena.ui.theme.SectionTitle
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.Warn
import com.gtarena.ui.theme.color

@Composable
fun HomeScreen(
    state: HomeState,
    onProfileSelected: (Profile) -> Unit,
    onLaunchGame: (GameEntry) -> Unit,
    onEditGame: (GameEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAllGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = state.profile.color
    val side = Modifier.padding(horizontal = 20.dp)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
        ) {
            item {
                Header(accent = accent, onOpenSettings = onOpenSettings, modifier = side.padding(bottom = 10.dp))
            }
            if (!state.rootOk) {
                item {
                    Text(
                        text = "Root mati. Profil nggak bisa diterapin.",
                        color = Warn,
                        fontSize = 13.sp,
                        modifier = side.padding(bottom = 10.dp)
                    )
                }
            }
            item {
                HeroPanel(state = state, modifier = side.padding(bottom = 10.dp))
            }
            item {
                ProfileSelector(
                    selected = state.profile,
                    onSelected = onProfileSelected,
                    showDescription = false,
                    modifier = side.padding(bottom = 10.dp)
                )
            }
            item {
                LoadRow(stats = state.stats, accent = accent, modifier = side.padding(bottom = 14.dp))
            }
            item {
                GameHeader(
                    count = state.games.size,
                    accent = accent,
                    onSeeAll = onSeeAllGames,
                    modifier = side.introEnter(IntroTiming.GAME_HEADER_START, IntroTiming.GAME_DUR, dx = 16.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(state.games.take(8), key = { _, game -> game.pkg }) { index, game ->
                        GameCard(
                            game = game,
                            onLaunch = { onLaunchGame(game) },
                            onEdit = { onEditGame(game) },
                            modifier = Modifier.introEnter(
                                IntroTiming.GAME_ROW_START + IntroTiming.GAME_STEP * minOf(index, 3),
                                IntroTiming.GAME_DUR,
                                dx = 16.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(accent: Color, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val intro = LocalIntro.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .onGloballyPositioned { intro.logoPos = it.positionInRoot() }
                .graphicsLayer { alpha = if (intro.logoShown) 1f else 0f },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Emblem(accent)
            Wordmark()
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(role = Role.Button, onClick = onOpenSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = GTIcons.Settings,
                contentDescription = "Setelan",
                tint = TextPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun GameHeader(count: Int, accent: Color, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SectionTitle(text = "Game", accent = accent)
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onSeeAll),
            contentAlignment = Alignment.CenterEnd
        ) {
            Label(text = "Semua ($count)", color = TextSoft, fontSize = 12.sp)
        }
    }
}

@Preview(widthDp = 360, heightDp = 800, showBackground = true, backgroundColor = 0xFF060607)
@Composable
private fun HomeScreenPreview() {
    GTArenaTheme {
        HomeScreen(
            state = sampleHomeState,
            onProfileSelected = {},
            onLaunchGame = {},
            onEditGame = {},
            onOpenSettings = {},
            onSeeAllGames = {}
        )
    }
}
