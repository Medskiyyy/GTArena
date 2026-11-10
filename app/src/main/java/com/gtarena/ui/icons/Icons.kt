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

    val Arena: ImageVector by lazy {
        ImageVector.Builder(
            name = "Arena",
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
                moveTo(4f, 14f)
                arcTo(8f, 8f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 14f)
                moveTo(12f, 14f); lineTo(16f, 9f)
            }
        }.build()
    }

    val Games: ImageVector by lazy {
        ImageVector.Builder(
            name = "Games",
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
                moveTo(6f, 7f); horizontalLineTo(18f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 10f)
                verticalLineTo(15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 18f)
                horizontalLineTo(6f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 15f)
                verticalLineTo(10f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6f, 7f)
                close()
                moveTo(8f, 11f); verticalLineTo(14f)
                moveTo(6.5f, 12.5f); horizontalLineTo(9.5f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(16f, 12.5f); horizontalLineTo(16.01f)
            }
        }.build()
    }

    val Overlay: ImageVector by lazy {
        ImageVector.Builder(
            name = "Overlay",
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
                moveTo(12f, 4f); lineTo(21f, 9f); lineTo(12f, 14f); lineTo(3f, 9f)
                close()
                moveTo(3f, 14f); lineTo(12f, 19f); lineTo(21f, 14f)
            }
        }.build()
    }
}
