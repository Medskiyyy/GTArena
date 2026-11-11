package com.gtarena.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtarena.data.GameConfig
import com.gtarena.data.SettingsStore
import com.gtarena.model.GameEntry
import com.gtarena.ui.home.components.ProfileSelector
import com.gtarena.ui.settings.SettingRow
import com.gtarena.ui.tabInsets
import com.gtarena.ui.theme.AppIcon
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.SlantTab
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.color

private val RefreshOptions = listOf(60, 90, 120)
private val ThermalLabels = listOf("Normal", "Longgar", "Lepas")
private val ThermalCaptions = listOf(
    "Bawaan HP. Performa diturunin pas bodi 45-48°C.",
    "Performa baru diturunin pas bodi 53-56°C. Balik normal kalau baterai nyentuh 46°C.",
    "Suhu bodi diabaikan, CPU boleh sampe 95°C. HP bisa panas banget dan baterai lebih cepet aus. " +
        "Balik normal kalau baterai nyentuh 50°C. Pengaman mati paksa bawaan HP tetep jalan."
)

@Composable
fun BackLink(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clickable(role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.CenterStart
    ) {
        Label(text = "‹  Kembali", fontSize = 12.sp)
    }
}

@Composable
fun GameEditor(game: GameEntry, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val configs by store.gameConfigs.collectAsStateWithLifecycle()
    val cfg = configs[game.pkg] ?: GameConfig()
    val accent = cfg.profile.color
    fun save(next: GameConfig) = store.setGameConfig(game.pkg, next)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .tabInsets()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BackLink(onBack)
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppIcon(pkg = game.pkg, modifier = Modifier.size(44.dp).clip(ChamferShape(9.dp)))
            Text(
                text = game.label,
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SectionLabel("Profil")
        ProfileSelector(selected = cfg.profile, onSelected = { save(cfg.copy(profile = it)) })
        SectionLabel("Refresh rate")
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup().padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            RefreshOptions.forEach { hz ->
                SlantTab(
                    text = "$hz Hz",
                    active = hz == cfg.refreshHz,
                    color = accent,
                    onClick = { save(cfg.copy(refreshHz = hz)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        SettingRow(
            title = "Mode fokus",
            caption = "Jangan Ganggu nyala, kecerahan dikunci, RAM dibersihin pas game mulai",
            checked = cfg.focus,
            accent = accent,
            onCheckedChange = { save(cfg.copy(focus = it)) }
        )
        SettingRow(
            title = "Bypass cas",
            caption = "Pas dicolok, HP nyala dari charger dan baterai nggak diisi selama main",
            checked = cfg.bypassCharge,
            accent = accent,
            onCheckedChange = { save(cfg.copy(bypassCharge = it)) }
        )
        SettingRow(
            title = "Crosshair",
            caption = "Titik bidik di tengah layar langsung nyala pas game dibuka",
            checked = cfg.crosshair,
            accent = accent,
            onCheckedChange = { save(cfg.copy(crosshair = it)) }
        )
        SectionLabel("Batas panas")
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ThermalLabels.forEachIndexed { level, label ->
                SlantTab(
                    text = label,
                    active = level == cfg.thermalLevel,
                    color = accent,
                    onClick = { save(cfg.copy(thermalLevel = level)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            text = ThermalCaptions[cfg.thermalLevel],
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Label(text = text, modifier = Modifier.padding(top = 10.dp, bottom = 8.dp))
}
