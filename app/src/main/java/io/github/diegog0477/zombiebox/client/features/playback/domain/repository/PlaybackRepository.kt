package io.github.diegog0477.zombiebox.client.features.playback.domain.repository

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*

interface PlaybackRepository {
    fun start(itemId: String, mode: String): PlaybackPlan

    fun progress(sessionId: String, progress: PlaybackProgress)

    fun stop(sessionId: String)
}
