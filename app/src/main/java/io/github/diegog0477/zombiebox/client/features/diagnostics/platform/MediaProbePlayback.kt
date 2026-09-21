package io.github.diegog0477.zombiebox.client.features.diagnostics.platform

import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.SurfaceHolder
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeAsset
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeResult
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.ProbePlayback
import io.github.diegog0477.zombiebox.client.features.playback.platform.PlayerSurface

/** API9 calls only. Completion and advancement are evidence, prepare alone is not. */
class MediaProbePlayback : ProbePlayback {
    private val thread = HandlerThread("zombie-probes").apply { start() }
    private val handler = Handler(thread.looper)
    @Volatile var surface: SurfaceHolder? = null
    @Volatile var textureSurface: PlayerSurface? = null
    @Volatile var textureFrames: () -> Int = { 0 }
    private var boundOutput: PlayerSurface? = null
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
            var operationStarted = false
            val initialFrames = textureFrames()
            var operationComplete = asset.kind in listOf("playback", "hls", "texture-output")
            var operationPosition = 0
            var advancedAfterOperation = false
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
                            val current = media.currentPosition
                            positionMs = maxOf(positionMs, current)
                            if (operationComplete && current >= operationPosition + 500) {
                                advancedAfterOperation = true
                            }
                            if (
                                !operationStarted &&
                                    current >= (if (asset.kind == "seek") 800 else 300) &&
                                    !operationComplete
                            ) {
                                operationStarted = true
                                when (asset.kind) {
                                    "seek" -> media.seekTo(0)
                                    "pause-resume" -> {
                                        media.pause()
                                        val pausedAt = media.currentPosition
                                        handler.postDelayed(
                                            {
                                                if (finished || run != generation)
                                                    return@postDelayed
                                                try {
                                                    if (
                                                        media.isPlaying ||
                                                            kotlin.math.abs(
                                                                media.currentPosition - pausedAt
                                                            ) > 200
                                                    ) {
                                                        finish("FAIL")
                                                    } else {
                                                        operationPosition = media.currentPosition
                                                        operationComplete = true
                                                        media.start()
                                                    }
                                                } catch (_: Exception) {
                                                    finish("UNKNOWN")
                                                }
                                            },
                                            300,
                                        )
                                    }
                                    "surface-reattach" -> {
                                        media.setDisplay(null)
                                        handler.postDelayed(
                                            {
                                                if (finished || run != generation)
                                                    return@postDelayed
                                                try {
                                                    val holder = surface
                                                    if (holder == null || !holder.surface.isValid) {
                                                        finish("UNKNOWN")
                                                    } else {
                                                        media.setDisplay(holder)
                                                        operationPosition = media.currentPosition
                                                        operationComplete = true
                                                    }
                                                } catch (_: Exception) {
                                                    finish("UNKNOWN")
                                                }
                                            },
                                            200,
                                        )
                                    }
                                    else -> finish("UNKNOWN")
                                }
                            }
                        } catch (_: Exception) {
                            finish("UNKNOWN")
                        }
                        handler.postDelayed(this, 100)
                    }
                }
            try {
                media.setVolume(0f, 0f)
                if (asset.video) {
                    if (asset.kind == "texture-output") {
                        val output = textureSurface
                        if (output == null) {
                            finish("UNKNOWN")
                            return@post
                        }
                        output.retain()
                        boundOutput = output
                        output.attach(media)
                    } else {
                        if (surface == null) {
                            finish("UNKNOWN")
                            return@post
                        }
                        media.setDisplay(surface)
                    }
                }
                media.setOnPreparedListener {
                    if (finished || run != generation) return@setOnPreparedListener
                    prepareMs = (SystemClock.elapsedRealtime() - started).toInt()
                    try {
                        media.start()
                        handler.post(tick)
                    } catch (_: Exception) {
                        finish("UNKNOWN")
                    }
                }
                media.setOnInfoListener { _, what, _ ->
                    // Numeric rendering-start event is optional on old vendor players.
                    if (what == 3 && firstFrameMs == 0)
                        firstFrameMs = (SystemClock.elapsedRealtime() - started).toInt()
                    false
                }
                media.setOnSeekCompleteListener {
                    if (
                        !finished && run == generation && asset.kind == "seek" && operationStarted
                    ) {
                        try {
                            val current = media.currentPosition
                            // Completion alone is insufficient: validate the target and
                            // advancement.
                            if (current in 0..350) {
                                operationPosition = current
                                operationComplete = true
                            } else finish("FAIL")
                        } catch (_: Exception) {
                            finish("UNKNOWN")
                        }
                    }
                }
                media.setOnCompletionListener {
                    val passed =
                        operationComplete &&
                            advancedAfterOperation &&
                            (asset.kind != "texture-output" || textureFrames() - initialFrames >= 3)
                    finish(if (passed) "PASS" else "UNKNOWN", completed = true)
                }
                media.setOnErrorListener { _, what, extra ->
                    finish(if (extra == -1010 || extra == -1007) "FAIL" else "UNKNOWN")
                    true
                }
                media.setDataSource(asset.url)
                media.prepareAsync()
                handler.postDelayed({ finish("UNKNOWN", stalled = prepareMs > 0) }, 12000)
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
        boundOutput?.release()
        boundOutput = null
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
