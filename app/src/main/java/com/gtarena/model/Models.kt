package com.gtarena.model

enum class Profile { HEMAT, SEIMBANG, GAMING }

data class DeviceStats(
    val cpuTempC: Int,
    val batteryTempC: Int,
    val batteryPct: Int,
    val refreshHz: Int,
    val ramUsedGb: Float,
    val bigCoreGhz: Float,
    val gpuLoadPct: Int,
    val charging: Boolean
)

data class GameEntry(
    val pkg: String,
    val label: String,
    val profile: Profile,
    val refreshHz: Int,
    val focus: Boolean,
    val bypassCharge: Boolean
)

data class HomeState(
    val rootOk: Boolean,
    val profile: Profile,
    val stats: DeviceStats,
    val games: List<GameEntry>
)

val Profile.label: String
    get() = when (this) {
        Profile.HEMAT -> "Hemat"
        Profile.SEIMBANG -> "Seimbang"
        Profile.GAMING -> "Gaming"
    }

val Profile.description: String
    get() = when (this) {
        Profile.HEMAT -> "Hemat baterai dan paling adem. Pengaman suhu tetap nyala."
        Profile.SEIMBANG -> "Adem dan irit buat pemakaian harian. Pengaman suhu selalu nyala."
        Profile.GAMING -> "Performa maksimal buat main game. Pengaman suhu tetap jalan."
    }

val sampleHomeState = HomeState(
    rootOk = true,
    profile = Profile.SEIMBANG,
    stats = DeviceStats(
        cpuTempC = 42,
        batteryTempC = 35,
        batteryPct = 12,
        refreshHz = 60,
        ramUsedGb = 5.0f,
        bigCoreGhz = 1.74f,
        gpuLoadPct = 1,
        charging = false
    ),
    games = listOf(
        GameEntry("com.mobile.legends", "Mobile Legends", Profile.GAMING, 120, focus = true, bypassCharge = false),
        GameEntry("com.tencent.ig", "PUBG Mobile", Profile.GAMING, 90, focus = false, bypassCharge = true),
        GameEntry("com.zeekerss.lethalcompany", "Lethal Company", Profile.SEIMBANG, 60, focus = false, bypassCharge = false)
    )
)
