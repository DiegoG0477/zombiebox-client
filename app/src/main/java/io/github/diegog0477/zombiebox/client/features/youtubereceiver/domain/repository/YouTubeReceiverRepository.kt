package io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.repository

import io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model.ReceiverFeedback
import io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model.YouTubeReceiver

interface YouTubeReceiverRepository {
    fun open(): YouTubeReceiver

    fun poll(id: String): YouTubeReceiver

    fun feedback(id: String, value: ReceiverFeedback)

    fun close(id: String)
}
