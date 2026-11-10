package com.gtarena.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.gtarena.model.GameEntry
import com.gtarena.model.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(private val context: Context) {

    suspend fun installedGames(): List<GameEntry> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcher, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != context.packageName && isUserOrUpdated(it) && isGame(it) }
            .map {
                GameEntry(
                    pkg = it.packageName,
                    label = it.loadLabel(pm).toString(),
                    profile = Profile.GAMING,
                    refreshHz = 120,
                    focus = false,
                    bypassCharge = false
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launch(pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isUserOrUpdated(info: ApplicationInfo): Boolean {
        val system = info.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val updated = info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        return !system || updated
    }

    @Suppress("DEPRECATION")
    private fun isGame(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_GAME ||
            info.flags and ApplicationInfo.FLAG_IS_GAME != 0 ||
            info.packageName in ALLOW_LIST

    private companion object {
        val ALLOW_LIST = setOf("com.zeekerss.lethalcompany")
    }
}
