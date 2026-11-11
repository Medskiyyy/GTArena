package com.gtarena.ui.overlay

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.data.FocusMode
import com.gtarena.data.GameTools
import com.gtarena.data.SettingsStore
import com.gtarena.data.TelemetryRepository
import com.gtarena.model.DeviceStats
import com.gtarena.model.Profile
import com.gtarena.model.label
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.theme.AppIcon
import com.gtarena.ui.theme.BevelBrush
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.PanelBrush
import com.gtarena.ui.theme.Saira
import com.gtarena.ui.theme.SairaCond
import com.gtarena.ui.theme.SegBar
import com.gtarena.ui.theme.SlantShape
import com.gtarena.ui.theme.SlantTab
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.color
import com.gtarena.ui.theme.mechaPanel
import com.gtarena.ui.theme.textOnDark
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val PANEL_WIDTH_DP = 324
internal const val PANEL_HEIGHT_DP = 300
internal const val HANDLE_WIDTH_DP = 14
internal const val HANDLE_HEIGHT_DP = 56

private val PanelShape = ChamferShape(16.dp)
private val TileShape = ChamferShape(9.dp)
private val BubbleShape = ChamferShape(6.dp)
private val TickShape = SlantShape(2.dp)
internal val AppShape = ChamferShape(7.dp)
private val RefreshSteps = listOf(60, 90, 120)
private val ThermalGlyphs = listOf("48°", "56°", "LEPAS")

internal class OverlayActions(
    val onProfile: (Profile) -> Unit,
    val onBypass: (Boolean) -> Unit,
    val onRefresh: (Int) -> Unit,
    val onThermal: (Int) -> Unit,
    val onBubble: (Boolean) -> Unit,
    val onCrosshair: (Boolean) -> Unit,
    val onScreenshot: () -> Unit,
    val onOpenApp: (String) -> Unit
)

