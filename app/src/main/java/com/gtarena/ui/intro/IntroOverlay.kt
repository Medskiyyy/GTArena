package com.gtarena.ui.intro

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
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
import com.gtarena.ui.theme.TextPrimary

private val EmblemColors = listOf(ProfileHemat, ProfileSeimbang, ProfileGaming)
private val TrailAlpha = floatArrayOf(0.32f, 0.2f, 0.1f)
private const val MAX_SCALE = 3.2f
private const val TRAIL_SPREAD = 0.12f

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        text = "GT ARENA",
        color = TextPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
        modifier = modifier
    )
}

private fun DrawScope.bar(x: Float, top: Float, barW: Float, h: Float, shift: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(x + shift, top)
        lineTo(x + barW + shift, top)
        lineTo(x + barW - shift, top + h)
        lineTo(x - shift, top + h)
        close()
    }
    drawPath(path, color, alpha = alpha)
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
                    translationX = lerp(startLeft(intro, size.width, screen.width.toFloat(), s0, 21.dp.toPx()), intro.logoPos.x, hp)
                    translationY = lerp((screen.height - size.height * s0) / 2f, intro.logoPos.y, hp)
                }
                .drawBehind {
                    if (screen.width == 0) return@drawBehind
                    val s0 = startScale(size.width, screen.width.toFloat(), 24.dp.toPx())
                    val g = lerp(s0, 1f, intro.p(IntroTiming.HANDOFF_START, IntroTiming.HANDOFF_END))
                    val barW = 5.dp.toPx()
                    val gap = 3.dp.toPx()
                    val h = 20.dp.toPx()
                    val shift = (h / 2f) * 0.3249197f
                    val top = (size.height - h) / 2f
                    val leftNat = startLeft(intro, size.width, screen.width.toFloat(), s0, 21.dp.toPx()) / s0
                    for (pass in 0..1) {
                        for (i in 0..2) {
                            val start = i * IntroTiming.BAR_STAGGER
                            val p = intro.p(start, start + IntroTiming.BAR_DUR, ExpoOut)
                            val x = i * (barW + gap)
                            val from = -leftNat - x - barW - shift - 4.dp.toPx()
                            val ox = from * (1f - p)
                            val rem = -ox
                            if (pass == 0) {
                                if (rem < 0.5f) continue
                                for (k in 3 downTo 1) {
                                    bar(x + ox - k * TRAIL_SPREAD * rem, top, barW, h, shift, EmblemColors[i], TrailAlpha[k - 1])
                                }
                            } else {
                                bar(x + ox, top, barW, h, shift, EmblemColors[i], 1f)
                            }
                        }
                    }
                    val wipe = intro.p(IntroTiming.WIPE_START, IntroTiming.WIPE_END)
                    val tail = intro.p(IntroTiming.WIPE_END, IntroTiming.LINE_RETRACT_END)
                    val x0 = 31.dp.toPx()
                    val span = size.width - x0
                    val a = x0 + span * tail
                    val b = x0 + span * wipe
                    if (b > a) {
                        drawRect(accent, Offset(a, size.height + 4.dp.toPx()), Size(b - a, 2.dp.toPx() / g))
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(Modifier.size(width = 21.dp, height = 20.dp))
            Wordmark(
                Modifier.drawWithContent {
                    val w = intro.p(IntroTiming.WIPE_START, IntroTiming.WIPE_END)
                    clipRect(right = size.width * w) { this@drawWithContent.drawContent() }
                }
            )
        }
    }
}

private fun startLeft(intro: IntroState, width: Float, screenWidth: Float, s0: Float, emblemWidth: Float): Float {
    val reveal = intro.p(IntroTiming.WIPE_START, IntroTiming.WIPE_END)
    return (screenWidth - width * s0) / 2f + (width - emblemWidth) * s0 / 2f * (1f - reveal)
}
