package com.gtarena.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gtarena.R

@OptIn(ExperimentalTextApi::class)
private fun saira(width: Float) = FontFamily(
    listOf(400, 600, 700, 800).map { weight ->
        Font(
            R.font.saira,
            FontWeight(weight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(weight),
                FontVariation.width(width)
            )
        )
    }
)

val Saira = saira(100f)
val SairaCond = saira(78f)
val SairaWide = saira(120f)

val GTTypography = Typography(
    bodyLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    titleLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = Saira, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    labelMedium = TextStyle(fontFamily = Saira, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
)
