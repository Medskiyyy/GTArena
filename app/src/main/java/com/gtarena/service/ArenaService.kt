package com.gtarena.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gtarena.MainActivity
import com.gtarena.R
import com.gtarena.data.FocusMode
import com.gtarena.data.ForegroundAppWatcher
import com.gtarena.data.GameConfig
import com.gtarena.data.GameRepository
import com.gtarena.data.LaunchWatcher
import com.gtarena.data.ProfileEngine
import com.gtarena.data.RootShell
import com.gtarena.data.SettingsStore
import com.gtarena.model.Profile
import com.gtarena.model.label
import com.gtarena.data.Sidebar
import com.gtarena.ui.overlay.OverlayController
import com.gtarena.ui.overlay.SidebarController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class GameSession(
    val pkg: String,
    val label: String,
    val profile: Profile,
    val bypass: Boolean = false,
    val refreshHz: Int = 120,
    val crosshair: Boolean = false,
    val thermal: Int = 0,
    val focus: Boolean = false,
    val startedAt: Long = SystemClock.elapsedRealtime(),
    val resumed: Boolean = false
)

private data class PausedSession(
    val session: GameSession,
    val pausedAt: Long,
    val configSnapshot: GameConfig
)

class ArenaService : LifecycleService() {

    private lateinit var settings: SettingsStore
    private lateinit var controller: OverlayController
    private lateinit var sidebar: SidebarController
    private val engineLock = Mutex()
    private val sessionLock = Mutex()
    private val games = HashMap<String, String>()
    private val pkgChanges = MutableStateFlow(0)

    @Volatile
    private var pausedSession: PausedSession? = null

