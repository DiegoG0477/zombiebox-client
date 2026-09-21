package io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model

data class ReceiverFeedback(
    val commandId: String = "",
    val success: Boolean = true,
    val state: String = "STOPPED",
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val volume: Int = 100,
    val muted: Boolean = false,
)
