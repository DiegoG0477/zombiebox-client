package io.github.diegog0477.zombiebox.client.features.playback.domain.policy

/** User intent survives temporary visibility/surface loss; an explicit pause always wins. */
class PlaybackIntent {
    var foreground = true
    var surfaceAvailable = false
    private var video = true
    var wantsPlayback = false
        private set

    val canPlay: Boolean
        get() = wantsPlayback && foreground && (!video || surfaceAvailable)

    fun begin(autoplay: Boolean, hasVideo: Boolean) {
        wantsPlayback = autoplay
        video = hasVideo
    }

    fun pause() {
        wantsPlayback = false
    }

    fun resume() {
        wantsPlayback = true
    }

    fun toggle() {
        wantsPlayback = !wantsPlayback
    }
}
