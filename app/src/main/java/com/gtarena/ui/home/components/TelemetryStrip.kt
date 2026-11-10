package com.gtarena.ui.home.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.ui.intro.IntroTiming
import com.gtarena.ui.intro.introDraw
import com.gtarena.ui.intro.introEnter
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary

class TelemetryItem(val label: String, val value: String, val unit: String? = null)

@Composable
fun TelemetryStrip(items: List<TelemetryItem>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .introDraw(IntroTiming.TELE_LINE_START, IntroTiming.TELE_LINE_DUR)
                .background(Hairline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .introEnter(IntroTiming.TELE_VALUES_START, IntroTiming.TELE_VALUES_DUR)
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(Modifier.width(1.dp).fillMaxHeight().background(Hairline))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (index > 0) 14.dp else 0.dp, top = 10.dp, bottom = 10.dp)
                ) {
                    Text(text = item.label, color = TextSecondary, fontSize = 13.sp, maxLines = 1)
                    Text(
                        text = buildAnnotatedString {
                            append(item.value)
                            if (item.unit != null) {
                                append(" ")
                                withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Normal)) {
                                    append(item.unit)
                                }
                            }
                        },
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .introDraw(IntroTiming.TELE_LINE_START + IntroTiming.TELE_LINE_STEP, IntroTiming.TELE_LINE_DUR)
                .background(Hairline)
        )
    }
}