@Composable
private fun AppStrip(apps: List<String>, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        apps.forEach { pkg ->
            AppIcon(
                pkg = pkg,
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppShape)
                    .clickable(role = Role.Button, onClickLabel = "Buka melayang") { onOpen(pkg) }
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(AppShape)
                .border(1.dp, BevelBrush, AppShape)
                .clickable(role = Role.Button, onClickLabel = "Tambah app melayang", onClick = onAdd),
            contentAlignment = Alignment.Center
        ) {
            Icon(GTIcons.Plus, contentDescription = "Tambah app melayang", tint = TextSoft, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun OverlayRoot(state: OverlayUiState, actions: OverlayActions) {
    val session = state.session ?: return
    val accent = session.profile.color
    if (!state.expanded) {
        OverlayHandle(accent = accent, onEnd = state.onEnd)
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<DeviceStats?>(null) }
    var brightness by remember { mutableStateOf<Float?>(null) }
    var adjustBrightness by remember { mutableStateOf(false) }
    val focusOn by FocusMode.enabled.collectAsState()
    val brightnessLocked by FocusMode.brightnessLocked.collectAsState()
    val recording by GameTools.recording.collectAsState()
    val store = remember { SettingsStore.get(context) }
    val apps by store.sidebarApps.collectAsState()
    var picking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FocusMode.refreshBrightnessLock()
        brightness = GameTools.brightness()
        GameTools.syncRecording(context)
    }
    LaunchedEffect(Unit) {
        TelemetryRepository(context).stats(1000L).collect { stats = it }
    }

    ProvideTextStyle(TextStyle(fontFamily = Saira, color = TextPrimary)) {
        Column(
            modifier = Modifier
                .width(PANEL_WIDTH_DP.dp)
                .mechaPanel(PanelShape, accent)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PanelHeader(label = session.label, start = state.sessionStart, accent = accent)
            if (picking) {
                AppPicker(selected = apps, accent = accent, onToggle = store::setSidebarApp, onDone = { picking = false })
                return@Column
            }
            AppStrip(apps = apps, onOpen = actions.onOpenApp, onAdd = { picking = true })
            HeroRow(fps = state.fps, stats = stats, accent = accent, fpsColor = session.profile.textOnDark)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Profile.values().forEach { p ->
                    SlantTab(
                        text = p.label,
                        active = p == session.profile,
                        color = p.color,
                        onClick = { actions.onProfile(p) },
                        modifier = Modifier.weight(1f),
                        height = 34.dp,
                        fontSize = 13.sp
                    )
                }
            }
            if (adjustBrightness) BrightnessRow(
                percent = brightness,
                accent = accent,
                onChange = { brightness = it },
                onCommit = { scope.launch { GameTools.setBrightness(it) } },
                onDone = { adjustBrightness = false }
            ) else Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Tile("Bubble FPS", state.bubble, accent, { actions.onBubble(!state.bubble) }, icon = GTIcons.Arena)
                Tile("Crosshair", state.crosshair, accent, { actions.onCrosshair(!state.crosshair) }, icon = GTIcons.Crosshair)
                Tile("Terang", false, accent, { adjustBrightness = true }, glyph = brightness?.let { "${it.toInt()}%" } ?: "--")
                Tile(
                    "Hz maks", false, accent,
                    {
                        val i = RefreshSteps.indexOf(session.refreshHz)
                        actions.onRefresh(RefreshSteps[(i + 1) % RefreshSteps.size])
                    },
                    glyph = "${session.refreshHz}"
                )
                Tile(
                    "Fokus", focusOn, accent,
                    { scope.launch { if (focusOn) FocusMode.disable() else FocusMode.enable() } },
                    icon = GTIcons.Focus
                )
                Tile("Bypass cas", session.bypass, accent, { actions.onBypass(!session.bypass) }, icon = GTIcons.Bypass)
                Tile(
                    "Batas panas", session.thermal > 0, accent,
                    { actions.onThermal((session.thermal + 1) % 3) },
                    glyph = ThermalGlyphs[session.thermal]
                )
                Tile(
                    "Kunci terang", brightnessLocked, accent,
                    { scope.launch { FocusMode.setBrightnessLocked(!brightnessLocked) } },
                    icon = GTIcons.Brightness
                )
                Tile("Screenshot", false, accent, actions.onScreenshot, icon = GTIcons.Camera)
                Tile(
                    if (recording) "Stop" else "Rekam", recording, accent,
                    {
                        scope.launch {
                            if (recording) GameTools.stopRecording(context) else GameTools.startRecording()
                        }
                    },
                    icon = GTIcons.Record
                )
                RamTile(accent)
            }
        }
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
                .height(44.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, accent.copy(alpha = 0.85f), accent.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
internal fun OverlayBubble(state: OverlayUiState) {
    val session = state.session ?: return
    val accent = session.profile.textOnDark
    Row(
        modifier = Modifier
            .clip(BubbleShape)
            .background(Color(0xD9060607))
            .border(1.dp, accent.copy(alpha = 0.55f), BubbleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = state.fps?.toString() ?: "--",
            color = accent,
            fontFamily = Saira,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            maxLines = 1
        )
        Label("FPS", fontSize = 9.sp, color = TextSoft)
    }
}

@Composable
internal fun OverlayCrosshair(state: OverlayUiState) {
    val accent = state.session?.profile?.color ?: return
    Canvas(modifier = Modifier.size(36.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val gap = 5.dp.toPx()
        val len = 9.dp.toPx()
        val arms = listOf(Offset(0f, -1f), Offset(0f, 1f), Offset(-1f, 0f), Offset(1f, 0f))
        for (pass in 0..1) {
            val color = if (pass == 0) Color.Black.copy(alpha = 0.7f) else accent
            val w = if (pass == 0) 4.dp.toPx() else 2.dp.toPx()
            arms.forEach { d ->
                drawLine(color, c + d * gap, c + d * (gap + len), strokeWidth = w)
            }
            drawCircle(color, radius = w * 0.6f, center = c)
        }
    }
}

@Composable
private fun PanelHeader(label: String, start: Long, accent: Color) {
    val elapsed by produceState(0L, start) {
        while (true) {
            value = (SystemClock.elapsedRealtime() - start) / 1000L
            delay(1000L)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .size(width = 7.dp, height = 12.dp)
                .clip(TickShape)
                .background(accent)
        )
        Text(
            text = label.uppercase(),
            color = TextPrimary,
            fontFamily = SairaCond,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Label("Sesi", fontSize = 10.sp)
        Text(
            text = if (elapsed >= 3600) {
                "%d:%02d:%02d".format(elapsed / 3600, elapsed / 60 % 60, elapsed % 60)
            } else {
                "%02d:%02d".format(elapsed / 60, elapsed % 60)
            },
            color = TextSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun HeroRow(fps: Int?, stats: DeviceStats?, accent: Color, fpsColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.width(96.dp)) {
            Text(
                text = fps?.toString() ?: "--",
                color = fpsColor,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                modifier = Modifier.height(46.dp)
            )
            Label("FPS · ${stats?.refreshHz ?: "--"} Hz", fontSize = 10.sp, modifier = Modifier.height(14.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            val s = stats
            MiniStat("CPU", s?.let { "${it.cpuTempC}°" }, (s?.cpuTempC ?: 0) / 95f, accent, redFrom = 85f / 95f)
            MiniStat("GPU", s?.let { "${it.gpuLoadPct}%" }, (s?.gpuLoadPct ?: 0) / 100f, accent)
            MiniStat("Bat", s?.let { "${it.batteryTempC}°" }, ((s?.batteryTempC ?: 20) - 20) / 30f, accent, redFrom = 0.8f)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String?, fraction: Float, accent: Color, redFrom: Float = 2f) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Label(label, fontSize = 10.sp, modifier = Modifier.width(28.dp))
        SegBar(
            fraction = fraction,
            color = accent,
            segments = 14,
            redFrom = redFrom,
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
        )
        Text(
            text = value ?: "--",
            color = TextPrimary,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun BrightnessRow(
    percent: Float?,
    accent: Color,
    onChange: (Float) -> Unit,
    onCommit: (Float) -> Unit,
    onDone: () -> Unit
) {
    val change by rememberUpdatedState(onChange)
    val commit by rememberUpdatedState(onCommit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = GTIcons.Brightness,
            contentDescription = "Kecerahan",
            tint = TextSoft,
            modifier = Modifier.size(16.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(26.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var v = (down.position.x / size.width).coerceIn(0.01f, 1f) * 100f
                        change(v)
                        drag(down.id) {
                            v = (it.position.x / size.width).coerceIn(0.01f, 1f) * 100f
                            change(v)
                            it.consume()
                        }
                        commit(v)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SegBar(
                fraction = (percent ?: 0f) / 100f,
                color = accent,
                segments = 30,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )
        }
        Text(
            text = percent?.let { "${it.toInt()}%" } ?: "--",
            color = TextSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.width(34.dp)
        )
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 40.dp)
                .clip(TileShape)
                .background(accent)
                .clickable(role = Role.Button, onClick = onDone),
            contentAlignment = Alignment.Center
        ) {
            Label("OK", color = OnLime, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RamTile(accent: Color) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var freedMb by remember { mutableStateOf<Int?>(null) }
    val fill = remember { Animatable(0f) }
    val freed = freedMb
    Tile(
        label = when {
            busy -> "Bersihin"
            freed == null -> "Bersihin RAM"
            freed > 0 -> "MB lega"
            else -> "Udah lega"
        },
        on = busy || freed != null,
        accent = accent,
        onClick = {
            if (!busy && freedMb == null) scope.launch {
                busy = true
                val sweep = launch {
                    while (true) {
                        fill.snapTo(0f)
                        fill.animateTo(1f, tween(1400, easing = LinearEasing))
                    }
                }
                val started = SystemClock.elapsedRealtime()
                val result = FocusMode.cleanRam()
                delay((1200L - (SystemClock.elapsedRealtime() - started)).coerceAtLeast(0L))
                sweep.cancelAndJoin()
                fill.snapTo(1f)
                busy = false
                freedMb = result
                launch { fill.animateTo(0f, tween(5000, easing = LinearEasing)) }
                delay(2600L)
                fill.snapTo(0f)
                freedMb = null
            }
        },
        icon = if (freed == null) GTIcons.Chip else null,
        glyph = freed?.let { if (it > 0) "+$it" else "OK" },
        fill = { fill.value }
    )
}

@Composable
private fun Tile(
    label: String,
    on: Boolean,
    accent: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    glyph: String? = null,
    fill: (() -> Float)? = null
) {
    val tint = if (on) accent else TextSoft
    Column(
        modifier = Modifier
            .width(86.dp)
            .height(54.dp)
            .clip(TileShape)
            .background(PanelBrush)
            .drawBehind {
                if (on) {
                    drawRect(Brush.verticalGradient(listOf(accent.copy(alpha = 0.05f), accent.copy(alpha = 0.30f))))
                    val bar = 2.dp.toPx()
                    drawRect(accent, topLeft = Offset(0f, size.height - bar), size = Size(size.width, bar))
                }
                val f = fill?.invoke() ?: 0f
                if (f > 0f) drawRect(accent.copy(alpha = 0.35f), size = Size(size.width * f, size.height))
            }
            .then(
                if (on) Modifier.border(1.dp, accent.copy(alpha = 0.7f), TileShape)
                else Modifier.border(1.dp, BevelBrush, TileShape)
            )
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        } else {
            Text(
                text = glyph.orEmpty(),
                color = TextPrimary,
                fontSize = 17.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        Label(text = label, color = tint, fontSize = 10.sp)
    }
}