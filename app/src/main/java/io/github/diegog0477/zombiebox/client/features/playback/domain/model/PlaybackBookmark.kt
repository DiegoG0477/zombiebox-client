package io.github.diegog0477.zombiebox.client.features.playback.domain.model

import io.github.diegog0477.zombiebox.client.core.model.MediaItem

/** Durable user intent only: no old stream URL, ticket, session ID or receiver lease. */
data class PlaybackBookmark(
    val item: MediaItem,
    val positionMs: Int,
    val queue: List<MediaItem>,
    val cursor: QueueCursor?,
    val subtitleId: Int?,
)
