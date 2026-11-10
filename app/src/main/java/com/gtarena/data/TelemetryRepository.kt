package com.gtarena.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.view.Display
import com.gtarena.model.DeviceStats
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TelemetryRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private var cpuZonePath: String? = null

    fun stats(periodMs: Long = 1000L): Flow<DeviceStats> = flow {
        var last = DeviceStats(0, 0, 0, 0, 0f, 0f, 0, false)
        while (true) {
            last = read(last)
            emit(last)
            delay(periodMs)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun read(prev: DeviceStats): DeviceStats {
        val battery = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        var batteryPct = prev.batteryPct
        var batteryTemp = prev.batteryTempC
        var charging = prev.charging
        if (battery != null) {
            val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) batteryPct = level * 100 / scale
            val t = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (t != Int.MIN_VALUE) batteryTemp = (t / 10f).roundToInt()
            val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }

        val refresh = try {
            val dm = appContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            dm.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate?.roundToInt() ?: prev.refreshHz
        } catch (_: Exception) {
            prev.refreshHz
        }

        val ramUsed = try {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            (mi.totalMem - mi.availMem) / 1073741824f
        } catch (_: Exception) {
            prev.ramUsedGb
        }

        val cpuTemp = readCpuTemp() ?: prev.cpuTempC

        val bigGhz = readLong("/sys/devices/system/cpu/cpufreq/policy7/scaling_cur_freq")
            ?.let { it / 1_000_000f } ?: prev.bigCoreGhz

        val gpuLoad = readGpuLoad() ?: prev.gpuLoadPct

        return DeviceStats(
            cpuTempC = cpuTemp,
            batteryTempC = batteryTemp,
            batteryPct = batteryPct,
            refreshHz = refresh,
            ramUsedGb = ramUsed,
            bigCoreGhz = bigGhz,
            gpuLoadPct = gpuLoad,
            charging = charging
        )
    }

    private fun readLong(path: String): Long? = try {
        File(path).readText().trim().toLongOrNull()
    } catch (_: Exception) {
        null
    }

    private fun findCpuZone(): String? {
        val zones = File("/sys/class/thermal").listFiles { f -> f.name.startsWith("thermal_zone") }
            ?: return null
        for (z in zones) {
            try {
                if (File(z, "type").readText().trim() == "mtktscpu") return File(z, "temp").path
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun readCpuTemp(): Int? {
        var path = cpuZonePath
        if (path == null) {
            path = findCpuZone() ?: return null
            cpuZonePath = path
        }
        val milli = readLong(path)
        if (milli == null) {
            cpuZonePath = null
            return null
        }
        return (milli / 1000f).roundToInt()
    }

    private suspend fun readGpuLoad(): Int? {
        val out = RootShell.run("cat /sys/kernel/ged/hal/gpu_utilization") ?: return null
        val first = out.trim().split(Regex("\\s+")).firstOrNull()?.toIntOrNull() ?: return null
        return first.coerceIn(0, 100)
    }
}
