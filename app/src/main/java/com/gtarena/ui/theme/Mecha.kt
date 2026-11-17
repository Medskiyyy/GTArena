package com.gtarena.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val PanelBrush = Brush.verticalGradient(listOf(PanelHi, PanelLo))
val BevelBrush = Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x0DFFFFFF)))

class ChamferShape(
    private val cut: Dp,
    private val topStart: Boolean = true,
    private val topEnd: Boolean = false,
    private val bottomEnd: Boolean = true,
    private val bottomStart: Boolean = false
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { cut.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        val w = size.width
        val h = size.height
        return Outline.Generic(
            Path().apply {
                moveTo(if (topStart) c else 0f, 0f)
                lineTo(if (topEnd) w - c else w, 0f)
                if (topEnd) lineTo(w, c)
                lineTo(w, if (bottomEnd) h - c else h)
                if (bottomEnd) lineTo(w - c, h)
                lineTo(if (bottomStart) c else 0f, h)
                if (bottomStart) lineTo(0f, h - c)
                lineTo(0f, if (topStart) c else 0f)
                close()
            }
        )
    }
}

class SlantShape(private val skew: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val s = with(density) { skew.toPx() }.coerceAtMost(size.width / 2f)
        return Outline.Generic(
            Path().apply {
                moveTo(s, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width - s, size.height)
                lineTo(0f, size.height)
                close()
            }
        )
    }
}

fun Modifier.mechaPanel(shape: Shape, accent: Color? = null): Modifier =
    clip(shape)
        .background(PanelBrush)
        .then(
            if (accent == null) Modifier
            else Modifier.drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.8f
                    )
                )
            }
        )
        .border(1.dp, BevelBrush, shape)

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    fontSize: TextUnit = 11.sp
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = SairaCond,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        letterSpacing = 1.4.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = TextSecondary,
    fontSize: TextUnit = 17.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(width = 10.dp, height = 14.dp)
                .clip(SlantShape(4.dp))
                .background(accent)
        )
        Text(
            text = text.uppercase(),
            color = TextPrimary,
            fontFamily = SairaWide,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            letterSpacing = 2.sp,
            maxLines = 1
        )
    }
}

@Composable
fun SegBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    segments: Int = 24,
    redFrom: Float = 2f
) {
    Canvas(modifier = modifier) {
        val gap = 2.dp.toPx()
        val skew = size.height * 0.4f
        val w = (size.width - skew - gap * (segments - 1)) / segments
        val lit = (fraction.coerceIn(0f, 1f) * segments).roundToInt()
        val path = Path()
        for (i in 0 until segments) {
            val x = i * (w + gap)
            val red = (i + 0.5f) / segments >= redFrom
            val c = when {
                i < lit && red -> ProfileGaming
                i < lit -> color
                red -> GaugeRedZone
                else -> GaugeTrack
            }
            path.reset()
            path.moveTo(x + skew, 0f)
            path.lineTo(x + skew + w, 0f)
            path.lineTo(x + w, size.height)
            path.lineTo(x, size.height)
            path.close()
            drawPath(path, c)
        }
    }
}

@Composable
fun SlantTab(
    text: String,
    active: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
    fontSize: TextUnit = 15.sp
) {
    val shape = remember { SlantShape(10.dp) }
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(
                if (active) Brush.verticalGradient(listOf(color, lerp(color, Color.Black, 0.25f)))
                else PanelBrush
            )
            .then(if (active) Modifier else Modifier.border(1.dp, BevelBrush, shape))
            .drawBehind {
                if (!active) {
                    val bar = 3.dp.toPx()
                    drawRect(color, topLeft = Offset(0f, size.height - bar), size = Size(size.width, bar))
                }
            }
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (active) OnLime else TextInactive,
            fontFamily = SairaCond,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
    }
}

class IconData(val bitmap: ImageBitmap, val tint: Color)

private val iconCache = ConcurrentHashMap<String, IconData>()
private val NeutralTint = Color(0xFF3A3D44)

private fun vividTint(bitmap: Bitmap): Color {
    val small = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
    val hsv = FloatArray(3)
    var r = 0f
    var g = 0f
    var b = 0f
    var total = 0f
    for (y in 0 until 16) {
        for (x in 0 until 16) {
            val p = small.getPixel(x, y)
            if (android.graphics.Color.alpha(p) < 128) continue
            android.graphics.Color.colorToHSV(p, hsv)
            val w = hsv[1] * hsv[1] * hsv[2]
            r += android.graphics.Color.red(p) * w
            g += android.graphics.Color.green(p) * w
            b += android.graphics.Color.blue(p) * w
            total += w
        }
    }
    if (total < 4f) return NeutralTint
    return Color(r / total / 255f, g / total / 255f, b / total / 255f)
}

private fun fullBleed(drawable: Drawable, size: Int): Bitmap {
    if (drawable !is AdaptiveIconDrawable) return drawable.toBitmap(size, size)
    val bitmap = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bitmap)
    val full = (size * 1.5f).toInt()
    val off = (size - full) / 2
    listOfNotNull(drawable.background, drawable.foreground).forEach {
        it.setBounds(off, off, off + full, off + full)
        it.draw(canvas)
    }
    return bitmap
}

@Composable
fun rememberAppIcon(pkg: String): IconData? {
    val context = LocalContext.current
    val icon by produceState(iconCache[pkg], pkg) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val bitmap = fullBleed(context.packageManager.getApplicationIcon(pkg), 192)
                    IconData(bitmap.asImageBitmap(), vividTint(bitmap))
                }.getOrNull()
            }?.also { iconCache[pkg] = it }
        }
    }
    return icon
}

@Composable
fun AppIcon(pkg: String, modifier: Modifier = Modifier) {
    val bitmap = rememberAppIcon(pkg)?.bitmap
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(GaugeTrack))
    }
}
