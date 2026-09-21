package io.github.diegog0477.zombiebox.client.features.playback.presentation.ui

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import io.github.diegog0477.zombiebox.client.features.playback.platform.PlayerSurface
import java.util.concurrent.atomic.AtomicInteger

/** Loaded only through the API14 factory. Texture ownership outlives asynchronous detach. */
@TargetApi(14)
class Api14TextureOutput : VideoOutputView {
    private lateinit var texture: TextureView
    private var output: TextureSurface? = null
    private var changed: ((PlayerSurface?) -> Unit)? = null
    private var failed: (() -> Unit)? = null
    private val updates = AtomicInteger()
    private var videoWidth = 0
    private var videoHeight = 0
    override val frames: Int
        get() = updates.get()

    override val view: View
        get() = texture

    override fun create(
        context: Context,
        changed: (PlayerSurface?) -> Unit,
        failed: () -> Unit,
    ): View {
        this.changed = changed
        this.failed = failed
        texture = TextureView(context)
        texture.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    value: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    if (!texture.isHardwareAccelerated) {
                        this@Api14TextureOutput.failed?.invoke()
                        return
                    }
                    try {
                        output = TextureSurface(value) { this@Api14TextureOutput.failed?.invoke() }
                        this@Api14TextureOutput.changed?.invoke(output)
                        resize()
                    } catch (_: Exception) {
                        this@Api14TextureOutput.failed?.invoke()
                    } catch (_: LinkageError) {
                        this@Api14TextureOutput.failed?.invoke()
                    }
                }

                override fun onSurfaceTextureSizeChanged(
                    value: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    resize()
                }

                override fun onSurfaceTextureUpdated(value: SurfaceTexture) {
                    updates.incrementAndGet()
                }

                override fun onSurfaceTextureDestroyed(value: SurfaceTexture): Boolean {
                    this@Api14TextureOutput.changed?.invoke(null)
                    val old = output
                    output = null
                    old?.release()
                    return old == null
                }
            }
        return texture
    }

    override fun setVideoSize(width: Int, height: Int) {
        videoWidth = width
        videoHeight = height
        resize()
    }

    private fun resize() {
        if (videoWidth <= 0 || videoHeight <= 0 || texture.width <= 0 || texture.height <= 0) return
        val factor =
            minOf(texture.width.toFloat() / videoWidth, texture.height.toFloat() / videoHeight)
        texture.setTransform(
            Matrix().apply {
                setScale(
                    videoWidth * factor / texture.width,
                    videoHeight * factor / texture.height,
                    texture.width / 2f,
                    texture.height / 2f,
                )
            }
        )
    }

    override fun close() {
        changed?.invoke(null)
        changed = null
        failed = null
        // onSurfaceTextureDestroyed releases the View's reference after removal.
    }

    private class TextureSurface(
        private val texture: SurfaceTexture,
        private val failed: () -> Unit,
    ) : PlayerSurface {
        private val surface = Surface(texture)
        private var references = 1

        @Synchronized
        override fun retain() {
            check(references > 0)
            references++
        }

        @Synchronized
        override fun release() {
            check(references > 0)
            references--
            if (references == 0) {
                surface.release()
                texture.release()
            }
        }

        override fun attach(player: MediaPlayer) {
            try {
                player.setSurface(surface)
            } catch (error: Exception) {
                failed()
                throw error
            } catch (_: LinkageError) {
                failed()
                throw IllegalStateException("Texture output unavailable")
            }
        }

        override fun detach(player: MediaPlayer) {
            try {
                player.setSurface(null)
            } catch (error: Exception) {
                failed()
                throw error
            } catch (_: LinkageError) {
                failed()
                throw IllegalStateException("Texture detach unavailable")
            }
        }
    }
}
