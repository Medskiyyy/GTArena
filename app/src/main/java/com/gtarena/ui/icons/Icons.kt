package com.gtarena.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
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

    val Focus: ImageVector by lazy {
        strokeIcon("Focus") {
            moveTo(4f, 9f); verticalLineTo(4f); horizontalLineTo(9f)
            moveTo(15f, 4f); horizontalLineTo(20f); verticalLineTo(9f)
            moveTo(20f, 15f); verticalLineTo(20f); horizontalLineTo(15f)
            moveTo(9f, 20f); horizontalLineTo(4f); verticalLineTo(15f)
            moveTo(10f, 12f); horizontalLineTo(14f)
            moveTo(12f, 10f); verticalLineTo(14f)
        }
    }

    val Bypass: ImageVector by lazy {
        strokeIcon("Bypass") {
            moveTo(13f, 3f); lineTo(6f, 13f); horizontalLineTo(12f)
            lineTo(11f, 21f); lineTo(18f, 11f); horizontalLineTo(12f)
            close()
        }
    }

    val Brightness: ImageVector by lazy {
        strokeIcon("Brightness") {
            moveTo(16f, 12f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 12f)
            arcTo(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 16f, 12f)
            close()
            moveTo(12f, 2f); verticalLineTo(5f)
            moveTo(12f, 19f); verticalLineTo(22f)
            moveTo(2f, 12f); horizontalLineTo(5f)
            moveTo(19f, 12f); horizontalLineTo(22f)
        }
    }

    val Chip: ImageVector by lazy {
        strokeIcon("Chip") {
            moveTo(6f, 6f); horizontalLineTo(18f); verticalLineTo(18f); horizontalLineTo(6f)
            close()
            moveTo(9f, 3f); verticalLineTo(6f)
            moveTo(15f, 3f); verticalLineTo(6f)
            moveTo(9f, 18f); verticalLineTo(21f)
            moveTo(15f, 18f); verticalLineTo(21f)
            moveTo(3f, 9f); horizontalLineTo(6f)
            moveTo(3f, 15f); horizontalLineTo(6f)
            moveTo(18f, 9f); horizontalLineTo(21f)
            moveTo(18f, 15f); horizontalLineTo(21f)
        }
    }

    private fun strokeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Butt,
                pathBuilder = block
            )
        }.build()
}
