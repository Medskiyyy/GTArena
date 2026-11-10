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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.GameEntry
import com.gtarena.model.HomeState
import com.gtarena.model.Profile
import com.gtarena.model.label
import com.gtarena.model.sampleHomeState
import com.gtarena.ui.home.components.GameRow
import com.gtarena.ui.home.components.ProfileSelector
import com.gtarena.ui.home.components.TelemetryItem
import com.gtarena.ui.home.components.TelemetryStrip
import com.gtarena.ui.home.components.TempGauge
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.intro.IntroTiming
import com.gtarena.ui.intro.LocalIntro
import com.gtarena.ui.intro.Wordmark
import com.gtarena.ui.intro.introEnter
import com.gtarena.ui.theme.GTArenaTheme
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.ProfileGaming
import com.gtarena.ui.theme.ProfileHemat
import com.gtarena.ui.theme.ProfileSeimbang
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.Warn
import com.gtarena.ui.theme.color
import java.util.Locale

private val IdLocale: Locale = Locale.forLanguageTag("id-ID")

@Composable
fun HomeScreen(
    state: HomeState,
    onProfileSelected: (Profile) -> Unit,
    onLaunchGame: (GameEntry) -> Unit,
    onOpenSettings: () -> Unit,
    onSeeAllGames: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = state.profile.color
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)) {
            item {
                Header(onOpenSettings = onOpenSettings, modifier = Modifier.padding(bottom = 14.dp))
            }
            if (!state.rootOk) {
                item {
                    Text(
                        text = "Root mati. Profil nggak bisa diterapin.",
                        color = Warn,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }
            item {
                GaugeRow(state, accent, modifier = Modifier.padding(bottom = 14.dp))
            }
            item {
                ProfileSelector(
                    selected = state.profile,
                    onSelected = onProfileSelected,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
            }
            item {
                TelemetryStrip(telemetryItems(state), modifier = Modifier.padding(bottom = 14.dp))
            }
            item {
                GameHeader(
                    count = state.games.size,
                    onSeeAll = onSeeAllGames,
                    modifier = Modifier.introEnter(IntroTiming.GAME_HEADER_START, IntroTiming.GAME_DUR, dx = 16.dp)
                )
            }
            itemsIndexed(state.games.take(3), key = { _, game -> game.pkg }) { index, game ->
                GameRow(
                    game = game,
                    accent = accent,
                    primary = index == 0,
                    showDivider = index < minOf(state.games.size, 3) - 1,
                    onLaunch = { onLaunchGame(game) },
                    modifier = Modifier.introEnter(
                        IntroTiming.GAME_ROW_START + IntroTiming.GAME_STEP * index,
                        IntroTiming.GAME_DUR,
                        dx = 16.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
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
            Emblem()
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
private fun Emblem() {
    Canvas(modifier = Modifier.size(width = 21.dp, height = 20.dp)) {
        val barW = 5.dp.toPx()
        val gap = 3.dp.toPx()
        val h = 20.dp.toPx()
        val shift = (h / 2f) * 0.3249197f
        listOf(ProfileHemat, ProfileSeimbang, ProfileGaming).forEachIndexed { i, color ->
            val x = i * (barW + gap)
            val path = Path().apply {
                moveTo(x + shift, 0f)
                lineTo(x + barW + shift, 0f)
                lineTo(x + barW - shift, h)
                lineTo(x - shift, h)
                close()
            }
            drawPath(path, color)
        }
    }
}

@Composable
private fun SideStat(value: String, label: String, align: TextAlign, index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .introEnter(IntroTiming.STAT_START + IntroTiming.STAT_STEP * index, IntroTiming.STAT_DUR, dy = 12.dp)
    ) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 26.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.Bold,
            textAlign = align,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = align,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GaugeRow(state: HomeState, accent: Color, modifier: Modifier = Modifier) {
    val stats = state.stats
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(204.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SideStat("${stats.batteryTempC}°", "Baterai", TextAlign.Start, 0)
            SideStat("${stats.batteryPct}%", "Daya", TextAlign.Start, 1)
        }
        TempGauge(tempC = stats.cpuTempC, accent = accent, profileName = state.profile.label)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SideStat("${stats.refreshHz}", "Hz layar", TextAlign.End, 2)
            SideStat(String.format(IdLocale, "%.1f", stats.ramUsedGb), "GB RAM", TextAlign.End, 3)
        }
    }
}

private fun telemetryItems(state: HomeState): List<TelemetryItem> {
    val stats = state.stats
    return listOf(
        TelemetryItem("CPU besar", String.format(IdLocale, "%.2f", stats.bigCoreGhz), "GHz"),
        TelemetryItem("Beban GPU", "${stats.gpuLoadPct}", "%"),
        TelemetryItem("Cas", if (stats.charging) "Ngecas" else "Lepas")
    )
}

@Composable
private fun GameHeader(count: Int, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "GAME",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onSeeAll),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(text = "Semua ($count)", color = TextPrimary, fontSize = 14.sp)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
private fun HomeScreenPreview() {
    GTArenaTheme {
        HomeScreen(
            state = sampleHomeState,
            onProfileSelected = {},
            onLaunchGame = {},
            onOpenSettings = {},
            onSeeAllGames = {}
        )
    }
}
