package io.github.diegog0477.zombiebox.client.features.playback.domain.model

/** Public system metadata contains no gateway URLs, tokens or private IDs. */
data class SystemPlayback(
    val active: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val status: String = "STOPPED",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val canPause: Boolean = false,
    val canSeek: Boolean = false,
    val canNext: Boolean = false,
) {
    fun allows(command: String): Boolean =
        active &&
            when (command) {
                "play",
                "pause",
                "toggle" -> canPause
                "seek" -> canSeek
                "next" -> canNext
                "stop" -> true
                else -> false
            }

    companion object {
        fun from(session: PlaybackSession, remotePaused: Boolean = false): SystemPlayback {
            val plan = session.plan ?: return SystemPlayback()
            val progress = session.progress
            val status =
                if (remotePaused) "PAUSED" else if (session.loading) "BUFFERING" else progress.state
            val controlled = !session.incoming || !plan.live || session.item?.provider == "spotify"
            return SystemPlayback(
                active = true,
                title = session.item?.title.orEmpty().take(300),
                subtitle = session.item?.subtitle.orEmpty().take(300),
                status = status,
                positionMs = progress.positionMs.toLong().coerceAtLeast(0),
                durationMs = if (plan.live) 0 else progress.durationMs.toLong().coerceAtLeast(0),
                canPause = controlled && status in listOf("PLAYING", "PAUSED", "BUFFERING"),
                canSeek =
                    controlled &&
                        !session.loading &&
                        !plan.live &&
                        plan.seekable &&
                        progress.durationMs > 0 &&
                        status in listOf("PLAYING", "PAUSED"),
                canNext = session.canNext,
            )
        }

        fun localSeek(session: PlaybackSession, requested: Long): Int? {
            if (!from(session).canSeek || requested < 0) return null
            val offset = session.plan?.timelineOffsetMs?.coerceAtLeast(0) ?: 0
            val end = session.progress.durationMs.coerceAtLeast(offset)
            return (requested.coerceIn(offset.toLong(), end.toLong()) - offset).toInt()
        }
    }
}
