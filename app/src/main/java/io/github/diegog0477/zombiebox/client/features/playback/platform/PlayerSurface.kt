package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.media.MediaPlayer
import android.view.SurfaceHolder

/** UI and media looper retain their own references to a platform output. */
interface PlayerSurface {
    fun retain()

    fun release()

    fun attach(player: MediaPlayer)

    fun detach(player: MediaPlayer)
}

class HolderSurface(private val holder: SurfaceHolder) : PlayerSurface {
    override fun retain() {}

    override fun release() {}

    override fun attach(player: MediaPlayer) = player.setDisplay(holder)

    override fun detach(player: MediaPlayer) = player.setDisplay(null)
}
