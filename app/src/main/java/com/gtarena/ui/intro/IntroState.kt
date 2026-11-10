package com.gtarena.ui.intro

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.withContext
import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object IntroTiming {
    const val TOTAL = 3000f
    const val BAR_STAGGER = 112f
    const val BAR_DUR = 500f
    const val WIPE_START = 720f
    const val WIPE_END = 1360f
    const val LINE_RETRACT_END = 2050f
    const val HANDOFF_START = 1810f
    const val HANDOFF_END = 2290f
    const val RING_START = 1890f
    const val RING_END = 2370f
    const val ARC_START = 1970f
    const val ARC_END = 2930f
    const val STAT_START = 1970f
    const val STAT_STEP = 96f
    const val STAT_DUR = 560f
    const val PROFILE_START = 2050f
    const val PROFILE_STEP = 80f
    const val PROFILE_DUR = 560f
    const val PROFILE_BAR_DELAY = 240f
    const val PROFILE_BAR_DUR = 480f
    const val DESC_START = 2290f
    const val DESC_DUR = 480f
    const val TELE_LINE_START = 2210f
    const val TELE_LINE_STEP = 80f
    const val TELE_LINE_DUR = 480f
    const val TELE_VALUES_START = 2530f
    const val TELE_VALUES_DUR = 400f
    const val GAME_HEADER_START = 2210f
    const val GAME_ROW_START = 2290f
    const val GAME_STEP = 80f
    const val GAME_DUR = 530f
    const val NAV_START = 2450f
}

object IntroGate {
    fun animationsOn(): Boolean = ValueAnimator.areAnimatorsEnabled()
}

private object RealSpeed : MotionDurationScale {
    override val scaleFactor: Float = 1f
}

val ExpoOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Stable
class IntroState(val enabled: Boolean) {
    val time = Animatable(if (enabled) 0f else IntroTiming.TOTAL)
    var logoPos by mutableStateOf(Offset.Zero)

    val logoShown: Boolean get() = time.value >= IntroTiming.HANDOFF_END

    fun p(start: Float, end: Float, easing: Easing = FastOutSlowInEasing): Float =
        easing.transform(((time.value - start) / (end - start)).coerceIn(0f, 1f))

    suspend fun play() {
        if (!enabled) return
        repeat(3) { withFrameNanos { } }
        withContext(RealSpeed) {
            time.animateTo(IntroTiming.TOTAL, tween(IntroTiming.TOTAL.toInt(), easing = LinearEasing))
        }
    }

    suspend fun skip() {
        time.snapTo(IntroTiming.TOTAL)
    }
}

val LocalIntro = staticCompositionLocalOf { IntroState(false) }

@Composable
fun Modifier.introEnter(start: Float, dur: Float, dx: Dp = 0.dp, dy: Dp = 0.dp): Modifier {
    val intro = LocalIntro.current
    return graphicsLayer {
        val p = intro.p(start, start + dur)
        alpha = p
        translationX = (1f - p) * dx.toPx()
        translationY = (1f - p) * dy.toPx()
    }
}

@Composable
fun Modifier.introDraw(start: Float, dur: Float): Modifier {
    val intro = LocalIntro.current
    return graphicsLayer {
        transformOrigin = TransformOrigin(0f, 0.5f)
        scaleX = intro.p(start, start + dur)
    }
}

@Composable
fun Modifier.introFromBelow(start: Float, end: Float): Modifier {
    val intro = LocalIntro.current
    return graphicsLayer {
        val p = intro.p(start, end)
        alpha = p
        translationY = (1f - p) * size.height
    }
}
