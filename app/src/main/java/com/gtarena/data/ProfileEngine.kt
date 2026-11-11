package com.gtarena.data

import com.gtarena.model.Profile

object ProfileEngine {
    private const val C = "/sys/devices/system/cpu/cpufreq"
    private const val G = "/sys/kernel/ged/hal"
    private const val DIR = "/data/adb/gtarena"

    private const val MIN_FLOORS =
        "echo 500000 > $C/policy0/scaling_min_freq; " +
            "echo 437000 > $C/policy4/scaling_min_freq; " +
            "echo 659000 > $C/policy7/scaling_min_freq; "

    private const val EFFICIENT =
        "echo 1450000 > $C/policy0/schedhorizon/efficient_freq; " +
            "echo 1740000 > $C/policy4/schedhorizon/efficient_freq; " +
            "echo 1998000 > $C/policy7/schedhorizon/efficient_freq; "

    private fun maxFreq(p0: Int, p4: Int, p7: Int) =
        "echo $p0 > $C/policy0/scaling_max_freq; " +
            "echo $p4 > $C/policy4/scaling_max_freq; " +
            "echo $p7 > $C/policy7/scaling_max_freq; "

    private fun upDelay(p0: Int, p4: Int, p7: Int) =
        "echo $p0 > $C/policy0/schedhorizon/up_delay; " +
            "echo $p4 > $C/policy4/schedhorizon/up_delay; " +
            "echo $p7 > $C/policy7/schedhorizon/up_delay; "

    private fun gpu(upbound: Int) =
        "echo $upbound > $G/custom_upbound_gpu_freq; echo 40 > $G/custom_boost_gpu_freq; "

    private fun refresh(peak: Int, min: Int) =
        "settings put system peak_refresh_rate $peak.0; settings put system min_refresh_rate $min.0; "

    private fun touch(rate: Int) =
        "echo $rate > /proc/tp_report_rate_switch; echo 0 > /proc/tp_transient_report_rate_switch; "

    private fun bypass(on: Boolean) =
        "echo '0 ${if (on) 1 else 0}' > /proc/mtk_battery_cmd/current_cmd; "

    private const val TM = "/vendor/bin/thermal_manager /vendor/etc/.tp"

    private const val CTM_UNLOCKED = "2 95000 95000 53000 56000 85000 85000 95000 0 85000 0 500 500 13500 -1"

    private fun thermal(level: Int) = when (level) {
        1, 2 ->
            "[ \"$(cat $DIR/thermal 2>/dev/null)\" = $level ] || { $TM/.thermal_policy_03; " +
                (if (level == 2) "echo '$CTM_UNLOCKED' > /proc/driver/thermal/clctm; " else "") +
                "echo $level > $DIR/thermal; }; "
        else -> "if [ -f $DIR/thermal ]; then $TM/thermal.conf; rm -f $DIR/thermal; fi; "
    }

    suspend fun batteryTempC(): Int? =
        RootShell.run("cat /sys/class/power_supply/battery/temp")?.trim()?.toIntOrNull()?.div(10)

    private fun recipe(profile: Profile, refreshHz: Int?, bypassCharge: Boolean, thermalLevel: Int = 0): String {
        val hz = refreshHz?.takeIf { it == 60 || it == 90 || it == 120 }
        return bypass(bypassCharge) + thermal(thermalLevel) + when (profile) {
            Profile.HEMAT ->
                MIN_FLOORS + maxFreq(1450000, 1451000, 1482000) + EFFICIENT + upDelay(20, 30, 40) +
                    gpu(20) + refresh(hz ?: 60, hz ?: 60) + touch(0)
            Profile.SEIMBANG ->
                MIN_FLOORS + maxFreq(2000000, 2600000, 3000000) + EFFICIENT + upDelay(20, 30, 40) +
                    gpu(0) + refresh(hz ?: 120, hz ?: 60) + touch(0)
            Profile.GAMING ->
                MIN_FLOORS + maxFreq(2000000, 2600000, 3000000) + EFFICIENT + upDelay(0, 0, 0) +
                    gpu(0) + refresh(hz ?: 120, hz ?: 120) + touch(1)
        }
    }

    private const val BEAT = "mkdir -p $DIR; date +%s > $DIR/heartbeat"

    suspend fun apply(
        profile: Profile,
        refreshHz: Int? = null,
        bypassCharge: Boolean = false,
        thermalLevel: Int = 0
    ): Boolean =
        RootShell.run(BEAT + "; " + recipe(profile, refreshHz, bypassCharge, thermalLevel) + "echo ${profile.name} > $DIR/profile") != null

    suspend fun heartbeat() {
        RootShell.run(BEAT)
    }

    suspend fun writeGames(pkgs: List<String>) {
        val body = if (pkgs.isEmpty()) ": > $DIR/games" else "printf '%s\\n' ${pkgs.joinToString(" ")} > $DIR/games"
        RootShell.run("mkdir -p $DIR; $body")
    }

    suspend fun release() {
        RootShell.run(recipe(Profile.SEIMBANG, null, false) + "rm -f $DIR/heartbeat $DIR/profile")
    }
}
