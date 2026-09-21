package io.github.diegog0477.zombiebox.client.features.playback.domain.model

data class MediaTrack(
    val id: Int,
    val kind: String,
    val language: String,
    val title: String,
    val default: Boolean,
    val forced: Boolean,
    val selectable: Boolean,
)

data class MediaTracks(
    val available: Boolean,
    val tracks: List<MediaTrack>,
    val audioId: Int? = null,
)

data class SubtitleCue(val startMs: Int, val endMs: Int, val text: String)

/** Bounded intervals, including overlapping cues. Seeking needs no mutable cursor. */
class SubtitleTimeline(cues: List<SubtitleCue>) {
    private val entries = cues.sortedBy { it.startMs }

    fun textAt(positionMs: Int): String =
        entries
            .asSequence()
            .takeWhile { it.startMs <= positionMs }
            .filter { positionMs < it.endMs }
            .take(4)
            .joinToString("\n") { it.text }
}
