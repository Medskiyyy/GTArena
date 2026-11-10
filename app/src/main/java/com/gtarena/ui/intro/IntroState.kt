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
    const val TOTAL = 1600f
    const val BAR_STAGGER = 70f
    const val BAR_DUR = 310f
    const val WIPE_START = 450f
    const val WIPE_END = 850f
    const val LINE_RETRACT_END = 1000f
    const val HANDOFF_START = 850f
    const val HANDOFF_END = 1150f
    const val RING_START = 900f
    const val RING_END = 1200f
    const val ARC_START = 950f
    const val ARC_END = 1550f
    const val STAT_START = 950f
    const val STAT_STEP = 60f
    const val STAT_DUR = 350f
    const val PROFILE_START = 1000f
    const val PROFILE_STEP = 50f
    const val PROFILE_DUR = 350f
    const val PROFILE_BAR_DELAY = 150f
    const val PROFILE_BAR_DUR = 300f
    const val DESC_START = 1150f
    const val DESC_DUR = 300f
    const val TELE_LINE_START = 1100f
    const val TELE_LINE_STEP = 50f
    const val TELE_LINE_DUR = 300f
    const val TELE_VALUES_START = 1300f
    const val TELE_VALUES_DUR = 250f
    const val GAME_HEADER_START = 1100f
    const val GAME_ROW_START = 1150f
    const val GAME_STEP = 50f
    const val GAME_DUR = 330f
    const val NAV_START = 1250f
}

object IntroGate {
    var played = false

    fun claim(onArena: Boolean): Boolean {
        val go = !played && onArena && ValueAnimator.areAnimatorsEnabled()
        played = true
        return go
    }
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
