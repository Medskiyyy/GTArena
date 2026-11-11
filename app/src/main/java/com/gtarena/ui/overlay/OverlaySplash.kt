package com.gtarena.ui.overlay

import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.gtarena.model.Profile
import com.gtarena.model.label
import com.gtarena.ui.intro.ExpoOut
import com.gtarena.ui.intro.Wordmark
import com.gtarena.ui.intro.turbineBlade
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.ProfileGaming
import com.gtarena.ui.theme.ProfileHemat
import com.gtarena.ui.theme.ProfileSeimbang
import com.gtarena.ui.theme.color
import com.gtarena.ui.theme.textOnDark

private const val TOTAL_MS = 2000f
private const val FADE_OUT_MS = 1650f
private val BladeColors = listOf(ProfileHemat, ProfileSeimbang, ProfileGaming)

private fun span(ms: Float, start: Float, end: Float): Float = ((ms - start) / (end - start)).coerceIn(0f, 1f)

@Composable
internal fun OverlaySplash(profile: Profile, onDone: () -> Unit) {
    var ms by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (ms < TOTAL_MS) ms = withFrameNanos { (it - start) / 1_000_000f }
        onDone()
    }
    val accent = profile.color
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = span(ms, 0f, 160f) * (1f - span(ms, FADE_OUT_MS, TOTAL_MS)) }
            .drawBehind { drawRect(Ground, alpha = 0.9f) },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Canvas(modifier = Modifier.size(92.dp)) {
                val box = size.minDimension
                val spin = ExpoOut.transform(span(ms, 0f, 900f))
                val merge = span(ms, 650f, 900f)
                val pulse = ExpoOut.transform(span(ms, 760f, 1300f))
                if (pulse > 0f && pulse < 1f) {
                    drawCircle(
                        accent,
                        radius = box * lerp(0.4f, 0.95f, pulse),
                        alpha = 0.7f * (1f - pulse),
                        style = Stroke(width = box * lerp(0.08f, 0.01f, pulse))
                    )
                }
                for (i in 0..2) {
                    val arrive = ExpoOut.transform(span(ms, i * 110f, i * 110f + 620f))
                    turbineBlade(
                        center = center,
                        box = box,
                        index = i,
                        color = lerp(BladeColors[i], accent, merge),
                        alpha = LinearEasing.transform(span(ms, i * 110f, i * 110f + 240f)),
                        rotation = -360f * (1f - spin),
                        spread = box * 0.9f * (1f - arrive),
                        grow = lerp(1.5f, 1f, arrive)
                    )
                }
            }
            Column(
                modifier = Modifier.graphicsLayer {
                    val p = ExpoOut.transform(span(ms, 720f, 1150f))
                    alpha = p
                    translationY = (1f - p) * 10.dp.toPx()
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Wordmark()
                Label(text = "Profil ${profile.label}", color = profile.textOnDark, fontSize = 12.sp)
            }
        }
    }
}
