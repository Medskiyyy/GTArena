package com.gtarena.ui.intro

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.gtarena.ui.theme.Ground
import com.gtarena.ui.theme.ProfileGaming
import com.gtarena.ui.theme.ProfileHemat
import com.gtarena.ui.theme.ProfileSeimbang
import com.gtarena.ui.theme.SairaWide
import com.gtarena.ui.theme.TextPrimary

private val EmblemColors = listOf(ProfileHemat, ProfileSeimbang, ProfileGaming)
private const val MAX_SCALE = 3.2f
private const val SPIN_TURNS = 420f

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        text = "GT ARENA",
        color = TextPrimary,
        fontFamily = SairaWide,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
        modifier = modifier
    )
}

private fun startScale(natW: Float, screenW: Float, marginPx: Float): Float =
    minOf(MAX_SCALE, (screenW - 2f * marginPx) / natW)

@Composable
fun IntroOverlay(intro: IntroState, accent: Color) {
    val visible by remember(intro) { derivedStateOf { intro.time.value < IntroTiming.HANDOFF_END } }
    if (!visible) return
    var screen by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { screen = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
            .drawBehind {
                drawRect(
                    Ground,
                    alpha = 1f - intro.p(IntroTiming.HANDOFF_START, IntroTiming.HANDOFF_END, LinearEasing)
                )
            }
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    if (screen.width == 0) {
                        alpha = 0f
                        return@graphicsLayer
                    }
                    transformOrigin = TransformOrigin(0f, 0f)
                    val hp = intro.p(IntroTiming.HANDOFF_START, IntroTiming.HANDOFF_END)
                    val s0 = startScale(size.width, screen.width.toFloat(), 24.dp.toPx())
                    val g = lerp(s0, 1f, hp)
                    scaleX = g
                    scaleY = g
                    translationX = lerp(startLeft(intro, size.width, screen.width.toFloat(), s0, EmblemSize.toPx()), intro.logoPos.x, hp)
                    translationY = lerp((screen.height - size.height * s0) / 2f, intro.logoPos.y, hp)
                }
                .drawBehind {
                    if (screen.width == 0) return@drawBehind
                    val box = EmblemSize.toPx()
                    val c = Offset(box / 2f, size.height / 2f)
                    val merge = intro.p(IntroTiming.MERGE_START, IntroTiming.MERGE_END)
                    val spin = intro.p(0f, IntroTiming.SPIN_END, ExpoOut)
                    val settle = intro.p(IntroTiming.HANDOFF_START, IntroTiming.HANDOFF_END)
                    val pulse = intro.p(IntroTiming.PULSE_START, IntroTiming.PULSE_END, ExpoOut)
                    if (pulse > 0f && pulse < 1f) {
                        drawCircle(
                            accent,
                            radius = box * lerp(0.45f, 1.7f, pulse),
                            center = c,
                            alpha = 0.7f * (1f - pulse),
                            style = Stroke(width = box * lerp(0.12f, 0.02f, pulse))
                        )
                    }
                    for (i in 0..2) {
                        val start = i * IntroTiming.BLADE_STAGGER
                        val arrive = intro.p(start, start + IntroTiming.BLADE_DUR, ExpoOut)
                        turbineBlade(
                            center = c,
                            box = box,
                            index = i,
                            color = lerp(EmblemColors[i], accent, merge),
                            alpha = intro.p(start, start + IntroTiming.BLADE_DUR * 0.4f, LinearEasing),
                            rotation = -SPIN_TURNS * (1f - spin) + 120f * settle,
                            spread = box * 1.6f * (1f - arrive),
                            grow = lerp(1.7f, 1f, arrive) * lerp(1.8f, 1f, intro.p(IntroTiming.WIPE_START, IntroTiming.WIPE_END))
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.size(EmblemSize))
            Wordmark(
                Modifier.drawWithContent {
                    val w = intro.p(IntroTiming.WIPE_START, IntroTiming.WIPE_END)
                    clipRect(right = size.width * w) {
                        translate(left = -12.dp.toPx() * (1f - w)) { this@drawWithContent.drawContent() }
                    }
                }
            )
        }
    }
}

private fun startLeft(intro: IntroState, width: Float, screenWidth: Float, s0: Float, emblemWidth: Float): Float {
    val reveal = intro.p(IntroTiming.WIPE_START, IntroTiming.WIPE_END)
    return (screenWidth - width * s0) / 2f + (width - emblemWidth) * s0 / 2f * (1f - reveal)
}
