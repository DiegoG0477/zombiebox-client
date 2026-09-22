package io.github.diegog0477.zombiebox.client.features.playback.domain.repository

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.SystemPlayback

interface SystemMediaControls {
    fun update(state: SystemPlayback)

    fun close()
}
