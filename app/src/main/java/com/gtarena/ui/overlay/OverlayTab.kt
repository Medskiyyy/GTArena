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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.gtarena.ui.settings.GTSwitch
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.OnLime
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
        Box(modifier = Modifier.height(44.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = "OVERLAY",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }

        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Overlay pas main", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Tampil FPS, suhu, dan panel kontrol di atas game.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .heightIn(min = 44.dp)
                    .toggleable(value = enabled, role = Role.Switch, onValueChange = { settings.setOverlayEnabled(it) }),
                contentAlignment = Alignment.Center
            ) {
                GTSwitch(checked = enabled, accent = accent)
            }
        }

        Hairline()
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
                    fontWeight = FontWeight.SemiBold
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
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent)
                        .clickable(role = Role.Button) {
                            scope.launch {
                                RootShell.run("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
                                granted = Settings.canDrawOverlays(context)
                                if (!granted) openOverlaySettings(context)
                            }
                        }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Izinin", color = OnLime, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Hairline()
        Text(
            text = "Contoh tampilan",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
        )
        OverlayPill(fps = 58, cpuTemp = 44, batteryPct = 12, accent = accent)
    }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
}

private fun openOverlaySettings(context: Context) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
