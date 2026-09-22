package io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model

data class YouTubeReception(
    val enabled: Boolean = false,
    val receiver: YouTubeReceiver? = null,
    val failed: Boolean = false,
)