    @Volatile
    private var gamePkgs: Set<String> = emptySet()

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart
            if (pkg != null) games.remove(pkg)
            pkgChanges.value++
        }
    }

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore.get(this)
        controller = OverlayController(
            this,
            { switchProfile(it) },
            { switchBypass(it) },
            { switchRefresh(it) },
            { switchThermal(it) },
            { switchFocus(it) },
            { switchCrosshair(it) }
        )
        sidebar = SidebarController(this)
        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, pkgReceiver, pkgFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        _running.value = true
        controller.warmUp()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )

        lifecycleScope.launch {
            FocusMode.recover()
            while (true) {
                ProfileEngine.heartbeat()
                val level = _session.value?.thermal ?: 0
                if (level > 0 && (ProfileEngine.batteryTempC() ?: 0) >= THERMAL_GUARD_C[level]) {
                    switchThermal(0, persist = false)
                }
                delay(HEARTBEAT_MS)
            }
        }
        lifecycleScope.launch {
            settings.profile.collect { profile ->
                if (_session.value == null) applyLocked(profile, null)
            }
        }
        lifecycleScope.launch {
            combine(_session, settings.profile) { s, p -> s to p }.collect { notifyUpdate() }
        }
        lifecycleScope.launch {
            combine(_session, settings.overlayEnabled) { s, e -> s to e }.collect { (s, enabled) ->
                if (s != null && enabled) controller.show(s) else controller.hide()
            }
        }
        lifecycleScope.launch {
            if (!settings.sidebarImported) {
                val rom = Sidebar.importRomApps().filter { packageManager.getLaunchIntentForPackage(it) != null }
                if (rom.isNotEmpty()) settings.setSidebarApps(rom)
            }
            settings.sidebarEnabled.collect { Sidebar.setRomSideline(!it) }
        }
        lifecycleScope.launch {
            combine(_session, settings.sidebarEnabled) { s, e -> s == null && e }.collect { show ->
                if (show) sidebar.show() else sidebar.hide()
            }
        }
        lifecycleScope.launch {
            ForegroundAppWatcher.foregroundPackage().collect { handleForeground(it) }
        }
        lifecycleScope.launch {
            combine(settings.extraGames, settings.autoApply, pkgChanges) { _, auto, _ -> auto }.collect { auto ->
                if (!auto) pausedSession = null
                val pkgs = if (auto) GameRepository(this@ArenaService).installedGames().map { it.pkg } else emptyList()
                gamePkgs = pkgs.toSet()
                ProfileEngine.writeGames(pkgs)
            }
        }
        lifecycleScope.launch {
            LaunchWatcher.launches().collect { pkg ->
                if (pkg in gamePkgs && _session.value?.pkg != pkg) announce(pkg)
            }
        }
    }

    private fun closeUi() {
        getSystemService(ActivityManager::class.java).appTasks.forEach { it.finishAndRemoveTask() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val game = intent?.getStringExtra(EXTRA_GAME)
        if (game != null && settings.autoApply.value) announce(game)
        return START_STICKY
    }

    private fun canResumePaused(game: String): Boolean {
        val paused = pausedSession ?: return false
        return paused.session.pkg == game &&
            SystemClock.elapsedRealtime() - paused.pausedAt <= SESSION_GRACE_MS &&
            settings.gameConfig(game) == paused.configSnapshot
    }

    private fun announce(game: String) {
        val resuming = canResumePaused(game)
        if (!resuming) controller.splash(game, settings.gameConfig(game).profile)
        lifecycleScope.launch {
            if (!resuming) delay(SPLASH_HEAD_START_MS)
            handleForeground(game)
            delay(CONFIRM_MS)
            val top = ForegroundAppWatcher.current()
            if (top != null && top != game && _session.value?.pkg == game) handleForeground(top)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(pkgReceiver)
        } catch (_: Exception) {
        }
        controller.hide()
        sidebar.hide()
        _session.value = null
        _running.value = false
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            FocusMode.disable()
            ProfileEngine.release()
            Sidebar.setRomSideline(true)
        }
        super.onDestroy()
    }

    private suspend fun applyLocked(
        profile: Profile,
        refreshHz: Int?,
        bypassCharge: Boolean = false,
        thermalLevel: Int = 0
    ) {
        engineLock.withLock { ProfileEngine.apply(profile, refreshHz, bypassCharge, thermalLevel) }
    }

    private suspend fun handleForeground(pkg: String) = sessionLock.withLock {
        if (pkg in IGNORED) return@withLock
        val current = _session.value
        if (current?.pkg == pkg) return@withLock
        if (current != null) endSession()
        if (!settings.autoApply.value) {
            pausedSession = null
            return@withLock
        }
        val label = gameLabel(pkg) ?: return@withLock
        closeUi()
        val now = SystemClock.elapsedRealtime()
        val paused = pausedSession
        pausedSession = null
        val cfg = settings.gameConfig(pkg)
        val resumed = if (
            paused != null &&
            paused.session.pkg == pkg &&
            now - paused.pausedAt <= SESSION_GRACE_MS &&
            cfg == paused.configSnapshot &&
            RootShell.run("pidof '$pkg'") != null
        ) {
            paused.session.copy(
                startedAt = paused.session.startedAt + (now - paused.pausedAt),
                resumed = true
            )
        } else {
            GameSession(
                pkg = pkg,
                label = label,
                profile = cfg.profile,
                bypass = cfg.bypassCharge,
                refreshHz = cfg.refreshHz,
                crosshair = cfg.crosshair,
                thermal = cfg.thermalLevel,
                focus = cfg.focus,
                startedAt = now
            )
        }
        _session.value = resumed
        applyLocked(resumed.profile, resumed.refreshHz, resumed.bypass, resumed.thermal)
        if (resumed.focus) FocusMode.enable()
    }

    private suspend fun endSession() {
        val current = _session.value ?: return
        pausedSession = if (settings.autoApply.value) {
            PausedSession(current, SystemClock.elapsedRealtime(), settings.gameConfig(current.pkg))
        } else {
            null
        }
        _session.value = null
        FocusMode.disable()
        applyLocked(settings.profile.value, null)
    }

    private fun updateSession(persist: Boolean = true, transform: (GameSession) -> GameSession) {
        lifecycleScope.launch {
            sessionLock.withLock {
                val current = _session.value ?: return@withLock
                val next = transform(current)
                _session.value = next
                if (persist) {
                    settings.setGameConfig(
                        next.pkg,
                        settings.gameConfig(next.pkg).copy(
                            profile = next.profile,
                            refreshHz = next.refreshHz,
                            focus = next.focus,
                            bypassCharge = next.bypass,
                            crosshair = next.crosshair,
                            thermalLevel = next.thermal
                        )
                    )
                }
                if (next.focus != current.focus) {
                    if (next.focus) FocusMode.enable() else FocusMode.disable()
                }
                if (
                    next.profile != current.profile ||
                    next.refreshHz != current.refreshHz ||
                    next.bypass != current.bypass ||
                    next.thermal != current.thermal
                ) {
                    applyLocked(next.profile, next.refreshHz, next.bypass, next.thermal)
                }
            }
        }
    }

    private fun switchProfile(profile: Profile) = updateSession { it.copy(profile = profile) }

    private fun switchBypass(on: Boolean) = updateSession { it.copy(bypass = on) }

    private fun switchRefresh(hz: Int) = updateSession { it.copy(refreshHz = hz) }

    private fun switchThermal(level: Int, persist: Boolean = true) = updateSession(persist) { it.copy(thermal = level) }

    private fun switchFocus(on: Boolean) = updateSession { it.copy(focus = on) }

    private fun switchCrosshair(on: Boolean) = updateSession { it.copy(crosshair = on) }

    private suspend fun gameLabel(pkg: String): String? {
        games[pkg]?.let { return it }
        val extra = pkg in settings.extraGames.value
        GameRepository(this).installedGames().forEach { games[it.pkg] = it.label }
        games[pkg]?.let { return it }
        if (!extra) return null
        return runCatching {
            packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString()
        }.getOrDefault(pkg)
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "GT Arena", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    private fun buildNotification(): Notification {
        val s = _session.value
        val text = if (s != null) "${s.label}, profil ${s.profile.label}" else "Profil ${settings.profile.value.label}"
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_arena)
            .setContentTitle("GT Arena aktif")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun notifyUpdate() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
    }

    companion object {
        private const val CHANNEL_ID = "arena"
        private const val EXTRA_GAME = "game"
        private const val NOTIFICATION_ID = 1
        private const val HEARTBEAT_MS = 10_000L
        private const val CONFIRM_MS = 4_000L
        private const val SPLASH_HEAD_START_MS = 450L
        private const val SESSION_GRACE_MS = 180_000L
        private val THERMAL_GUARD_C = intArrayOf(0, 46, 50)
        private val IGNORED = setOf("com.android.systemui", "android")

        private val _session = MutableStateFlow<GameSession?>(null)
        val session: StateFlow<GameSession?> = _session

        private val _uiVisible = MutableStateFlow(false)

        fun setUiVisible(visible: Boolean) {
            _uiVisible.value = visible
        }

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context, game: String? = null) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ArenaService::class.java).putExtra(EXTRA_GAME, game)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ArenaService::class.java))
        }
    }
}
