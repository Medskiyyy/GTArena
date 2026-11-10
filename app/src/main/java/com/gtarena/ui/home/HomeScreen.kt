package com.gtarena.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.GameEntry
import com.gtarena.model.HomeState
import com.gtarena.model.Profile
import com.gtarena.model.sampleHomeState
import com.gtarena.ui.home.components.GameRow
import com.gtarena.ui.home.components.ProfileSelector
import com.gtarena.ui.home.components.StatTile
import com.gtarena.ui.home.components.TempGauge
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.theme.GTArenaTheme
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.Lime
import com.gtarena.ui.theme.Panel
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.Warn
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        item {
            Header(
                rootOk = state.rootOk,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        item {
            GaugeRow(state, modifier = Modifier.padding(bottom = 16.dp))
        }
        item {
            ProfileSelector(
                selected = state.profile,
                onSelected = onProfileSelected,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        item {
            StatRow(state, modifier = Modifier.padding(bottom = 10.dp))
        }
        item {
            GameHeader(count = state.games.size, onSeeAll = onSeeAllGames)
        }
        items(state.games, key = { it.pkg }) { game ->
            GameRow(
                game = game,
                onLaunch = { onLaunchGame(game) },
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun Header(rootOk: Boolean, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "GT ARENA",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RootChip(rootOk)
            val shape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(shape)
                    .background(Panel)
                    .border(1.dp, Hairline, shape)
                    .clickable(role = Role.Button, onClick = onOpenSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = GTIcons.Settings,
                    contentDescription = "Setelan",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RootChip(rootOk: Boolean) {
    Row(
        modifier = Modifier
            .border(1.dp, Hairline, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(if (rootOk) Lime else Warn, CircleShape)
        )
        Text(
            text = if (rootOk) "Root aktif" else "Root mati",
            color = TextSoft,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SideStat(value: String, label: String, align: TextAlign) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
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
private fun GaugeRow(state: HomeState, modifier: Modifier = Modifier) {
    val stats = state.stats
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.width(70.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SideStat("${stats.batteryTempC}°", "Baterai", TextAlign.Start)
            SideStat("${stats.batteryPct}%", "Daya", TextAlign.Start)
        }
        TempGauge(tempC = stats.cpuTempC)
        Column(
            modifier = Modifier.width(70.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SideStat("${stats.refreshHz}", "Hz layar", TextAlign.End)
            SideStat(String.format(IdLocale, "%.1f", stats.ramUsedGb), "GB RAM", TextAlign.End)
        }
    }
}

@Composable
private fun StatRow(state: HomeState, modifier: Modifier = Modifier) {
    val stats = state.stats
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatTile(
            label = "CPU besar",
            value = String.format(IdLocale, "%.2f", stats.bigCoreGhz),
            unit = "GHz",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Beban GPU",
            value = "${stats.gpuLoadPct}",
            unit = "%",
            modifier = Modifier.weight(1f)
        )
        StatTile(
            label = "Cas",
            value = if (stats.charging) "Ngecas" else "Lepas",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GameHeader(count: Int, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "GAME",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .clickable(role = Role.Button, onClick = onSeeAll),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(text = "Semua ($count)", color = Lime, fontSize = 14.sp)
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
