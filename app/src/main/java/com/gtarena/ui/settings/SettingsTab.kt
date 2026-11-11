package com.gtarena.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtarena.data.RootShell
import com.gtarena.data.SettingsStore
import com.gtarena.service.ArenaService
import com.gtarena.ui.ScreenTitle
import com.gtarena.ui.tabInsets
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.Warn
import com.gtarena.ui.theme.mechaPanel

private val PanelShape = ChamferShape(14.dp)

@Composable
fun SettingsTab(accent: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val autoApply by store.autoApply.collectAsStateWithLifecycle()
    val overlay by store.overlayEnabled.collectAsStateWithLifecycle()
    val introSound by store.introSound.collectAsStateWithLifecycle()
    val running by ArenaService.running.collectAsStateWithLifecycle()
    val root by produceState<Boolean?>(initialValue = null) { value = RootShell.isAvailable() }
    val version = remember { appVersion(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .tabInsets()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        ScreenTitle("Setelan", modifier = Modifier.padding(bottom = 14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .mechaPanel(PanelShape, accent)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            StatusCell(
                label = "Root",
                value = when (root) {
                    null -> "Ngecek"
                    true -> "Aktif"
                    false -> "Mati"
                },
                warn = root == false,
                modifier = Modifier.weight(1f)
            )
            StatusCell(
                label = "Layanan",
                value = if (running) "Jalan" else "Mati",
                warn = !running,
                modifier = Modifier.weight(1f)
            )
            StatusCell(label = "Versi", value = version, warn = false, modifier = Modifier.weight(1f))
        }
        if (root == false) {
            Text(
                text = "Nyalain GT Arena di KernelSU manager",
                color = Warn,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Label(text = "Otomasi", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .mechaPanel(PanelShape)
                .padding(horizontal = 16.dp)
        ) {
            SettingRow(
                title = "Terapin otomatis pas game dibuka",
                caption = "Profil dan setelan per game dipasang begitu game kebuka, balik lagi pas keluar.",
                checked = autoApply,
                accent = accent,
                onCheckedChange = store::setAutoApply
            )
            SettingRow(
                title = "Overlay di dalam game",
                caption = "Garis tepi, panel kontrol, dan bubble FPS.",
                checked = overlay,
                accent = accent,
                onCheckedChange = store::setOverlayEnabled,
                showDivider = false
            )
        }
        Label(text = "Tampilan", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .mechaPanel(PanelShape)
                .padding(horizontal = 16.dp)
        ) {
            SettingRow(
                title = "Suara intro",
                caption = "Efek suara pas animasi pembuka. Ngikutin volume media.",
                checked = introSound,
                accent = accent,
                onCheckedChange = store::setIntroSound,
                showDivider = false
            )
        }
    }
}

@Composable
private fun StatusCell(label: String, value: String, warn: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Label(label, fontSize = 10.sp)
        Text(
            text = value,
            color = if (warn) Warn else TextPrimary,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String = try {
    "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    ""
}
