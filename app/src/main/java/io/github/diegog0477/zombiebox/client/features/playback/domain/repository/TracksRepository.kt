package io.github.diegog0477.zombiebox.client.features.playback.domain.repository

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*

interface TracksRepository {
    fun inventory(session: String): MediaTracks

    fun subtitles(session: String, track: Int): List<SubtitleCue>

    fun audio(session: String, track: Int, positionMs: Int): PlaybackPlan

    fun release(session: String)
}
