package io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model

/** The owned lease expired or was replaced, rather than an unknown network failure. */
class ReceiverExpired : Exception("Receiver lease ended")
