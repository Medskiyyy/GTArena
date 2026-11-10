package com.gtarena.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object RootShell {
    private const val TIMEOUT_MS = 8000L

    private val mutex = Mutex()
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private var reader: BufferedReader? = null
    private var rootConfirmed = false

    suspend fun isAvailable(): Boolean {
        if (rootConfirmed) return true
        val out = run("id -u") ?: return false
        val ok = out.trim() == "0"
        if (ok) rootConfirmed = true
        return ok
    }

    suspend fun run(cmd: String): String? = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                execute(cmd)
            } catch (e: Exception) {
                close()
                null
            }
        }
    }

    private suspend fun execute(cmd: String): String? {
        val proc = ensureProcess() ?: return null
        val w = writer ?: return null
        val r = reader ?: return null
        val marker = "__GTA_END_" + UUID.randomUUID().toString().replace("-", "") + "__"
        return coroutineScope {
            val watchdog = launch {
                delay(TIMEOUT_MS)
                proc.destroy()
            }
            try {
                w.write("{ $cmd\n} 2>/dev/null\necho \"$marker\$?\"\n")
                w.flush()
                val sb = StringBuilder()
                var exit: Int? = null
                while (true) {
                    val line = r.readLine() ?: break
                    val idx = line.indexOf(marker)
                    if (idx >= 0) {
                        if (idx > 0) sb.append(line, 0, idx).append('\n')
                        exit = line.substring(idx + marker.length).trim().toIntOrNull()
                        break
                    }
                    sb.append(line).append('\n')
                }
                if (exit == null) {
                    close()
                    null
                } else if (exit != 0) {
                    null
                } else {
                    sb.toString().trim()
                }
            } finally {
                watchdog.cancel()
            }
        }
    }

    private fun ensureProcess(): Process? {
        val p = process
        if (p != null && p.isAlive) return p
        close()
        return try {
            val np = ProcessBuilder("su").redirectErrorStream(true).start()
            process = np
            writer = OutputStreamWriter(np.outputStream)
            reader = BufferedReader(InputStreamReader(np.inputStream))
            np
        } catch (e: Exception) {
            null
        }
    }

    private fun close() {
        try { writer?.close() } catch (_: Exception) {}
        try { process?.destroy() } catch (_: Exception) {}
        process = null
        writer = null
        reader = null
    }
}
