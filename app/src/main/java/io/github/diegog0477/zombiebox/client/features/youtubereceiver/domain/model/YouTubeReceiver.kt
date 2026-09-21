package io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model

data class YouTubeReceiver(
    val id: String,
    val state: String,
    val code: String,
    val command: YouTubeCommand? = null,
)
