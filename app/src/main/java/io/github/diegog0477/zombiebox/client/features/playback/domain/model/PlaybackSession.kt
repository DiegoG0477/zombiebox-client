package io.github.diegog0477.zombiebox.client.features.playback.domain.model

import io.github.diegog0477.zombiebox.client.core.model.MediaItem

data class QueueCursor(val provider: String, val parent: String, val query: String, val offset: Int)

data class PlaybackSession(
    val plan: PlaybackPlan? = null,
    val item: MediaItem? = null,
    val progress: PlaybackProgress = PlaybackProgress("STOPPED", 0, 0),
    val queue: List<MediaItem> = emptyList(),
    val cursor: QueueCursor? = null,
    val incoming: Boolean = false,
    val loading: Boolean = false,
    val error: Boolean = false,
    val subtitleId: Int? = null,
) {
    val canNext: Boolean
        get() = !incoming && !loading && (queue.isNotEmpty() || cursor != null)
}
