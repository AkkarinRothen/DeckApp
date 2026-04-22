package com.deckapp.feature.npcs

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File

class AudioPlayer(private val context: Context) {

    private var player: MediaPlayer? = null

    fun playFile(file: File, onFinished: () -> Unit = {}) {
        playUri(Uri.fromFile(file), onFinished)
    }

    fun playPath(path: String, onFinished: () -> Unit = {}) {
        val file = File(path)
        if (file.exists()) {
            playFile(file, onFinished)
        }
    }

    fun playUri(uri: Uri, onFinished: () -> Unit = {}) {
        stop()
        MediaPlayer.create(context, uri)?.apply {
            player = this
            setOnCompletionListener { 
                onFinished()
                release()
                player = null
            }
            start()
        } ?: onFinished()
    }

    fun stop() {
        player?.stop()
        player?.release()
        player = null
    }

    val isPlaying: Boolean
        get() = player?.isPlaying ?: false
}
