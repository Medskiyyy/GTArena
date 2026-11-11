package com.gtarena.data

import android.content.Context
import android.media.MediaScannerConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object GameTools {
    private const val MOVIES = "/sdcard/Movies"
    private const val SCREENSHOT_SETTLE_MS = 800L
    const val RECORD_LIMIT_S = 180

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording
    private var recordPath: String? = null

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    suspend fun screenshot(): Boolean {
        val ok = RootShell.run("input keyevent KEYCODE_SYSRQ") != null
        delay(SCREENSHOT_SETTLE_MS)
        return ok
    }

    suspend fun startRecording(): Boolean {
        val path = "$MOVIES/GTArena_${stamp()}.mp4"
        val ok = RootShell.run(
            "mkdir -p $MOVIES && { nohup screenrecord --bit-rate 12000000 --time-limit $RECORD_LIMIT_S $path >/dev/null 2>&1 & }"
        ) != null
        if (ok) {
            recordPath = path
            _recording.value = true
        }
        return ok
    }

    suspend fun stopRecording(context: Context) {
        RootShell.run("pkill -INT screenrecord; sleep 1")
        finish(context)
    }

    suspend fun syncRecording(context: Context) {
        if (_recording.value && RootShell.run("pidof screenrecord") == null) finish(context)
    }

    private fun finish(context: Context) {
        _recording.value = false
        recordPath?.let { MediaScannerConnection.scanFile(context, arrayOf(it), null, null) }
        recordPath = null
    }

    suspend fun brightness(): Float? =
        RootShell.run("cmd display get-brightness --unit percentage")?.trim()?.toFloatOrNull()

    suspend fun setBrightness(percent: Float) {
        RootShell.run("cmd display set-brightness ${percent.coerceIn(1f, 100f)} --unit percentage")
    }
}
