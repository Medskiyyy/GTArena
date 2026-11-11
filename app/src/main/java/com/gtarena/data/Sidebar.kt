package com.gtarena.data

import android.content.Context

data class AppShortcut(val label: String, val command: String)

object Sidebar {
    private const val ROM_PKG = "com.libremobileos.sidebar"
    private const val ROM_DATA = "/data_mirror/data_ce/null/0/$ROM_PKG"
    private const val ROM_DB = "$ROM_DATA/databases/database.db"
    private const val ROM_PREFS = "$ROM_DATA/shared_prefs/config.xml"

    suspend fun importRomApps(): List<String> {
        val out = RootShell.run("sqlite3 -readonly $ROM_DB 'select packageName from SidebarAppsEntity order by sortNum'")
            ?: return emptyList()
        return out.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    suspend fun setRomSideline(on: Boolean) {
        val want = "name=\"sideline\" value=\"$on\""
        RootShell.run(
            "f=$ROM_PREFS; [ -f \$f ] && ! grep -q '$want' \$f && " +
                "sed -i 's/name=\"sideline\" value=\"[a-z]*\"/$want/' \$f && kill \$(pidof $ROM_PKG)"
        )
    }

    suspend fun openFloating(context: Context, pkg: String): Boolean {
        val activity = context.packageManager.getLaunchIntentForPackage(pkg)?.component?.className ?: return false
        return RootShell.run(
            "am broadcast -a com.libremobileos.freeform.START_FREEFORM " +
                "-n com.libremobileos.freeform/.receiver.StartFreeformReceiver " +
                "--es packageName '$pkg' --es activityName '$activity' --ei userId 0"
        ) != null
    }

    suspend fun openFull(context: Context, pkg: String): Boolean {
        val cmp = context.packageManager.getLaunchIntentForPackage(pkg)?.component ?: return false
        return RootShell.run("am start -n '${cmp.packageName}/${cmp.className}'") != null
    }

    suspend fun shortcuts(pkg: String): List<AppShortcut> {
        val out = RootShell.run("cmd shortcut get-shortcuts --flags 11 '$pkg'") ?: return emptyList()
        val result = ArrayList<AppShortcut>()
        var label: String? = null
        for (raw in out.lines()) {
            val line = raw.trim()
            when {
                line.startsWith("ShortcutInfo {") -> label = null
                line.startsWith("shortLabel=") -> label = line.removePrefix("shortLabel=").substringBefore(", resId=")
                line.startsWith("intents=[Intent {") -> {
                    val name = label?.takeIf { it != "null" } ?: continue
                    val command = parseIntent(line) ?: continue
                    if (result.none { it.label == name }) result.add(AppShortcut(name, command))
                }
            }
        }
        return result.take(MAX_SHORTCUTS)
    }

    suspend fun openShortcut(shortcut: AppShortcut): Boolean = RootShell.run(shortcut.command) != null

    private fun parseIntent(line: String): String? {
        val body = line.substringAfter("Intent { ").substringBefore(" }")
        val parts = body.split(" ").associate { it.substringBefore("=") to it.substringAfter("=", "") }
        val cmp = parts["cmp"] ?: return null
        val sb = StringBuilder("am start")
        parts["act"]?.let { sb.append(" -a '").append(it).append('\'') }
        parts["dat"]?.let { sb.append(" -d '").append(it).append('\'') }
        parts["flg"]?.let { sb.append(" -f ").append(it) }
        sb.append(" -n '").append(cmp).append('\'')
        val bundle = line.substringAfter("PersistableBundle[{", "").substringBefore("}]", "")
        if (bundle.isNotEmpty()) {
            bundle.split(", ").forEach { pair ->
                val key = pair.substringBefore("=")
                val value = pair.substringAfter("=", "")
                if (key.isEmpty() || '\'' in key || '\'' in value) return@forEach
                val flag = when {
                    value == "true" || value == "false" -> "--ez"
                    value.toIntOrNull() != null -> "--ei"
                    value.toLongOrNull() != null -> "--el"
                    else -> "--es"
                }
                sb.append(' ').append(flag).append(" '").append(key).append("' '").append(value).append('\'')
            }
        }
        return sb.toString()
    }

    private const val MAX_SHORTCUTS = 4
}
