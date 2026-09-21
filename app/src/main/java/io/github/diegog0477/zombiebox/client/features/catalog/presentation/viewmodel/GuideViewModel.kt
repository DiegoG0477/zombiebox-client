package io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.model.MediaItem

class GuideViewModel(val channels: List<MediaItem>, private val start: Long) {
    var time = start
        private set

    fun shift(delta: Long) {
        time = (time + delta).coerceIn(start, start + 48 * 60 * 60)
    }

    fun restore(time: Long) {
        shift(time - this.time)
    }

    fun programme(channel: Int) =
        channels.getOrNull(channel)?.programmes?.firstOrNull { it.start <= time && it.end > time }
}
