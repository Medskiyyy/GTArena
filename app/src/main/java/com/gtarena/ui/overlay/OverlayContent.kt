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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.min
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

@Composable
internal fun OverlayRoot(
    state: OverlayUiState,
    onExpandedChange: (Boolean) -> Unit,
    onProfileChange: (Profile) -> Unit
) {
    val session = state.session ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<DeviceStats?>(null) }
    var fps by remember { mutableStateOf<Int?>(null) }
    val samples = remember { mutableStateListOf<Int>() }
    val focusOn by FocusMode.enabled.collectAsState()

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

    val accent = session.profile.color
    ProvideTextStyle(TextStyle(fontFamily = Saira, color = TextPrimary)) {
        if (state.expanded) {
            val width = min(300, state.screenWidthDp - 20).coerceAtLeast(200)
            val maxHeight = (state.screenHeightDp - 20).coerceAtLeast(160)
            OverlayPanel(
                width = width,
                maxHeight = maxHeight,
                accent = accent,
                profile = session.profile,
                fps = fps,
                samples = samples,
                stats = stats,
                focusOn = focusOn,
                onCollapse = { onExpandedChange(false) },
                onProfile = onProfileChange,
                onFocus = { scope.launch { if (focusOn) FocusMode.disable() else FocusMode.enable() } },
                onCleanRam = { scope.launch { FocusMode.cleanRam() } }
            )
        } else {
            OverlayPill(fps = fps, cpuTemp = stats?.cpuTempC, batteryPct = stats?.batteryPct, accent = accent)
        }
    }
}

@Composable
internal fun OverlayPill(fps: Int?, cpuTemp: Int?, batteryPct: Int?, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Ground)
            .border(1.dp, Hairline, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "${fps?.toString() ?: "--"} FPS",
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "${cpuTemp?.toString() ?: "--"}°",
            color = TextSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "${batteryPct?.toString() ?: "--"}%",
            color = TextSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun OverlayPanel(
    width: Int,
    maxHeight: Int,
    accent: Color,
    profile: Profile,
    fps: Int?,
    samples: List<Int>,
    stats: DeviceStats?,
    focusOn: Boolean,
    onCollapse: () -> Unit,
    onProfile: (Profile) -> Unit,
    onFocus: () -> Unit,
    onCleanRam: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .width(width.dp)
            .heightIn(max = maxHeight.dp)
            .clip(shape)
            .background(Ground)
            .border(1.dp, Hairline, shape)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "GT ARENA",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Box(
                modifier = Modifier
                    .requiredSize(44.dp)
                    .clickable(role = Role.Button, onClick = onCollapse),
                contentAlignment = Alignment.CenterEnd
            ) {
                Canvas(modifier = Modifier.requiredSize(20.dp)) {
                    val s = size.width / 24f
                    val p = Path().apply {
                        moveTo(15f * s, 6f * s)
                        lineTo(9f * s, 12f * s)
                        lineTo(15f * s, 18f * s)
                    }
                    drawPath(
                        p,
                        TextSoft,
                        style = Stroke(width = 2f * s, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Panel)
                .border(1.dp, Hairline, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = fps?.toString() ?: "--",
                    color = accent,
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "FPS, layar ${stats?.refreshHz ?: "--"} Hz",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            Sparkline(
                samples = samples,
                color = accent,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
                    .height(48.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatTile("CPU", stats?.let { "${it.cpuTempC}°" } ?: "--", Modifier.weight(1f))
            StatTile("GPU", stats?.let { "${it.gpuLoadPct}%" } ?: "--", Modifier.weight(1f))
            StatTile("Baterai", stats?.let { "${it.batteryTempC}°" } ?: "--", Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Profile.values().forEach { p ->
                OverlayProfileButton(p, p == profile, { onProfile(p) }, Modifier.weight(1f))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleButton("Mode fokus", focusOn, accent, onFocus, Modifier.weight(1f))
            ActionButton("Bersihin RAM", onCleanRam, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Sparkline(samples: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val top = 1.dp.toPx()
        val h = size.height - 2 * top
        drawLine(
            Hairline,
            Offset(0f, top),
            Offset(size.width, top),
            strokeWidth = 1.dp.toPx()
        )
        if (samples.size < 2) return@Canvas
        val ceiling = max(60, samples.max()).toFloat()
        val step = size.width / 29f
        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = i * step
            val y = top + h * (1f - v.coerceAtLeast(0) / ceiling)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Panel)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp, maxLines = 1)
        Text(text = value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
            .height(44.dp)
            .clip(if (active) CutShape(8f) else RectangleShape)
            .background(if (active) profile.color else Panel)
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
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
                fontSize = 14.sp,
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
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(Panel)
            .border(1.dp, if (on) accent else Hairline, shape)
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (on) accent else TextSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(Panel)
            .border(1.dp, Hairline, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
