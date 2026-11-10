package com.gtarena.data

import kotlin.math.roundToInt

object FpsReader {
    private var layerPkg: String? = null
    private var layer: String? = null
    private var lastFrameNs = 0L

    suspend fun read(pkg: String): Int? {
        if (layer == null || layerPkg != pkg) {
            layer = findLayer(pkg) ?: return null
            layerPkg = pkg
            lastFrameNs = 0L
        }
        val raw = RootShell.run("dumpsys SurfaceFlinger --latency '$layer'")
        val frames = raw?.let(::frameTimes).orEmpty()
        if (frames.size < 2) {
            layer = null
            return null
        }
        val newest = frames.last()
        if (newest == lastFrameNs) return 0
        lastFrameNs = newest
        return fps(frames)
    }

    private suspend fun findLayer(pkg: String): String? {
        val list = RootShell.run("dumpsys SurfaceFlinger --list | grep -F '$pkg/'") ?: return null
        val names = list.lines().mapNotNull { line ->
            val start = line.indexOf('{')
            val end = line.indexOf(" parentId=")
            if (start < 0 || end <= start) null else line.substring(start + 1, end)
        }.filter { '\'' !in it }
        return names.firstOrNull { "SurfaceView[" in it && "(BLAST)" in it }
            ?: names.firstOrNull { Regex("^[0-9a-f]+ ${Regex.escape(pkg)}/").containsMatchIn(it) }
    }

    internal fun frameTimes(raw: String): List<Long> =
        raw.lines().mapNotNull { line ->
            val cols = line.trim().split(Regex("\\s+"))
            if (cols.size != 3) return@mapNotNull null
            val t = cols[0].toLongOrNull() ?: return@mapNotNull null
            if (t <= 0L || t == Long.MAX_VALUE) null else t
        }.sorted()

    internal fun fps(frames: List<Long>): Int {
        val newest = frames.last()
        val window = frames.filter { newest - it <= 1_000_000_000L }
        if (window.size < 2) return 0
        val span = newest - window.first()
        if (span <= 0L) return 0
        return ((window.size - 1) * 1_000_000_000.0 / span).roundToInt()
    }
}
