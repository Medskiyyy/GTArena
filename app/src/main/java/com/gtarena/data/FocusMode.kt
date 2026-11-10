package com.gtarena.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object FocusMode {
    private val mutex = Mutex()
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled

    private var prevZen: Int? = null
    private var prevBrightnessMode: Int? = null

    suspend fun enable() = mutex.withLock {
        if (_enabled.value) return@withLock
        prevZen = RootShell.run("settings get global zen_mode")?.trim()?.toIntOrNull()
        prevBrightnessMode = RootShell.run("settings get system screen_brightness_mode")?.trim()?.toIntOrNull()
        if (prevZen == 0) RootShell.run("cmd notification set_dnd priority")
        RootShell.run("settings put system screen_brightness_mode 0")
        RootShell.run("am kill-all")
        _enabled.value = true
    }

    suspend fun disable() = mutex.withLock {
        if (!_enabled.value) return@withLock
        if (prevZen == 0) RootShell.run("cmd notification set_dnd off")
        prevBrightnessMode?.let { RootShell.run("settings put system screen_brightness_mode $it") }
        prevZen = null
        prevBrightnessMode = null
        _enabled.value = false
    }

    suspend fun cleanRam() {
        RootShell.run("am kill-all")
    }
}
