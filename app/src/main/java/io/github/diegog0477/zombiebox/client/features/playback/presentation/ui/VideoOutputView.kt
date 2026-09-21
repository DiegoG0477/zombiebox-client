package io.github.diegog0477.zombiebox.client.features.playback.presentation.ui

import android.content.Context
import android.os.Build
import android.view.SurfaceHolder
import android.view.View
import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.SurfacePolicy
import io.github.diegog0477.zombiebox.client.features.playback.platform.HolderSurface
import io.github.diegog0477.zombiebox.client.features.playback.platform.PlayerSurface
import io.github.diegog0477.zombiebox.client.features.playback.platform.SurfaceEvidence

interface VideoOutputView {
    val view: View
    val frames: Int

    fun create(context: Context, changed: (PlayerSurface?) -> Unit, failed: () -> Unit): View

    fun setVideoSize(width: Int, height: Int)

    fun close()
}

class SurfaceOutputView : VideoOutputView {
    private lateinit var surface: VideoSurface
    override val view: View
        get() = surface

    override val frames: Int
        get() = 0

    private var changed: ((PlayerSurface?) -> Unit)? = null

    override fun create(
        context: Context,
        changed: (PlayerSurface?) -> Unit,
        failed: () -> Unit,
    ): View {
        this.changed = changed
        surface = VideoSurface(context)
        surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    this@SurfaceOutputView.changed?.invoke(HolderSurface(holder))
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    this@SurfaceOutputView.changed?.invoke(null)
                }
            }
        )
        return surface
    }

    override fun setVideoSize(width: Int, height: Int) = surface.setVideoSize(width, height)

    override fun close() {
        changed?.invoke(null)
        changed = null
    }
}

object VideoOutputFactory {
    fun textureCandidate(): VideoOutputView? {
        if (Build.VERSION.SDK_INT < 14) return null
        return try {
            Class.forName(
                    "io.github.diegog0477.zombiebox.client.features.playback.presentation.ui.Api14TextureOutput"
                )
                .getConstructor()
                .newInstance() as VideoOutputView
        } catch (_: Exception) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    fun select(context: Context, handheld: Boolean): VideoOutputView {
        val evidence = SurfaceEvidence(context)
        val policy =
            context
                .getSharedPreferences("zombie", Context.MODE_PRIVATE)
                .getString("surfaceBackend", "AUTO") ?: "AUTO"
        if (
            SurfacePolicy.texture(
                Build.VERSION.SDK_INT,
                policy,
                evidence.probe("texture-output"),
                evidence.probe("surface-reattach"),
                evidence.health(),
                System.currentTimeMillis(),
                handheld,
            )
        ) {
            textureCandidate()?.let {
                return it
            }
            evidence.failed()
        }
        return SurfaceOutputView()
    }
}
