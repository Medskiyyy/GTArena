package com.gtarena.ui.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gtarena.data.RootShell
import com.gtarena.data.SettingsStore
import com.gtarena.ui.ScreenTitle
import com.gtarena.ui.settings.SettingRow
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.SlantShape
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.Warn
import com.gtarena.ui.theme.color
import kotlinx.coroutines.launch

@Composable
fun OverlayTab() {
    val context = LocalContext.current
    val settings = remember { SettingsStore.get(context) }
    val enabled by settings.overlayEnabled.collectAsState()
    val bubble by settings.fpsBubble.collectAsState()
    val profile by settings.profile.collectAsState()
    val accent = profile.color
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = Settings.canDrawOverlays(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)
    ) {
        ScreenTitle("Overlay", modifier = Modifier.padding(bottom = 14.dp))
        SettingRow(
            title = "Overlay pas main",
            caption = "Garis tipis di tepi layar. Ketuk buat buka panel kontrol, geser buat mindahin.",
            checked = enabled,
            accent = accent,
            onCheckedChange = settings::setOverlayEnabled
        )
        SettingRow(
            title = "Bubble FPS",
            caption = "Angka FPS kecil yang selalu keliatan pas main. Bisa digeser ke mana aja.",
            checked = bubble,
            accent = accent,
            onCheckedChange = settings::setFpsBubble
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tampil di atas app lain",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (granted) "Udah diizinin" else "Belum diizinin, overlay nggak bakal muncul",
                    color = if (granted) TextSecondary else Warn,
                    fontSize = 13.sp
                )
            }
            if (!granted) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .height(44.dp)
                        .clip(SlantShape(8.dp))
                        .background(accent)
                        .clickable(role = Role.Button) {
                            scope.launch {
                                RootShell.run("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
                                granted = Settings.canDrawOverlays(context)
                                if (!granted) openOverlaySettings(context)
                            }
                        }
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Label(text = "Izinin", color = OnLime, fontSize = 13.sp)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(GaugeTrack))
        Label(
            text = "Isi panel",
            modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
        )
        Text(
            text = "FPS dan grafiknya, suhu CPU, beban GPU, suhu baterai, ganti profil, kecerahan, " +
                "crosshair, refresh rate, mode fokus, bypass cas, kunci terang, screenshot, " +
                "rekam layar (maks 3 menit, tanpa suara), dan bersihin RAM.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
