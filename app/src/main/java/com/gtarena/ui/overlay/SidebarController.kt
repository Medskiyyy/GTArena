package com.gtarena.ui.overlay

import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gtarena.MainActivity
import com.gtarena.data.AppShortcut
import com.gtarena.data.GameTools
import com.gtarena.data.SettingsStore
import com.gtarena.data.Sidebar
import com.gtarena.ui.icons.GTIcons
import com.gtarena.ui.theme.BevelBrush
import com.gtarena.ui.theme.ChamferShape
import com.gtarena.ui.theme.Hairline
import com.gtarena.ui.theme.Label
import com.gtarena.ui.theme.OnLime
import com.gtarena.ui.theme.OutlineButton
import com.gtarena.ui.theme.Saira
import com.gtarena.ui.theme.SairaCond
import com.gtarena.ui.theme.SlantShape
import com.gtarena.ui.theme.TextPrimary
import com.gtarena.ui.theme.TextSecondary
import com.gtarena.ui.theme.TextSoft
import com.gtarena.ui.theme.AppIcon
import com.gtarena.ui.theme.color
import com.gtarena.ui.theme.mechaPanel
import kotlin.math.hypot
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RailShape = ChamferShape(12.dp)
private val FlyoutShape = ChamferShape(10.dp)
private val ButtonShape = ChamferShape(6.dp)
private val SlashShape = SlantShape(2.dp)

internal class SidebarUiState {
    var expanded by mutableStateOf(false)
    var onEnd by mutableStateOf(true)
    var flyout by mutableStateOf<String?>(null)
    var shortcuts by mutableStateOf<List<AppShortcut>?>(null)
}

internal class SidebarActions(
    val onOpen: (String) -> Unit,
    val onOpenFull: (String) -> Unit,
    val onLongPress: (String) -> Unit,
    val onShortcut: (AppShortcut) -> Unit,
    val onScreenshot: () -> Unit,
    val onRecord: () -> Unit,
    val onEdit: () -> Unit
)

class SidebarController(private val context: Context) {
    private val settings = SettingsStore.get(context)
    private val ui = SidebarUiState()
    private var wm: WindowManager? = null
    private var windowContext: Context? = null
    private var window: OverlayWindow? = null
    private var scope: CoroutineScope? = null
    private var density = 1f
    private var handleFrac = 0.4f

