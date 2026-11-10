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

    private fun recipe(profile: Profile, refreshHz: Int?): String {
        val hz = refreshHz?.takeIf { it == 60 || it == 90 || it == 120 }
        return when (profile) {
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

    suspend fun apply(profile: Profile, refreshHz: Int? = null): Boolean =
        RootShell.run(BEAT + "; " + recipe(profile, refreshHz) + "echo ${profile.name} > $DIR/profile") != null

    suspend fun heartbeat() {
        RootShell.run(BEAT)
    }

    suspend fun release() {
        RootShell.run(recipe(Profile.SEIMBANG, null) + "rm -f $DIR/heartbeat $DIR/profile")
    }
}
