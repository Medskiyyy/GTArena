package com.gtarena.ui.games

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
import com.gtarena.ui.home.components.CutBottomEndShape
import com.gtarena.ui.home.components.ProfileSelector
import com.gtarena.ui.settings.SettingRow
import com.gtarena.ui.tabInsets
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.Panel
import com.gtarena.ui.theme.TextInactive
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.color

private val RefreshOptions = listOf(60, 90, 120)

@Composable
fun BackLink(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clickable(role = Role.Button, onClick = onBack),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = "Kembali", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
        Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RefreshOptions.forEach { hz ->
                HzButton(
                    hz = hz,
                    active = hz == cfg.refreshHz,
                    accent = accent,
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
            caption = "Belum tersedia. Perlu dites dulu pas ngecas.",
            checked = false,
            accent = accent,
            enabled = false,
            onCheckedChange = {}
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun HzButton(hz: Int, active: Boolean, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(54.dp)
            .clip(if (active) CutBottomEndShape else RectangleShape)
            .background(if (active) accent else Panel)
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(accent))
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Text(
                text = "$hz Hz",
                color = if (active) OnLime else TextInactive,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
