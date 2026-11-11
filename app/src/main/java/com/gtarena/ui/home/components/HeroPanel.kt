package com.gtarena.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.model.DeviceStats
import com.gtarena.model.HomeState
import com.gtarena.model.description
import com.gtarena.model.label
import com.gtarena.ui.intro.IntroTiming
import com.gtarena.ui.intro.LocalIntro
import com.gtarena.ui.intro.introEnter
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.Saira
import com.gtarena.ui.theme.SairaWide
import com.gtarena.ui.theme.SegBar
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.color
import com.gtarena.ui.theme.mechaPanel
import com.gtarena.ui.theme.textOnDark
import java.util.Locale

private const val LIMIT_C = 95f
private const val RED_ZONE_C = 85f
private val IdLocale: Locale = Locale.forLanguageTag("id-ID")
private val HeroShape = ChamferShape(18.dp)
private val CellShape = ChamferShape(10.dp)

@Composable
fun HeroPanel(state: HomeState, modifier: Modifier = Modifier) {
    val intro = LocalIntro.current
    val stats = state.stats
    val accent = state.profile.color
    val fraction by animateFloatAsState((stats.cpuTempC / LIMIT_C).coerceIn(0f, 1f), tween(500), label = "temp")
    val shownTemp by remember(stats.cpuTempC) {
        derivedStateOf { Math.round(stats.cpuTempC * intro.p(IntroTiming.ARC_START, IntroTiming.ARC_END)) }
    }
    val barFraction by remember {
        derivedStateOf { intro.p(IntroTiming.ARC_START, IntroTiming.ARC_END) }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .introEnter(IntroTiming.RING_START, IntroTiming.RING_END - IntroTiming.RING_START, dy = 12.dp)
            .mechaPanel(HeroShape, accent)
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Label("Mode aktif")
                Text(
                    text = state.profile.label.uppercase(),
                    color = state.profile.textOnDark,
                    fontFamily = SairaWide,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    lineHeight = 34.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
                Text(
                    text = state.profile.description,
                    color = TextSoft,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    minLines = 3,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .semantics { contentDescription = "Suhu CPU ${stats.cpuTempC} derajat" },
                horizontalAlignment = Alignment.End
            ) {
                Label("Suhu CPU")
                Text(
                    text = "$shownTemp°",
                    color = TextPrimary,
                    fontFamily = Saira,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 62.sp,
                    lineHeight = 64.sp,
                    maxLines = 1
                )
            }
        }
        SegBar(
            fraction = fraction * barFraction,
            color = accent,
            segments = 34,
            redFrom = RED_ZONE_C / LIMIT_C,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(14.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Label("0°", fontSize = 10.sp)
            Label("Batas ${LIMIT_C.toInt()}°", fontSize = 10.sp)
        }
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Hairline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(top = 10.dp)
        ) {
            Readout("Baterai", "${stats.batteryTempC}°", 0, Modifier.weight(1f))
            Divider()
            Readout("Daya", "${stats.batteryPct}%", 1, Modifier.weight(1f))
            Divider()
            Readout("Layar", "${stats.refreshHz} Hz", 2, Modifier.weight(1f))
            Divider()
            Readout("Cas", if (stats.charging) "Ngecas" else "Lepas", 3, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .padding(end = 10.dp)
            .width(1.dp)
            .fillMaxHeight()
            .background(Hairline)
    )
}

@Composable
private fun Readout(label: String, value: String, index: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.introEnter(
            IntroTiming.STAT_START + IntroTiming.STAT_STEP * index,
            IntroTiming.STAT_DUR,
            dy = 8.dp
        )
    ) {
        Label(label, fontSize = 10.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun LoadRow(stats: DeviceStats, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .introEnter(IntroTiming.TELE_VALUES_START, IntroTiming.TELE_VALUES_DUR, dy = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoadCell(
            label = "CPU besar",
            value = String.format(IdLocale, "%.2f", stats.bigCoreGhz),
            unit = "GHz",
            fraction = stats.bigCoreGhz / 3f,
            accent = accent,
            modifier = Modifier.weight(1f)
        )
        LoadCell(
            label = "GPU",
            value = "${stats.gpuLoadPct}",
            unit = "%",
            fraction = stats.gpuLoadPct / 100f,
            accent = accent,
            modifier = Modifier.weight(1f)
        )
        LoadCell(
            label = "RAM",
            value = String.format(IdLocale, "%.1f", stats.ramUsedGb),
            unit = "GB",
            fraction = stats.ramUsedGb / 7.4f,
            accent = accent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LoadCell(
    label: String,
    value: String,
    unit: String,
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "load")
    Column(
        modifier = modifier
            .mechaPanel(CellShape)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Label(label, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Label(unit, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        SegBar(
            fraction = shown,
            color = accent,
            segments = 12,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(6.dp)
        )
    }
}
