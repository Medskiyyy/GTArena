package com.gtarena.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Process
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gtarena.MainActivity
import com.gtarena.R
import com.gtarena.data.FocusMode
import com.gtarena.data.ForegroundAppWatcher
import com.gtarena.data.GameRepository
import com.gtarena.data.ProfileEngine
import com.gtarena.data.SettingsStore
import com.gtarena.model.Profile
import com.gtarena.model.label
import com.gtarena.ui.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class GameSession(
    val pkg: String,
    val label: String,
    val profile: Profile,
    val bypass: Boolean = false,
    val refreshHz: Int = 120,
    val crosshair: Boolean = false,
    val thermal: Int = 0
)

class ArenaService : LifecycleService() {

    private lateinit var settings: SettingsStore
    private lateinit var controller: OverlayController
    private val engineLock = Mutex()
    private val sessionLock = Mutex()
    private val games = HashMap<String, String>()

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore.get(this)
        controller = OverlayController(this, { switchProfile(it) }, { switchBypass(it) }, { switchRefresh(it) }, { switchThermal(it) })
        _running.value = true
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
                if (level > 0 && (ProfileEngine.batteryTempC() ?: 0) >= THERMAL_GUARD_C[level]) switchThermal(0)
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
            ForegroundAppWatcher.foregroundPackage().collect { handleForeground(it) }
        }
        lifecycleScope.launch {
            combine(settings.extraGames, settings.autoApply) { _, auto -> auto }.collect { auto ->
                val pkgs = if (auto) GameRepository(this@ArenaService).installedGames().map { it.pkg } else emptyList()
                ProfileEngine.writeGames(pkgs)
            }
        }
        lifecycleScope.launch {
            combine(_session, _uiVisible) { s, ui -> s == null && !ui }.collectLatest { idle ->
                if (idle) {
                    delay(IDLE_GRACE_MS)
                    shutdown()
                }
            }
        }
    }

    private suspend fun shutdown() = withContext(NonCancellable) {
        controller.hide()
        FocusMode.disable()
        engineLock.withLock { ProfileEngine.release() }
        if (_uiVisible.value || _session.value != null) {
            if (_session.value == null) applyLocked(settings.profile.value, null)
            return@withContext
        }
        closeUi()
        ServiceCompat.stopForeground(this@ArenaService, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        Process.killProcess(Process.myPid())
    }

    private fun closeUi() {
        getSystemService(ActivityManager::class.java).appTasks.forEach { it.finishAndRemoveTask() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val game = intent?.getStringExtra(EXTRA_GAME)
        if (game != null && settings.autoApply.value) {
            controller.splash(game, settings.gameConfig(game).profile)
            if (!_uiVisible.value) lifecycleScope.launch { handleForeground(game) }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        controller.hide()
        _session.value = null
        _running.value = false
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            FocusMode.disable()
            ProfileEngine.release()
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
        if (!settings.autoApply.value) return@withLock
        val label = gameLabel(pkg) ?: return@withLock
        val cfg = settings.gameConfig(pkg)
        _session.value = GameSession(pkg, label, cfg.profile, cfg.bypassCharge, cfg.refreshHz, cfg.crosshair, cfg.thermalLevel)
        closeUi()
        applyLocked(cfg.profile, cfg.refreshHz, cfg.bypassCharge, cfg.thermalLevel)
        if (cfg.focus) FocusMode.enable()
    }

    private suspend fun endSession() {
        _session.value = null
        FocusMode.disable()
        applyLocked(settings.profile.value, null)
    }

    private fun switchProfile(profile: Profile) {
        lifecycleScope.launch {
            val s = _session.value ?: return@launch
            _session.value = s.copy(profile = profile)
            applyLocked(profile, s.refreshHz, s.bypass, s.thermal)
        }
    }

    private fun switchBypass(on: Boolean) {
        lifecycleScope.launch {
            val s = _session.value ?: return@launch
            _session.value = s.copy(bypass = on)
            applyLocked(s.profile, s.refreshHz, on, s.thermal)
        }
    }

    private fun switchRefresh(hz: Int) {
        lifecycleScope.launch {
            val s = _session.value ?: return@launch
            _session.value = s.copy(refreshHz = hz)
            applyLocked(s.profile, hz, s.bypass, s.thermal)
        }
    }

    private fun switchThermal(level: Int) {
        lifecycleScope.launch {
            val s = _session.value ?: return@launch
            _session.value = s.copy(thermal = level)
            applyLocked(s.profile, s.refreshHz, s.bypass, level)
        }
    }

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
        private const val IDLE_GRACE_MS = 8_000L
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
