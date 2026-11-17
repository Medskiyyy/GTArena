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
                line.startsWith("shortLabel=") -> label = line.removePrefix("shortLabel=").split(SHORT_LABEL_END, limit = 2)[0].trim()
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
        val body = line.substringAfter("Intent { ", "").substringBefore(" }")
        if (body.isEmpty()) return null
        val fields = INTENT_FIELD.findAll(body).associate { it.groupValues[1] to it.groupValues[2] }
        val cmp = fields["cmp"] ?: return null
        if ('\'' in cmp) return null
        val sb = StringBuilder("am start")
        fields["act"]?.takeIf { '\'' !in it }?.let { sb.append(" -a '").append(it).append('\'') }
        fields["dat"]?.takeIf { '\'' !in it }?.let { sb.append(" -d '").append(it).append('\'') }
        fields["flg"]?.takeIf { '\'' !in it }?.let { sb.append(" -f ").append(it) }
        sb.append(" -n '").append(cmp).append('\'')
        val bundle = line.substringAfter("PersistableBundle[{", "").substringBefore("}]", "")
        if (bundle.isNotEmpty()) {
            bundle.split(BUNDLE_PAIR_SEP).forEach { pair ->
                val eq = pair.indexOf('=')
                if (eq <= 0) return@forEach
                val key = pair.substring(0, eq)
                val value = pair.substring(eq + 1)
                if ('\'' in key || '\'' in value) return@forEach
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
    private val SHORT_LABEL_END = Regex(", (?:resPackage|resId)=")
    private val INTENT_FIELD = Regex("""(?:^|\s)(act|dat|flg|cmp)=([^\s}]+)""")
    private val BUNDLE_PAIR_SEP = Regex(""", (?=[A-Za-z0-9_.\-]+=)""")
}
