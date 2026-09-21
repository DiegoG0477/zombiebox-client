package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.SurfaceHolder

/** All MediaPlayer calls share a looper; navigation never creates a second player. */
class EmbeddedPlayer(
    private val sizeChanged: (Int, Int) -> Unit,
    private val changed: (String, Int, Int) -> Unit,
) {
    private val thread = HandlerThread("zombie-player").apply { start() }
    private val handler = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var holder: SurfaceHolder? = null
    private var prepared = false
    private var resume = 0
    private var wantPlay = false
    private var volumeGain = 1f
    private var prepareTimeout: Runnable? = null
    private var state = "STOPPED"
    @Volatile private var closed = false
    @Volatile private var epoch = 0
    private val tick =
        object : Runnable {
            override fun run() {
                report()
                if (!closed && prepared) handler.postDelayed(this, 250)
            }
        }

    private fun report() {
        var position = 0
        var duration = 0
        if (prepared)
            try {
                position = player?.currentPosition ?: 0
                duration = (player?.duration ?: 0).coerceAtLeast(0)
            } catch (_: IllegalStateException) {}
        val current = state
        val reportEpoch = epoch
        main.post { if (!closed && reportEpoch == epoch) changed(current, position, duration) }
    }

    fun surface(value: SurfaceHolder?) {
        handler.post {
            holder = value
            player?.setDisplay(value)
        }
    }

    fun play(url: String, position: Int) {
        handler.post {
            dispose()
            resume = position
            wantPlay = true
            state = "BUFFERING"
            report()
            val media = MediaPlayer()
            player = media
            media.setAudioStreamType(AudioManager.STREAM_MUSIC)
            media.setVolume(volumeGain, volumeGain)
            media.setDisplay(holder)
            media.setOnVideoSizeChangedListener { _, width, height ->
                main.post { if (!closed) sizeChanged(width, height) }
            }
            media.setOnPreparedListener {
                if (player === it) {
                    prepared = true
                    if (resume > 0 && it.duration > 0) it.seekTo(resume.coerceAtMost(it.duration))
                    if (wantPlay) {
                        it.start()
                        state = "PLAYING"
                    } else state = "PAUSED"
                    tick.run()
                }
            }
            media.setOnCompletionListener {
                state = "ENDED"
                report()
                handler.removeCallbacks(tick)
            }
            media.setOnErrorListener { _, _, _ ->
                dispose()
                state = "FAILED"
                report()
                true
            }
            try {
                media.setDataSource(url)
                media.prepareAsync()
                prepareTimeout = Runnable {
                    if (player === media && !prepared) {
                        dispose()
                        state = "FAILED"
                        report()
                    }
                }
                handler.postDelayed(prepareTimeout!!, 20000)
            } catch (_: Exception) {
                dispose()
                state = "FAILED"
                report()
            }
        }
    }

    fun toggle() {
        handler.post {
            if (prepared)
                try {
                    val media = player ?: return@post
                    if (media.isPlaying) {
                        media.pause()
                        wantPlay = false
                        state = "PAUSED"
                    } else {
                        media.start()
                        wantPlay = true
                        state = "PLAYING"
                    }
                    report()
                } catch (_: IllegalStateException) {
                    state = "FAILED"
                    report()
                }
        }
    }

    fun resume() {
        handler.post {
            wantPlay = true
            if (prepared && state == "PAUSED")
                try {
                    player?.start()
                    state = "PLAYING"
                    report()
                } catch (_: IllegalStateException) {
                    state = "FAILED"
                    report()
                }
        }
    }

    fun pause() {
        handler.post {
            wantPlay = false
            if (prepared && state == "PLAYING") {
                player?.pause()
                state = "PAUSED"
                report()
            }
        }
    }

    fun seek(delta: Int) {
        handler.post {
            if (prepared)
                try {
                    val media = player ?: return@post
                    if (media.duration > 0)
                        media.seekTo((media.currentPosition + delta).coerceIn(0, media.duration))
                } catch (_: IllegalStateException) {}
        }
    }

    fun seekTo(position: Int) {
        handler.post {
            if (prepared)
                try {
                    player?.seekTo(position.coerceAtLeast(0))
                } catch (_: Exception) {}
        }
    }

    fun volume(level: Int, muted: Boolean, done: (Boolean) -> Unit) {
        handler.post {
            val success =
                try {
                    val media = player
                    if (media == null) false
                    else {
                        volumeGain = if (muted) 0f else level.coerceIn(0, 100) / 100f
                        media.setVolume(volumeGain, volumeGain)
                        true
                    }
                } catch (_: Exception) {
                    false
                }
            main.post { if (!closed) done(success) }
        }
    }

    fun stop() {
        handler.post {
            dispose()
            state = "STOPPED"
            report()
        }
    }

    private fun dispose() {
        epoch++
        handler.removeCallbacks(tick)
        prepareTimeout?.let { handler.removeCallbacks(it) }
        prepareTimeout = null
        prepared = false
        player?.release()
        player = null
    }

    fun close() {
        closed = true
        handler.post {
            dispose()
            thread.quit()
        }
    }
}
