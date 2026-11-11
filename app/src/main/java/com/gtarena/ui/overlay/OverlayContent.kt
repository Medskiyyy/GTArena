package com.gtarena.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.data.FocusMode
import com.gtarena.data.FpsReader
import com.gtarena.data.TelemetryRepository
import com.gtarena.model.DeviceStats
import com.gtarena.model.Profile
import com.gtarena.model.label
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.Panel
import com.gtarena.ui.theme.Saira
import com.gtarena.ui.theme.TextInactive
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.color
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class CutShape(private val cutDp: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { cutDp.dp.toPx() }
        return Outline.Generic(
            Path().apply {
                moveTo(c, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - c)
                lineTo(size.width - c, size.height)
                lineTo(0f, size.height)
                lineTo(0f, c)
                close()
            }
        )
    }
}

internal const val PANEL_WIDTH_DP = 236
internal const val PANEL_HEIGHT_DP = 208
internal const val HANDLE_WIDTH_DP = 14
internal const val HANDLE_HEIGHT_DP = 56

@Composable
internal fun OverlayRoot(
    state: OverlayUiState,
    onProfileChange: (Profile) -> Unit,
    onBypassChange: (Boolean) -> Unit
) {
    val session = state.session ?: return
    val accent = session.profile.color
    if (!state.expanded) {
        OverlayHandle(accent = accent, onEnd = state.onEnd)
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<DeviceStats?>(null) }
    var fps by remember { mutableStateOf<Int?>(null) }
    val samples = remember { mutableStateListOf<Int>() }
    val focusOn by FocusMode.enabled.collectAsState()
    val brightnessLocked by FocusMode.brightnessLocked.collectAsState()

    LaunchedEffect(Unit) {
        FocusMode.refreshBrightnessLock()
    }
    LaunchedEffect(Unit) {
        TelemetryRepository(context).stats(1000L).collect { stats = it }
    }
    LaunchedEffect(session.pkg) {
        samples.clear()
        while (true) {
            val v = FpsReader.read(session.pkg)
            fps = v
            if (v != null) {
                samples.add(v)
                if (samples.size > 30) samples.removeAt(0)
            }
            delay(1000L)
        }
    }

    ProvideTextStyle(TextStyle(fontFamily = Saira, color = TextPrimary)) {
        OverlayPanel(
            accent = accent,
            profile = session.profile,
            fps = fps,
            samples = samples,
            stats = stats,
            focusOn = focusOn,
            bypassOn = session.bypass,
            brightnessLocked = brightnessLocked,
            onProfile = onProfileChange,
            onFocus = { scope.launch { if (focusOn) FocusMode.disable() else FocusMode.enable() } },
            onBypass = { onBypassChange(!session.bypass) },
            onBrightnessLock = { scope.launch { FocusMode.setBrightnessLocked(!brightnessLocked) } },
            onCleanRam = { scope.launch { FocusMode.cleanRam() } }
        )
    }
}

@Composable
internal fun OverlayHandle(accent: Color, onEnd: Boolean) {
    Box(
        modifier = Modifier.size(width = HANDLE_WIDTH_DP.dp, height = HANDLE_HEIGHT_DP.dp),
        contentAlignment = if (onEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .background(accent.copy(alpha = 0.75f))
        )
    }
}

@Composable
private fun OverlayPanel(
    accent: Color,
    profile: Profile,
    fps: Int?,
    samples: List<Int>,
    stats: DeviceStats?,
    focusOn: Boolean,
    bypassOn: Boolean,
    brightnessLocked: Boolean,
    onProfile: (Profile) -> Unit,
    onFocus: () -> Unit,
    onBypass: () -> Unit,
    onBrightnessLock: () -> Unit,
    onCleanRam: () -> Unit
) {
    val shape = remember { CutShape(10f) }
    Column(
        modifier = Modifier
            .width(PANEL_WIDTH_DP.dp)
            .clip(shape)
            .background(Ground.copy(alpha = 0.94f))
            .border(1.dp, Hairline, shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(accent)
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = fps?.toString() ?: "--",
                    color = accent,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "FPS / ${stats?.refreshHz ?: "--"} HZ",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp, bottom = 7.dp)
                )
                Sparkline(
                    samples = samples,
                    color = accent,
                    modifier = Modifier
                        .padding(start = 10.dp, bottom = 6.dp)
                        .weight(1f)
                        .height(26.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Stat("CPU", stats?.let { "${it.cpuTempC}°" } ?: "--")
                Stat("GPU", stats?.let { "${it.gpuLoadPct}%" } ?: "--")
                Stat("BAT", stats?.let { "${it.batteryTempC}°" } ?: "--")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Profile.values().forEach { p ->
                    OverlayProfileButton(p, p == profile, { onProfile(p) }, Modifier.weight(1f))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToggleButton("Mode fokus", focusOn, accent, onFocus, Modifier.weight(1f))
                ToggleButton("Bypass cas", bypassOn, accent, onBypass, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ToggleButton("Kunci terang", brightnessLocked, accent, onBrightnessLock, Modifier.weight(1f))
                ActionButton("Bersihin RAM", onCleanRam, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Sparkline(samples: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (samples.size < 2) return@Canvas
        val ceiling = max(60, samples.max()).toFloat()
        val step = size.width / 29f
        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = i * step
            val y = size.height * (1f - v.coerceAtLeast(0) / ceiling)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Miter))
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp, letterSpacing = 1.sp, maxLines = 1)
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun OverlayProfileButton(
    profile: Profile,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(32.dp)
            .clip(if (active) CutShape(6f) else RectangleShape)
            .background(if (active) profile.color else Panel)
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(profile.color)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.label,
                color = if (active) OnLime else TextInactive,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ToggleButton(
    label: String,
    on: Boolean,
    accent: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .background(Panel)
            .border(1.dp, if (on) accent else Hairline)
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (on) accent else TextSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(32.dp)
            .background(Panel)
            .border(1.dp, Hairline)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
