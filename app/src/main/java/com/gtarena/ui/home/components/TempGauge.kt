package com.gtarena.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.Lime
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary

private const val LIMIT_C = 95f

@Composable
fun TempGauge(tempC: Int, modifier: Modifier = Modifier) {
    val fraction = (tempC / LIMIT_C).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .size(196.dp)
            .semantics { contentDescription = "Suhu CPU $tempC derajat dari batas 95" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(196.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = 86.dp.toPx()
            val innerR = 70.dp.toPx()
            val outerStroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val arcTopLeft = Offset(center.x - outerR, center.y - outerR)
            val arcSize = Size(outerR * 2, outerR * 2)
            drawArc(
                color = GaugeTrack,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = outerStroke
            )
            if (fraction > 0f) {
                drawArc(
                    color = Lime,
                    startAngle = 135f,
                    sweepAngle = 270f * fraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = outerStroke
                )
            }
            drawCircle(
                color = Hairline,
                radius = innerR,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(2.dp.toPx(), 9.dp.toPx())
                    )
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$tempC°",
                color = TextPrimary,
                fontSize = 60.sp,
                lineHeight = 60.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Suhu CPU",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
