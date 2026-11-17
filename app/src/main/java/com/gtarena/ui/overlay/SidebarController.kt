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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
private const val APP_LIST_MAX_DP = 236
private const val SIDEBAR_HANDLE_W_DP = 22
private const val SIDEBAR_HANDLE_H_DP = 96

internal class SidebarUiState {
    var expanded by mutableStateOf(false)
    var onEnd by mutableStateOf(true)
    var availHeightDp by mutableIntStateOf(600)
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
    private var lastExpandedHeight = 0

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
            val manager = wm ?: return
            lastExpandedHeight = 0
            loadEdgePos(manager)
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
            loadEdgePos(manager)
            wctx.registerComponentCallbacks(callbacks)
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            val lp = overlayParams(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            lp.gravity = Gravity.TOP or (if (ui.onEnd) Gravity.END else Gravity.START)
            lp.y = edgeHandleY(manager, dp(SIDEBAR_HANDLE_H_DP), handleFrac)
            ui.expanded = false
            window = addOverlayWindow(wctx, manager, lp) { SidebarRoot(ui, actions) }.also {
                it.view.setOnTouchListener(touchListener())
                excludeSystemGestures(it.view)
                reclampExpandedOnLayout(
                    view = it.view,
                    handleHeightPx = dp(SIDEBAR_HANDLE_H_DP),
                    isExpanded = { ui.expanded },
                    window = { window },
                    manager = { wm },
                    onHeight = { h -> lastExpandedHeight = h },
                    desiredY = { mgr, h -> clampedExpandedY(mgr, dp(SIDEBAR_HANDLE_H_DP), handleFrac, h, dp(8)) }
                )
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
        lastExpandedHeight = 0
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
            GameTools.screenshot()
            view.visibility = View.VISIBLE
        }
    }

    private fun touchListener(): View.OnTouchListener {
        val slop = ViewConfiguration.get(context).scaledTouchSlop
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        return View.OnTouchListener { v, e ->
            val lp = window?.params ?: return@OnTouchListener false
            val manager = wm ?: return@OnTouchListener false
            if (ui.expanded) {
                if (e.actionMasked == MotionEvent.ACTION_OUTSIDE) setExpanded(false)
                return@OnTouchListener false
            }
            val bounds = manager.currentWindowMetrics.bounds
            val insets = systemInsets(manager)
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    startX = if (ui.onEnd) bounds.width() - v.width else 0
                    startY = lp.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) {
                        dragging = true
                        lp.gravity = Gravity.TOP or Gravity.START
                    }
                    if (dragging) {
                        val minY = insets.top
                        val maxY = max(minY, bounds.height() - insets.bottom - v.height)
                        lp.x = (startX + dx).toInt().coerceIn(0, max(0, bounds.width() - v.width))
                        lp.y = (startY + dy).toInt().coerceIn(minY, maxY)
                        val span = max(1, maxY - minY)
                        handleFrac = ((lp.y - minY) / span.toFloat()).coerceIn(0f, 1f)
                        manager.updateViewLayout(v, lp)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        ui.onEnd = e.rawX > bounds.width() / 2f
                        settings.setEdgePos(true, isLandscape(), ui.onEnd, handleFrac)
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
        lp.gravity = Gravity.TOP or side
        if (expanded) {
            updateAvailHeight(manager)
            val insets = systemInsets(manager)
            lp.x = dp(8) + (if (ui.onEnd) insets.right else insets.left)
            val estHeight = when {
                w.view.height > dp(SIDEBAR_HANDLE_H_DP) -> w.view.height
                lastExpandedHeight > 0 -> lastExpandedHeight
                else -> dp(340)
            }
            lp.y = clampedExpandedY(manager, dp(SIDEBAR_HANDLE_H_DP), handleFrac, estHeight, dp(8))
        } else {
            lp.x = 0
            lp.y = edgeHandleY(manager, dp(SIDEBAR_HANDLE_H_DP), handleFrac)
            ui.flyout = null
            ui.shortcuts = null
        }
        ui.expanded = expanded
        try {
            manager.updateViewLayout(w.view, lp)
        } catch (_: Exception) {
        }
    }

    private fun isLandscape(): Boolean {
        val bounds = wm?.currentWindowMetrics?.bounds ?: return false
        return bounds.width() > bounds.height()
    }

    private fun loadEdgePos(manager: WindowManager) {
        val pos = settings.edgePos(isSidebar = true, landscape = isLandscape())
        ui.onEnd = pos.onEnd
        handleFrac = pos.yFrac
        updateAvailHeight(manager)
    }

    private fun updateAvailHeight(manager: WindowManager) {
        val insets = systemInsets(manager)
        val usablePx = manager.currentWindowMetrics.bounds.height() - insets.top - insets.bottom
        ui.availHeightDp = max(0, (usablePx / density).toInt() - 16)
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
        SidebarHandle(accent = accent, onEnd = state.onEnd)
        return
    }
    LaunchedEffect(Unit) { GameTools.syncRecording(context) }
    val apps by store.sidebarApps.collectAsState()
    val recording by GameTools.recording.collectAsState()
    val flyout = state.flyout
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.onEnd && flyout != null) Flyout(flyout, state.shortcuts, state.availHeightDp, accent, actions)
        Rail(apps, flyout, recording, state.availHeightDp, accent, actions)
        if (!state.onEnd && flyout != null) Flyout(flyout, state.shortcuts, state.availHeightDp, accent, actions)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Rail(
    apps: List<String>,
    flyout: String?,
    recording: Boolean,
    availHeightDp: Int,
    accent: Color,
    actions: SidebarActions
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .mechaPanel(RailShape, accent)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(1f, 0.6f, 0.3f).forEach {
                Box(
                    Modifier
                        .size(width = 5.dp, height = 10.dp)
                        .clip(SlashShape)
                        .background(accent.copy(alpha = it))
                )
            }
        }
        val listMaxDp = minOf(APP_LIST_MAX_DP, max(88, availHeightDp - 176))
        Column(
            modifier = Modifier
                .heightIn(max = listMaxDp.dp)
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(AppShape)
                    .border(1.dp, OutlineButton, AppShape)
                    .clickable(role = Role.Button, onClickLabel = "Atur isi sidebar", onClick = actions.onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(GTIcons.Plus, contentDescription = "Atur isi sidebar", tint = TextSecondary, modifier = Modifier.size(18.dp))
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
    }
}


@Composable
private fun RailButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(AppShape)
            .border(1.dp, BevelBrush, AppShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SidebarHandle(accent: Color, onEnd: Boolean) {
    Box(
        modifier = Modifier.size(width = SIDEBAR_HANDLE_W_DP.dp, height = SIDEBAR_HANDLE_H_DP.dp),
        contentAlignment = if (onEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(76.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.35f), accent, accent, accent.copy(alpha = 0.35f))
                    )
                )
        )
    }
}

@Composable
private fun Flyout(
    pkg: String,
    shortcuts: List<AppShortcut>?,
    availHeightDp: Int,
    accent: Color,
    actions: SidebarActions
) {
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
        Column(
            modifier = Modifier
                .heightIn(max = max(88, availHeightDp - 120).dp)
                .verticalScroll(rememberScrollState())
        ) {
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