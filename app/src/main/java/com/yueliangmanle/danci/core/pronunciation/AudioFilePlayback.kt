package com.yueliangmanle.danci.core.pronunciation

import android.media.MediaPlayer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object AudioPlaybackController {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun play(localPath: String): Boolean = withContext(Dispatchers.Main) {
        val file = File(localPath)
        if (!file.exists()) {
            return@withContext false
        }
        mediaPlayer?.release()
        val player = MediaPlayer()
        mediaPlayer = player
        runCatching {
            player.setDataSource(localPath)
            player.setOnPreparedListener { prepared ->
                prepared.start()
            }
            player.setOnCompletionListener { completed ->
                completed.release()
                if (mediaPlayer === completed) {
                    mediaPlayer = null
                }
            }
            player.setOnErrorListener { failed, _, _ ->
                failed.release()
                if (mediaPlayer === failed) {
                    mediaPlayer = null
                }
                true
            }
            player.prepareAsync()
        }.isSuccess
    }
}

internal suspend fun playAudioFile(localPath: String?): Boolean {
    val path = localPath?.takeIf(String::isNotBlank) ?: return false
    return AudioPlaybackController.play(path)
}
