package io.github.diegog0477.zombiebox.client.features.mirroring.domain.model

/** Listening intent and transport availability are independent of decoder playback state. */
data class BackgroundReception(val enabled: Boolean = false, val unavailable: Boolean = false)
