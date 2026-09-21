package io.github.diegog0477.zombiebox.client.platform

import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.SurfaceHolder
import io.github.diegog0477.zombiebox.client.model.*

/** API9 calls only. Completion and advancement are evidence, prepare alone is not. */
class MediaProbePlayback : ProbePlayback {
    private val thread = HandlerThread("zombie-probes").apply { start() }
    private val handler = Handler(thread.looper)
    @Volatile var surface: SurfaceHolder? = null
    private var player: MediaPlayer? = null
    private var generation = 0
    private var closed = false

    override fun start(asset: ProbeAsset, result: (ProbeResult) -> Unit) {
        handler.post {
            if (closed) return@post
            dispose()
            val run = ++generation
            val started = SystemClock.elapsedRealtime()
            var prepareMs = 0
            var firstFrameMs = 0
            var positionMs = 0
            val media = MediaPlayer()
            player = media
            var finished = false
            fun finish(status: String, completed: Boolean = false, stalled: Boolean = false) {
                if (finished || run != generation) return
                finished = true
                dispose()
                result(
                    ProbeResult(
                        asset.id,
                        status,
                        prepareMs,
                        firstFrameMs,
                        positionMs,
                        completed,
                        stalled,
                    )
                )
            }
            val tick =
                object : Runnable {
                    override fun run() {
                        if (finished || run != generation) return
                        try {
                            positionMs = maxOf(positionMs, media.currentPosition)
                        } catch (_: Exception) {}
                        handler.postDelayed(this, 100)
                    }
                }
            try {
                media.setVolume(0f, 0f)
                if (asset.video) {
                    if (surface == null) {
                        finish("UNKNOWN")
                        return@post
                    }
                    media.setDisplay(surface)
                }
                media.setOnPreparedListener {
                    if (finished || run != generation) return@setOnPreparedListener
                    prepareMs = (SystemClock.elapsedRealtime() - started).toInt()
                    try {
                        media.start()
                        handler.post(tick)
                    } catch (_: Exception) {
                        finish("FAIL")
                    }
                }
                media.setOnInfoListener { _, what, _ ->
                    // Numeric rendering-start event is optional on old vendor players.
                    if (what == 3 && firstFrameMs == 0)
                        firstFrameMs = (SystemClock.elapsedRealtime() - started).toInt()
                    false
                }
                media.setOnCompletionListener {
                    finish(if (positionMs >= 500) "PASS" else "UNKNOWN", completed = true)
                }
                media.setOnErrorListener { _, what, extra ->
                    finish(if (extra == -1010 || extra == -1007) "FAIL" else "UNKNOWN")
                    true
                }
                media.setDataSource(asset.url)
                media.prepareAsync()
                handler.postDelayed(
                    { finish(if (prepareMs > 0) "FAIL" else "UNKNOWN", stalled = prepareMs > 0) },
                    12000,
                )
            } catch (_: Exception) {
                finish("UNKNOWN")
            }
        }
    }

    private fun dispose() {
        try {
            player?.release()
        } catch (_: Exception) {}
        player = null
    }

    override fun cancel() {
        handler.post {
            generation++
            dispose()
        }
    }

    fun close() {
        handler.post {
            closed = true
            generation++
            dispose()
            thread.quit()
        }
    }
}
