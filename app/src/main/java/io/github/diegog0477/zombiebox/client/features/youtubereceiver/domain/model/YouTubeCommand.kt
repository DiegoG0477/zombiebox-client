package io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model

data class YouTubeCommand(
    val id: String,
    val action: String,
    val itemId: String = "",
    val positionMs: Int = 0,
    val volume: Int = 100,
    val muted: Boolean = false,
)
