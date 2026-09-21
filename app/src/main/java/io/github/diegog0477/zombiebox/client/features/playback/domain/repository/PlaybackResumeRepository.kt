package io.github.diegog0477.zombiebox.client.features.playback.domain.repository

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackBookmark

interface PlaybackResumeRepository {
    fun load(): PlaybackBookmark?

    fun save(value: PlaybackBookmark)

    fun clear()
}
