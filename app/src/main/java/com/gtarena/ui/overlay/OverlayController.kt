package com.gtarena.ui.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gtarena.data.FpsReader
import com.gtarena.data.GameTools
import com.gtarena.data.SettingsStore
import com.gtarena.model.Profile
import com.gtarena.service.GameSession
import com.gtarena.ui.theme.GTArenaTheme
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class OverlayUiState {
    var session by mutableStateOf<GameSession?>(null)
    var expanded by mutableStateOf(false)
    var onEnd by mutableStateOf(true)
    var fps by mutableStateOf<Int?>(null)
    val samples = mutableStateListOf<Int>()
    var bubble by mutableStateOf(false)
    var crosshair by mutableStateOf(false)
    var sessionStart by mutableLongStateOf(0L)
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

private class OverlayWindow(val view: ComposeView, val owner: OverlayOwner, val params: WindowManager.LayoutParams)

class OverlayController(
    private val context: Context,
    private val onProfileChange: (Profile) -> Unit,
    private val onBypassChange: (Boolean) -> Unit,
    private val onRefreshChange: (Int) -> Unit,
    private val onThermalChange: (Int) -> Unit
) {
    private val settings = SettingsStore.get(context)
    private val ui = OverlayUiState()
    private var windowContext: Context? = null
    private var wm: WindowManager? = null
    private var scope: CoroutineScope? = null
    private var main: OverlayWindow? = null
    private var bubble: OverlayWindow? = null
    private var crosshair: OverlayWindow? = null
    private var splash: OverlayWindow? = null
    private var splashWm: WindowManager? = null
    private var splashPkg: String? = null
    private var splashAt = 0L

    private var density = 1f
    private var handleY = 0
    private var bubbleX = 0
    private var bubbleY = 0

    private val actions = OverlayActions(
        onProfile = { onProfileChange(it) },
        onBypass = { onBypassChange(it) },
        onRefresh = { onRefreshChange(it) },
        onThermal = { onThermalChange(it) },
        onBubble = { settings.setFpsBubble(it) },
        onCrosshair = { setCrosshair(it) },
        onScreenshot = { screenshot() }
    )

    private val callbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            setExpanded(ui.expanded)
            clampBubble()
        }

        override fun onLowMemory() {}
    }

    fun show(session: GameSession) {
        if (ui.session?.pkg != session.pkg) {
            ui.sessionStart = SystemClock.elapsedRealtime()
            ui.samples.clear()
            ui.fps = null
        }
        ui.session = session
        if (main != null) return
        if (!Settings.canDrawOverlays(context)) return
        try {
            attach()
            setCrosshair(session.crosshair)
            splash(session.pkg, session.profile)
        } catch (e: Exception) {
            hide()
        }
    }

    fun hide() {
        scope?.cancel()
        scope = null
        remove(main)
        remove(bubble)
        remove(crosshair)
        main = null
        bubble = null
        crosshair = null
        try {
            windowContext?.unregisterComponentCallbacks(callbacks)
        } catch (_: Exception) {
        }
        wm = null
        windowContext = null
        ui.expanded = false
        ui.bubble = false
        ui.crosshair = false
        ui.session = null
    }

    private fun overlayContext(): Context {
        val dm = context.getSystemService(DisplayManager::class.java)
        return context.createDisplayContext(dm.getDisplay(Display.DEFAULT_DISPLAY))
            .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    }

    fun splash(pkg: String, profile: Profile) {
        val now = SystemClock.elapsedRealtime()
        if (splashPkg == pkg && now - splashAt < SPLASH_REPEAT_MS) return
        if (!Settings.canDrawOverlays(context)) return
        removeSplash()
        splashPkg = pkg
        splashAt = now
        try {
            val wctx = overlayContext()
            val manager = wctx.getSystemService(WindowManager::class.java)
            val lp = baseParams()
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            lp.gravity = Gravity.CENTER
            splashWm = manager
            splash = addWindow(lp, wctx, manager) { OverlaySplash(profile) { removeSplash() } }
        } catch (e: Exception) {
            removeSplash()
        }
    }

    fun warmUp() {
        if (!Settings.canDrawOverlays(context) || splash != null) return
        try {
            val wctx = overlayContext()
            val manager = wctx.getSystemService(WindowManager::class.java)
            val lp = baseParams(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            lp.width = 1
            lp.height = 1
            lp.alpha = 0f
            splashWm = manager
            splash = addWindow(lp, wctx, manager) { OverlaySplash(Profile.SEIMBANG) { removeSplash() } }
        } catch (e: Exception) {
            removeSplash()
        }
    }

    private fun removeSplash() {
        val window = splash ?: return
        try {
            splashWm?.removeView(window.view)
        } catch (_: Exception) {
        }
        window.owner.destroy()
        splash = null
        splashWm = null
    }

    private fun attach() {
        val wctx = overlayContext()
        windowContext = wctx
        wm = wctx.getSystemService(WindowManager::class.java)
        density = wctx.resources.displayMetrics.density
        handleY = dp(96)
        loadBubblePos()
        ui.expanded = false
        ui.onEnd = true
        wctx.registerComponentCallbacks(callbacks)

        val lp = baseParams(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        lp.gravity = Gravity.TOP or Gravity.END
        lp.x = 0
        lp.y = handleY
        main = addWindow(lp) { OverlayRoot(state = ui, actions = actions) }.also {
            it.view.setOnTouchListener(handleTouchListener())
        }

        val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = s
        s.launch {
            settings.fpsBubble.collect { setBubble(it) }
        }
        s.launch {
            while (true) {
                val session = ui.session
                if (session != null && (ui.expanded || ui.bubble)) {
                    val v = FpsReader.read(session.pkg)
                    ui.fps = v
                    if (v != null) {
                        ui.samples.add(v)
                        if (ui.samples.size > 40) ui.samples.removeAt(0)
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun baseParams(extraFlags: Int = 0): WindowManager.LayoutParams {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                extraFlags,
            PixelFormat.TRANSLUCENT
        )
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        return lp
    }

    private fun addWindow(
        lp: WindowManager.LayoutParams,
        wctx: Context = windowContext!!,
        manager: WindowManager = wm!!,
        content: @Composable () -> Unit
    ): OverlayWindow {
        val owner = OverlayOwner()
        val view = ComposeView(wctx)
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setContent { GTArenaTheme(content) }
        manager.addView(view, lp)
        return OverlayWindow(view, owner, lp)
    }

    private fun remove(window: OverlayWindow?) {
        window ?: return
        try {
            wm?.removeView(window.view)
        } catch (_: Exception) {
        }
        window.owner.destroy()
    }

    private fun setBubble(on: Boolean) {
        ui.bubble = on
        if (on && bubble == null && wm != null) {
            val lp = baseParams()
            lp.gravity = Gravity.TOP or Gravity.START
            lp.x = bubbleX
            lp.y = bubbleY
            bubble = addWindow(lp) { OverlayBubble(ui) }.also {
                it.view.setOnTouchListener(bubbleTouchListener())
            }
        } else if (!on) {
            remove(bubble)
            bubble = null
        }
    }

    private fun setCrosshair(on: Boolean) {
        ui.crosshair = on
        if (on && crosshair == null && wm != null) {
            val lp = baseParams(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            lp.gravity = Gravity.CENTER
            crosshair = addWindow(lp) { OverlayCrosshair(ui) }
        } else if (!on) {
            remove(crosshair)
            crosshair = null
        }
    }

    private fun screenshot() {
        setExpanded(false)
        val views = listOfNotNull(main, bubble, crosshair).map { it.view }
        views.forEach { it.visibility = View.INVISIBLE }
        scope?.launch {
            delay(400L)
            GameTools.screenshot(context)
            views.forEach { it.visibility = View.VISIBLE }
        }
    }

    private fun bubbleTouchListener(): View.OnTouchListener {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        return View.OnTouchListener { v, e ->
            val lp = bubble?.params ?: return@OnTouchListener false
            val manager = wm ?: return@OnTouchListener false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = lp.x
                    startY = lp.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val bounds = manager.currentWindowMetrics.bounds
                    lp.x = (startX + e.rawX - downX).toInt().coerceIn(0, max(0, bounds.width() - v.width))
                    lp.y = (startY + e.rawY - downY).toInt().coerceIn(0, max(0, bounds.height() - v.height))
                    bubbleX = lp.x
                    bubbleY = lp.y
                    manager.updateViewLayout(v, lp)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    settings.setBubblePos(isLandscape(), bubbleX, bubbleY)
                    true
                }
                else -> true
            }
        }
    }

    private fun isLandscape(): Boolean {
        val bounds = wm?.currentWindowMetrics?.bounds ?: return false
        return bounds.width() > bounds.height()
    }

    private fun loadBubblePos() {
        val saved = settings.bubblePos(isLandscape())
        val width = wm?.currentWindowMetrics?.bounds?.width() ?: 0
        bubbleX = saved?.first ?: (width * 0.36f).toInt()
        bubbleY = saved?.second ?: dp(2)
    }

    private fun clampBubble() {
        loadBubblePos()
        val b = bubble ?: return
        val manager = wm ?: return
        val bounds = manager.currentWindowMetrics.bounds
        b.params.x = bubbleX.coerceIn(0, max(0, bounds.width() - b.view.width))
        b.params.y = bubbleY.coerceIn(0, max(0, bounds.height() - b.view.height))
        try {
            manager.updateViewLayout(b.view, b.params)
        } catch (_: Exception) {
        }
    }

    private fun handleTouchListener(): View.OnTouchListener {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startY = 0
        var dragging = false
        return View.OnTouchListener { v, e ->
            val lp = main?.params ?: return@OnTouchListener false
            val manager = wm ?: return@OnTouchListener false
            if (ui.expanded) {
                if (e.actionMasked == MotionEvent.ACTION_OUTSIDE) setExpanded(false)
                return@OnTouchListener false
            }
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startY = lp.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = e.rawY - downY
                    if (!dragging && hypot(e.rawX - downX, dy) > slop) dragging = true
                    if (dragging) {
                        val bounds = manager.currentWindowMetrics.bounds
                        lp.y = (startY + dy).toInt().coerceIn(0, max(0, bounds.height() - v.height))
                        handleY = lp.y
                        manager.updateViewLayout(v, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        ui.onEnd = e.rawX > manager.currentWindowMetrics.bounds.width() / 2f
                        setExpanded(false)
                    } else {
                        setExpanded(true)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> true
                else -> false
            }
        }
    }

    private fun setExpanded(expanded: Boolean) {
        val window = main ?: return
        val manager = wm ?: return
        val lp = window.params
        val screenHeight = manager.currentWindowMetrics.bounds.height()
        lp.gravity = Gravity.TOP or (if (ui.onEnd) Gravity.END else Gravity.START)
        if (expanded) {
            lp.x = dp(8)
            lp.y = handleY.coerceIn(dp(8), max(dp(8), screenHeight - dp(PANEL_HEIGHT_DP + 8)))
            lp.width = dp(PANEL_WIDTH_DP)
        } else {
            handleY = handleY.coerceIn(0, max(0, screenHeight - dp(HANDLE_HEIGHT_DP)))
            lp.x = 0
            lp.y = handleY
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT
        }
        ui.expanded = expanded
        try {
            manager.updateViewLayout(window.view, lp)
        } catch (_: Exception) {
        }
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    private companion object {
        const val SPLASH_REPEAT_MS = 15_000L
    }
}
