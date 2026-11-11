package com.gtarena.ui.overlay

import android.os.SystemClock
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.data.FocusMode
import com.gtarena.data.GameTools
import com.gtarena.data.TelemetryRepository
import com.gtarena.model.DeviceStats
import com.gtarena.model.Profile
import com.gtarena.model.label
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.theme.BevelBrush
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.Label
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
import kotlin.math.max
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val PANEL_WIDTH_DP = 324
internal const val PANEL_HEIGHT_DP = 310
internal const val HANDLE_WIDTH_DP = 14
internal const val HANDLE_HEIGHT_DP = 56

private val PanelShape = ChamferShape(16.dp)
private val TileShape = ChamferShape(9.dp)
private val BubbleShape = ChamferShape(6.dp)
private val TickShape = SlantShape(2.dp)
private val RefreshSteps = listOf(60, 90, 120)

internal class OverlayActions(
    val onProfile: (Profile) -> Unit,
    val onBypass: (Boolean) -> Unit,
    val onRefresh: (Int) -> Unit,
    val onBubble: (Boolean) -> Unit,
    val onCrosshair: (Boolean) -> Unit,
    val onScreenshot: () -> Unit
)

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
    val focusOn by FocusMode.enabled.collectAsState()
    val brightnessLocked by FocusMode.brightnessLocked.collectAsState()
    val recording by GameTools.recording.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PanelHeader(label = session.label, start = state.sessionStart, accent = accent)
            FpsRow(fps = state.fps, hz = stats?.refreshHz, samples = state.samples, accent = session.profile.textOnDark)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val s = stats
                StatCell("CPU", s?.let { "${it.cpuTempC}°" }, (s?.cpuTempC ?: 0) / 95f, accent, Modifier.weight(1f), redFrom = 85f / 95f)
                StatCell("GPU", s?.let { "${it.gpuLoadPct}%" }, (s?.gpuLoadPct ?: 0) / 100f, accent, Modifier.weight(1f))
                StatCell("Baterai", s?.let { "${it.batteryTempC}°" }, ((s?.batteryTempC ?: 20) - 20) / 30f, accent, Modifier.weight(1f), redFrom = 0.8f)
            }
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
                        height = 36.dp,
                        fontSize = 13.sp
                    )
                }
            }
            BrightnessRow(
                percent = brightness,
                accent = accent,
                onChange = { brightness = it },
                onCommit = { scope.launch { GameTools.setBrightness(it) } }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Tile("Bubble FPS", state.bubble, accent, { actions.onBubble(!state.bubble) }, icon = GTIcons.Arena)
                Tile("Crosshair", state.crosshair, accent, { actions.onCrosshair(!state.crosshair) }, icon = GTIcons.Crosshair)
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
                Tile("Bersihin RAM", false, accent, { scope.launch { FocusMode.cleanRam() } }, icon = GTIcons.Chip)
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
private fun FpsRow(fps: Int?, hz: Int?, samples: List<Int>, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = fps?.toString() ?: "--",
            color = accent,
            fontSize = 44.sp,
            lineHeight = 54.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Column(modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)) {
            Label("FPS", color = TextPrimary, fontSize = 12.sp)
            Label("Layar ${hz ?: "--"} Hz", fontSize = 10.sp)
        }
        Sparkline(
            samples = samples,
            color = accent,
            modifier = Modifier
                .padding(start = 14.dp, bottom = 10.dp)
                .weight(1f)
                .height(38.dp)
        )
    }
}

@Composable
private fun Sparkline(samples: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val base = size.height
        drawLine(Color(0x33FFFFFF), Offset(0f, base), Offset(size.width, base), strokeWidth = 1.dp.toPx())
        if (samples.size < 2) return@Canvas
        val ceiling = max(60, samples.max()).toFloat() * 1.1f
        val step = size.width / 39f
        val line = Path()
        val first = (40 - samples.size) * step
        samples.forEachIndexed { i, v ->
            val x = first + i * step
            val y = base * (1f - v.coerceAtLeast(0) / ceiling)
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, base)
            lineTo(first, base)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.38f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Miter))
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String?,
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    redFrom: Float = 2f
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Label(label, fontSize = 10.sp, modifier = Modifier.padding(bottom = 2.dp))
            Text(
                text = value ?: "--",
                color = TextPrimary,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
        SegBar(
            fraction = fraction,
            color = accent,
            segments = 12,
            redFrom = redFrom,
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
        )
    }
}

@Composable
private fun BrightnessRow(percent: Float?, accent: Color, onChange: (Float) -> Unit, onCommit: (Float) -> Unit) {
    val change by rememberUpdatedState(onChange)
    val commit by rememberUpdatedState(onCommit)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
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
    }
}

@Composable
private fun Tile(
    label: String,
    on: Boolean,
    accent: Color,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    glyph: String? = null
) {
    val tint = if (on) accent else TextSoft
    Column(
        modifier = Modifier
            .width(86.dp)
            .height(60.dp)
            .clip(TileShape)
            .background(PanelBrush)
            .drawBehind {
                if (on) {
                    drawRect(Brush.verticalGradient(listOf(accent.copy(alpha = 0.05f), accent.copy(alpha = 0.30f))))
                    val bar = 2.dp.toPx()
                    drawRect(accent, topLeft = Offset(0f, size.height - bar), size = Size(size.width, bar))
                }
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