    private val actions = SidebarActions(
        onOpen = { pkg -> collapseThen { Sidebar.openFloating(context, pkg) } },
        onOpenFull = { pkg -> collapseThen { Sidebar.openFull(context, pkg) } },
        onLongPress = { pkg -> showFlyout(pkg) },
        onShortcut = { sc -> collapseThen { Sidebar.openShortcut(sc) } },
        onScreenshot = { screenshot() },
        onRecord = { collapseThen { if (GameTools.recording.value) GameTools.stopRecording(context) else GameTools.startRecording() } },
        onEdit = {
            setExpanded(false)
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_SCREEN, MainActivity.SCREEN_SIDEBAR)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )
        }
    )

    private val callbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            setExpanded(false)
        }

        override fun onLowMemory() {}
    }

    fun show() {
        if (window != null || !Settings.canDrawOverlays(context)) return
        try {
            val wctx = overlayContext(context)
            val manager = wctx.getSystemService(WindowManager::class.java)
            windowContext = wctx
            wm = manager
            density = wctx.resources.displayMetrics.density
            wctx.registerComponentCallbacks(callbacks)
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            val lp = overlayParams(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            lp.gravity = Gravity.TOP or (if (ui.onEnd) Gravity.END else Gravity.START)
            lp.y = handleY(manager)
            ui.expanded = false
            window = addOverlayWindow(wctx, manager, lp) { SidebarRoot(ui, actions) }.also {
                it.view.setOnTouchListener(touchListener())
            }
        } catch (e: Exception) {
            hide()
        }
    }

    fun hide() {
        scope?.cancel()
        scope = null
        removeOverlayWindow(wm, window)
        window = null
        try {
            windowContext?.unregisterComponentCallbacks(callbacks)
        } catch (_: Exception) {
        }
        windowContext = null
        wm = null
        ui.expanded = false
        ui.flyout = null
    }

    private fun collapseThen(block: suspend () -> Unit) {
        setExpanded(false)
        scope?.launch { block() }
    }

    private fun showFlyout(pkg: String) {
        ui.flyout = pkg
        ui.shortcuts = null
        scope?.launch {
            val list = Sidebar.shortcuts(pkg)
            if (ui.flyout == pkg) ui.shortcuts = list
        }
    }

    private fun screenshot() {
        setExpanded(false)
        val view = window?.view ?: return
        view.visibility = View.INVISIBLE
        scope?.launch {
            delay(400L)
            GameTools.screenshot(context)
            view.visibility = View.VISIBLE
        }
    }

    private fun touchListener(): View.OnTouchListener {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startY = 0
        var dragging = false
        return View.OnTouchListener { v, e ->
            val lp = window?.params ?: return@OnTouchListener false
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
                        handleFrac = lp.y / bounds.height().toFloat()
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
        val w = window ?: return
        val manager = wm ?: return
        val lp = w.params
        val side = if (ui.onEnd) Gravity.END else Gravity.START
        if (expanded) {
            lp.gravity = Gravity.CENTER_VERTICAL or side
            lp.x = dp(8)
            lp.y = 0
        } else {
            lp.gravity = Gravity.TOP or side
            lp.x = 0
            lp.y = handleY(manager)
            ui.flyout = null
            ui.shortcuts = null
        }
        ui.expanded = expanded
        try {
            manager.updateViewLayout(w.view, lp)
        } catch (_: Exception) {
        }
    }

    private fun handleY(manager: WindowManager): Int {
        val height = manager.currentWindowMetrics.bounds.height()
        return (height * handleFrac).toInt().coerceIn(0, max(0, height - dp(HANDLE_HEIGHT_DP)))
    }

    private fun dp(value: Int): Int = (value * density).toInt()
}

@Composable
private fun SidebarRoot(state: SidebarUiState, actions: SidebarActions) {
    val context = LocalContext.current
    val store = SettingsStore.get(context)
    val profile by store.profile.collectAsState()
    val accent = profile.color
    if (!state.expanded) {
        OverlayHandle(accent = accent, onEnd = state.onEnd)
        return
    }
    val apps by store.sidebarApps.collectAsState()
    val recording by GameTools.recording.collectAsState()
    val flyout = state.flyout
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.onEnd && flyout != null) Flyout(flyout, state.shortcuts, accent, actions)
        Rail(apps, flyout, recording, accent, actions)
        if (!state.onEnd && flyout != null) Flyout(flyout, state.shortcuts, accent, actions)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Rail(
    apps: List<String>,
    flyout: String?,
    recording: Boolean,
    accent: Color,
    actions: SidebarActions
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp - 48.dp
    Column(
        modifier = Modifier
            .width(64.dp)
            .heightIn(max = maxHeight)
            .mechaPanel(RailShape, accent)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(1f, 0.6f, 0.3f).forEach {
                Box(
                    Modifier
                        .size(width = 6.dp, height = 10.dp)
                        .clip(SlashShape)
                        .background(accent.copy(alpha = it))
                )
            }
        }
        Label("Melayang", fontSize = 9.sp)
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            apps.forEach { pkg ->
                AppIcon(
                    pkg = pkg,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(AppShape)
                        .then(if (pkg == flyout) Modifier.border(2.dp, accent, AppShape) else Modifier)
                        .combinedClickable(
                            role = Role.Button,
                            onClickLabel = "Buka melayang",
                            onLongClickLabel = "Pintasan",
                            onLongClick = { actions.onLongPress(pkg) },
                            onClick = { actions.onOpen(pkg) }
                        )
                )
            }
        }
        Box(
            Modifier
                .width(36.dp)
                .height(1.dp)
                .background(Hairline)
        )
        RailButton(GTIcons.Camera, "Screenshot", TextSoft, actions.onScreenshot)
        RailButton(GTIcons.Record, if (recording) "Stop rekam" else "Rekam layar", if (recording) accent else TextSoft, actions.onRecord)
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 36.dp)
                .clip(ButtonShape)
                .border(1.dp, OutlineButton, ButtonShape)
                .clickable(role = Role.Button, onClickLabel = "Atur isi sidebar", onClick = actions.onEdit),
            contentAlignment = Alignment.Center
        ) {
            Icon(GTIcons.Plus, contentDescription = "Atur isi sidebar", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun RailButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 40.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Flyout(pkg: String, shortcuts: List<AppShortcut>?, accent: Color, actions: SidebarActions) {
    val context = LocalContext.current
    val label = remember(pkg) {
        runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }
    Column(
        modifier = Modifier
            .width(212.dp)
            .mechaPanel(FlyoutShape)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(width = 7.dp, height = 12.dp)
                    .clip(SlashShape)
                    .background(accent)
            )
            Text(
                text = label.uppercase(),
                color = TextPrimary,
                fontFamily = SairaCond,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        when {
            shortcuts == null -> FlyoutNote("Lagi ngambil pintasan...")
            shortcuts.isEmpty() -> FlyoutNote("App ini nggak punya pintasan")
            else -> shortcuts.forEach { sc ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Hairline)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clickable(role = Role.Button) { actions.onShortcut(sc) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppIcon(
                        pkg = pkg,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(ChamferShape(5.dp))
                    )
                    Text(
                        text = sc.label,
                        color = TextPrimary,
                        fontFamily = Saira,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Hairline)
        )
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FlyoutButton("Melayang", true, accent, Modifier.weight(1f)) { actions.onOpen(pkg) }
            FlyoutButton("Layar penuh", false, accent, Modifier.weight(1f)) { actions.onOpenFull(pkg) }
        }
    }
}

@Composable
private fun FlyoutNote(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontFamily = Saira,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
    )
}

@Composable
private fun FlyoutButton(text: String, primary: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(ButtonShape)
            .then(
                if (primary) Modifier.background(accent)
                else Modifier.border(1.dp, BevelBrush, ButtonShape)
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (primary) OnLime else TextSoft,
            fontFamily = SairaCond,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            maxLines = 1
        )
    }
}
