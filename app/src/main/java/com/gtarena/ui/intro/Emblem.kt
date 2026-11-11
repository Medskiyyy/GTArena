package com.gtarena.ui.intro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val EmblemSize: Dp = 22.dp

private val Blade = Path().apply {
    moveTo(0f, -3f)
    quadraticTo(0f, -30f, 36f, -34f)
    quadraticTo(16f, -20f, 14f, 4f)
    quadraticTo(7f, 10f, 0f, -3f)
    close()
}

fun DrawScope.turbineBlade(
    center: Offset,
    box: Float,
    index: Int,
    color: Color,
    alpha: Float = 1f,
    rotation: Float = 0f,
    spread: Float = 0f,
    grow: Float = 1f
) {
    val unit = box / 100f
    withTransform({
        translate(center.x, center.y)
        rotate(rotation + index * 120f, Offset.Zero)
        translate(spread * 0.6f, -spread * 0.8f)
        scale(unit * grow, unit * grow, Offset.Zero)
    }) {
        drawPath(Blade, color, alpha = alpha)
    }
}

@Composable
fun Emblem(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(EmblemSize)) {
        for (i in 0..2) turbineBlade(center, size.minDimension, i, color)
    }
}
