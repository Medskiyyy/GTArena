package com.gtarena.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gtarena.data.RootShell
import com.gtarena.data.SettingsStore
import com.gtarena.ui.ScreenTitle
import com.gtarena.ui.tabInsets
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.Warn

@Composable
fun SettingsTab(accent: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val autoApply by store.autoApply.collectAsStateWithLifecycle()
    val overlay by store.overlayEnabled.collectAsStateWithLifecycle()
    val root by produceState<Boolean?>(initialValue = null) { value = RootShell.isAvailable() }
    val version = remember { appVersion(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ground)
            .tabInsets()
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenTitle("SETELAN", modifier = Modifier.padding(bottom = 14.dp))
            SettingRow(
                title = "Terapin otomatis pas game dibuka",
                checked = autoApply,
                accent = accent,
                onCheckedChange = store::setAutoApply
            )
            SettingRow(
                title = "Overlay di dalam game",
                checked = overlay,
                accent = accent,
                onCheckedChange = store::setOverlayEnabled
            )
            Column(modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Akses root",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = when (root) {
                            null -> "Ngecek..."
                            true -> "Aktif"
                            false -> "Mati"
                        },
                        color = if (root == false) Warn else TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (root == false) {
                    Text(text = "Nyalain GT Arena di KernelSU manager", color = TextSecondary, fontSize = 13.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(GaugeTrack))
        }
        Text(
            text = "GT Arena $version",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String = try {
    "v" + context.packageManager.getPackageInfo(context.packageName, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    ""
}
