package com.gtarena.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object GTIcons {
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Butt
            ) {
                moveTo(4f, 7f); horizontalLineTo(14f)
                moveTo(18f, 7f); horizontalLineTo(20f)
                moveTo(4f, 17f); horizontalLineTo(6f)
                moveTo(10f, 17f); horizontalLineTo(20f)
                moveTo(18f, 7f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 14f, 7f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 18f, 7f)
                close()
                moveTo(10f, 17f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 17f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = true, isPositiveArc = true, 10f, 17f)
                close()
            }
        }.build()
    }

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(7f, 4f)
                lineTo(20f, 12f)
                lineTo(7f, 20f)
                close()
            }
        }.build()
    }
}
