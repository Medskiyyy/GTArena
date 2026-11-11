package com.gtarena.ui.intro

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.gtarena.R

object IntroSound {
    private var player: MediaPlayer? = null

    fun play(context: Context) {
        stop()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val mp = try {
            MediaPlayer.create(context.applicationContext, R.raw.intro, attrs, 0)
        } catch (e: Exception) {
            null
        } ?: return
        player = mp
        mp.setOnCompletionListener {
            if (player === it) player = null
            it.release()
        }
        mp.start()
    }

    fun stop() {
        player?.release()
        player = null
    }
}
