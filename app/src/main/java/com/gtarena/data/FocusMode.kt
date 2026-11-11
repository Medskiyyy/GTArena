package com.gtarena.data

import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FocusMode {
    private const val DND_MARKER = "/data/adb/gtarena/dnd"
    private val mutex = Mutex()
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private var prevZen: Int? = null
    private var prevBrightnessMode: Int? = null

    suspend fun enable() = mutex.withLock {
        if (_enabled.value) return@withLock
        prevZen = RootShell.run("settings get global zen_mode")?.trim()?.toIntOrNull()
        prevBrightnessMode = RootShell.run("settings get system screen_brightness_mode")?.trim()?.toIntOrNull()
        if (prevZen == 0) RootShell.run("cmd notification set_dnd priority && mkdir -p /data/adb/gtarena && touch $DND_MARKER")
        if (prevBrightnessMode == 1) RootShell.run("settings put system screen_brightness_mode 0")
        RootShell.run("am kill-all")
        _enabled.value = true
    }

    suspend fun disable() = mutex.withLock {
        if (!_enabled.value) return@withLock
        if (prevZen == 0) RootShell.run("cmd notification set_dnd off; rm -f $DND_MARKER")
        if (prevBrightnessMode == 1) RootShell.run("settings put system screen_brightness_mode 1")
        prevZen = null
        prevBrightnessMode = null
        _enabled.value = false
    }

    private val _brightnessLocked = MutableStateFlow(false)
    val brightnessLocked: StateFlow<Boolean> = _brightnessLocked

    suspend fun refreshBrightnessLock() {
        val mode = RootShell.run("settings get system screen_brightness_mode")?.trim()?.toIntOrNull() ?: return
        _brightnessLocked.value = mode == 0
    }

    suspend fun setBrightnessLocked(locked: Boolean) {
        if (RootShell.run("settings put system screen_brightness_mode ${if (locked) 0 else 1}") != null) {
            _brightnessLocked.value = locked
        }
    }

    suspend fun recover() {
        RootShell.run("if [ -f $DND_MARKER ]; then cmd notification set_dnd off; rm -f $DND_MARKER; fi")
    }

    suspend fun cleanRam(): Int {
        val before = memAvailableKb()
        RootShell.run("am kill-all")
        delay(700L)
        return ((memAvailableKb() - before) / 1024L).toInt().coerceAtLeast(0)
    }

    private fun memAvailableKb(): Long = runCatching {
        File("/proc/meminfo").useLines { lines ->
            lines.first { it.startsWith("MemAvailable:") }.filter(Char::isDigit).toLong()
        }
    }.getOrDefault(0L)
}
