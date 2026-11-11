package com.gtarena.data

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job

object LaunchWatcher {
    private const val CMD =
        "logcat -b system,events -v brief -T 1 -s ActivityTaskManager:I input_focus:I"
    private val START = Regex("""START u\d+ \{.*?cmp=([A-Za-z0-9_.]+)/""")
    private val FOCUS = Regex("""Focus entering \S+ ([A-Za-z0-9_.]+)/""")

    fun launches(): Flow<String> = flow {
        while (currentCoroutineContext().isActive) {
            val proc = try {
                ProcessBuilder("su", "-c", CMD).redirectErrorStream(true).start()
            } catch (e: Exception) {
                null
            }
            if (proc != null) {
                val handle = currentCoroutineContext().job.invokeOnCompletion { proc.destroy() }
                try {
                    BufferedReader(InputStreamReader(proc.inputStream)).useLines { lines ->
                        for (line in lines) parse(line)?.let { emit(it) }
                    }
                } catch (e: java.io.IOException) {
                } finally {
                    handle.dispose()
                    proc.destroy()
                }
            }
            delay(3000L)
        }
    }.flowOn(Dispatchers.IO)

    internal fun parse(line: String): String? =
        (START.find(line) ?: FOCUS.find(line))?.groupValues?.get(1)
}
