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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.ui.theme.GaugeRedZone
import com.gtarena.ui.theme.GaugeTick
import com.gtarena.ui.theme.GaugeTrack
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary

private const val LIMIT_C = 95f
private const val RED_ZONE_C = 85f

@Composable
fun TempGauge(tempC: Int, accent: Color, profileName: String, modifier: Modifier = Modifier) {
    val fraction = (tempC / LIMIT_C).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .size(204.dp)
            .semantics { contentDescription = "Suhu CPU $tempC derajat, zona merah mulai ${RED_ZONE_C.toInt()}" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(204.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerR = 92.dp.toPx()
            val tickR = 77.dp.toPx()
            val arcTopLeft = Offset(center.x - outerR, center.y - outerR)
            val arcSize = Size(outerR * 2, outerR * 2)
            val flatStroke = Stroke(width = 10.dp.toPx())
            drawArc(
                color = GaugeTrack,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = flatStroke
            )
            val redStart = 270f * RED_ZONE_C / LIMIT_C
            drawArc(
                color = GaugeRedZone,
                startAngle = 135f + redStart,
                sweepAngle = 270f - redStart,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = flatStroke
            )
            if (fraction > 0f) {
                drawArc(
                    color = accent,
                    startAngle = 135f,
                    sweepAngle = 270f * fraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            drawCircle(
                color = GaugeTick,
                radius = tickR,
                center = center,
                style = Stroke(
                    width = 7.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(1.6.dp.toPx(), 10.5.dp.toPx())
                    )
                )
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Suhu CPU", color = TextSecondary, fontSize = 13.sp)
            Text(
                text = "$tempC°",
                color = TextPrimary,
                fontSize = 64.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Text(
            text = profileName.uppercase(),
            color = accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}
