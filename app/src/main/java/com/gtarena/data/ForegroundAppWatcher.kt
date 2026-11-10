package com.gtarena.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object ForegroundAppWatcher {
    private const val CMD =
        "dumpsys activity activities | grep -m1 -E \"topResumedActivity|ResumedActivity\""
    private val RECORD = Regex("""ActivityRecord\{[0-9a-fA-F]+ u\d+ ([A-Za-z0-9_.]+)/""")

    fun foregroundPackage(periodMs: Long = 2000): Flow<String> = flow {
        var last: String? = null
        while (true) {
            val pkg = RootShell.run(CMD)?.let { parsePackage(it) }
            if (pkg != null && pkg != last) {
                last = pkg
                emit(pkg)
            }
            delay(periodMs)
        }
    }

    internal fun parsePackage(out: String): String? = RECORD.find(out)?.groupValues?.get(1)
}
