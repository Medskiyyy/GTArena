package com.gtarena.data

import android.content.Context
import android.content.SharedPreferences
import com.gtarena.model.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GameConfig(
    val profile: Profile = Profile.GAMING,
    val refreshHz: Int = 120,
    val focus: Boolean = false,
    val bypassCharge: Boolean = false
)

class SettingsStore private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("gtarena", Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(
        runCatching { Profile.valueOf(prefs.getString(KEY_PROFILE, null) ?: "") }.getOrDefault(Profile.SEIMBANG)
    )
    val profile: StateFlow<Profile> = _profile

    private val _autoApply = MutableStateFlow(prefs.getBoolean(KEY_AUTO, true))
    val autoApply: StateFlow<Boolean> = _autoApply

    private val _overlayEnabled = MutableStateFlow(prefs.getBoolean(KEY_OVERLAY, true))
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled

    private val _fpsBubble = MutableStateFlow(prefs.getBoolean(KEY_BUBBLE, true))
    val fpsBubble: StateFlow<Boolean> = _fpsBubble

    private val _extraGames = MutableStateFlow(prefs.getStringSet(KEY_EXTRA, emptySet())!!.toSet())
    val extraGames: StateFlow<Set<String>> = _extraGames

    private val _gameConfigs = MutableStateFlow(loadGameConfigs())
    val gameConfigs: StateFlow<Map<String, GameConfig>> = _gameConfigs

    fun setProfile(p: Profile) {
        _profile.value = p
        prefs.edit().putString(KEY_PROFILE, p.name).apply()
    }

    fun setAutoApply(on: Boolean) {
        _autoApply.value = on
        prefs.edit().putBoolean(KEY_AUTO, on).apply()
    }

    fun setOverlayEnabled(on: Boolean) {
        _overlayEnabled.value = on
        prefs.edit().putBoolean(KEY_OVERLAY, on).apply()
    }

    fun setFpsBubble(on: Boolean) {
        _fpsBubble.value = on
        prefs.edit().putBoolean(KEY_BUBBLE, on).apply()
    }

    fun bubblePos(landscape: Boolean): Pair<Int, Int>? {
        val raw = prefs.getString(bubbleKey(landscape), null)?.split(",") ?: return null
        val x = raw.getOrNull(0)?.toIntOrNull() ?: return null
        val y = raw.getOrNull(1)?.toIntOrNull() ?: return null
        return x to y
    }

    fun setBubblePos(landscape: Boolean, x: Int, y: Int) {
        prefs.edit().putString(bubbleKey(landscape), "$x,$y").apply()
    }

    private fun bubbleKey(landscape: Boolean) = if (landscape) "bubble_pos_land" else "bubble_pos_port"

    fun setExtraGame(pkg: String, included: Boolean) {
        val next = if (included) _extraGames.value + pkg else _extraGames.value - pkg
        _extraGames.value = next
        prefs.edit().putStringSet(KEY_EXTRA, next).apply()
    }

    fun gameConfig(pkg: String): GameConfig = _gameConfigs.value[pkg] ?: GameConfig()

    fun setGameConfig(pkg: String, cfg: GameConfig) {
        _gameConfigs.value = _gameConfigs.value + (pkg to cfg)
        prefs.edit()
            .putString(GAME_PREFIX + pkg, "${cfg.profile.name}|${cfg.refreshHz}|${cfg.focus}|${cfg.bypassCharge}")
            .apply()
    }

    private fun loadGameConfigs(): Map<String, GameConfig> =
        prefs.all.filterKeys { it.startsWith(GAME_PREFIX) }.mapNotNull { (key, value) ->
            val parts = (value as? String)?.split("|") ?: return@mapNotNull null
            if (parts.size != 4) return@mapNotNull null
            val profile = runCatching { Profile.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val hz = parts[1].toIntOrNull() ?: return@mapNotNull null
            key.removePrefix(GAME_PREFIX) to GameConfig(profile, hz, parts[2].toBoolean(), parts[3].toBoolean())
        }.toMap()

    companion object {
        private const val KEY_PROFILE = "profile"
        private const val KEY_AUTO = "auto_apply"
        private const val KEY_OVERLAY = "overlay_enabled"
        private const val KEY_BUBBLE = "fps_bubble"
        private const val KEY_EXTRA = "extra_games"
        private const val GAME_PREFIX = "game:"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
