package io.github.diegog0477.zombiebox.client.features.playback.domain.repository

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*

interface PlaybackRepository {
    fun start(itemId: String, mode: String, positionMs: Int? = null): PlaybackPlan

    fun receive(itemId: String, receiverId: String, positionMs: Int?): PlaybackPlan =
        throw UnsupportedOperationException("Receiver playback is not available")

    fun recover(itemId: String, positionMs: Int, attempt: Int): PlaybackPlan =
        start(itemId, if (attempt == 1) "AUTO" else "TRANSCODE", positionMs)

    fun progress(sessionId: String, progress: PlaybackProgress)

    fun stop(sessionId: String)
}
