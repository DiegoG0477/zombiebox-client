package io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.repository

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackPlan

interface YouTubePlaybackControl {
    fun play(plan: PlaybackPlan, item: MediaItem)

    fun pause()

    fun resume(): Boolean

    fun seek(positionMs: Int)

    fun volume(value: Int, muted: Boolean, done: (Boolean) -> Unit)
}
