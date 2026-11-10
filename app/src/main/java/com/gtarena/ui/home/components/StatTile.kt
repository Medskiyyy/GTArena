package com.gtarena.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.Panel
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(Panel, shape)
            .border(1.dp, Hairline, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp, maxLines = 1)
        Text(
            text = buildAnnotatedString {
                append(value)
                if (unit != null) {
                    append(" ")
                    withStyle(SpanStyle(color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Normal)) {
                        append(unit)
                    }
                }
            },
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
