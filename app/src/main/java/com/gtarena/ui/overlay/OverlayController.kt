package com.gtarena.ui.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gtarena.model.Profile
import com.gtarena.service.GameSession
import com.gtarena.ui.theme.GTArenaTheme
import kotlin.math.hypot
import kotlin.math.max

internal class OverlayUiState {
    var session by mutableStateOf<GameSession?>(null)
    var expanded by mutableStateOf(false)
    var screenWidthDp by mutableIntStateOf(360)
    var screenHeightDp by mutableIntStateOf(640)
}

private class OverlayOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val viewModelStore = ViewModelStore()
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    init {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}

class OverlayController(
    private val context: Context,
    private val onProfileChange: (Profile) -> Unit
) {
    private val ui = OverlayUiState()
    private var windowContext: Context? = null
    private var wm: WindowManager? = null
    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null
    private var params: WindowManager.LayoutParams? = null

    private var density = 1f
    private var pillX = 0
    private var pillY = 0

    private val callbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            updateScreen()
        }

        override fun onLowMemory() {}
    }

    fun show(session: GameSession) {
        ui.session = session
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) return
        try {
            attach()
        } catch (e: Exception) {
            hide()
        }
    }

    fun hide() {
        val v = view
        val m = wm
        if (v != null && m != null) {
            try {
                m.removeView(v)
            } catch (_: Exception) {
            }
        }
        try {
            windowContext?.unregisterComponentCallbacks(callbacks)
        } catch (_: Exception) {
        }
        owner?.destroy()
        view = null
        owner = null
        wm = null
        windowContext = null
        params = null
        ui.expanded = false
    }

    private fun attach() {
        val dm = context.getSystemService(DisplayManager::class.java)
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
        val wctx = context.createDisplayContext(display)
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        val manager = wctx.getSystemService(WindowManager::class.java)
        windowContext = wctx
        wm = manager
        density = wctx.resources.displayMetrics.density
        pillX = dp(12)
        pillY = dp(12)
        ui.expanded = false
        updateScreen()
        wctx.registerComponentCallbacks(callbacks)

        val o = OverlayOwner()
        owner = o
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.END
        lp.x = pillX
        lp.y = pillY
        params = lp

        val cv = ComposeView(wctx)
        cv.setViewTreeLifecycleOwner(o)
        cv.setViewTreeSavedStateRegistryOwner(o)
        cv.setViewTreeViewModelStoreOwner(o)
        cv.setContent {
            GTArenaTheme {
                OverlayRoot(
                    state = ui,
                    onExpandedChange = { setExpanded(it) },
                    onProfileChange = onProfileChange
                )
            }
        }
        cv.setOnTouchListener(pillTouchListener())
        view = cv
        manager.addView(cv, lp)
    }

    private fun pillTouchListener(): android.view.View.OnTouchListener {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        return android.view.View.OnTouchListener { v, e ->
            if (ui.expanded) return@OnTouchListener false
            val lp = params ?: return@OnTouchListener false
            val manager = wm ?: return@OnTouchListener false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = lp.x
                    startY = lp.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) dragging = true
                    if (dragging) {
                        val bounds = manager.currentWindowMetrics.bounds
                        lp.x = (startX - dx).toInt().coerceIn(0, max(0, bounds.width() - v.width))
                        lp.y = (startY + dy).toInt().coerceIn(0, max(0, bounds.height() - v.height))
                        pillX = lp.x
                        pillY = lp.y
                        manager.updateViewLayout(v, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) setExpanded(true)
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun setExpanded(expanded: Boolean) {
        val lp = params ?: return
        val v = view ?: return
        val manager = wm ?: return
        if (expanded) {
            lp.gravity = Gravity.TOP or Gravity.START
            lp.x = dp(10)
            lp.y = dp(10)
        } else {
            lp.gravity = Gravity.TOP or Gravity.END
            lp.x = pillX
            lp.y = pillY
        }
        ui.expanded = expanded
        try {
            manager.updateViewLayout(v, lp)
        } catch (_: Exception) {
        }
    }

    private fun updateScreen() {
        val manager = wm ?: return
        val bounds = manager.currentWindowMetrics.bounds
        ui.screenWidthDp = (bounds.width() / density).toInt()
        ui.screenHeightDp = (bounds.height() / density).toInt()
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}
