package io.github.diegog0477.zombiebox.client.model

data class ReceiverPlan(val sessionId: String, val path: String, val mime: String)

interface ReceiverRepository {
    fun active(): ReceiverPlan?

    fun stop(sessionId: String)

    fun enabled(): Boolean

    fun setEnabled(enabled: Boolean)
}

data class PlaybackContext(val item: MediaItem?, val fullscreen: Boolean, val playing: Boolean)

sealed class ReceiverChange {
    data class Begin(val plan: ReceiverPlan) : ReceiverChange()

    data class Restore(val previous: PlaybackContext?) : ReceiverChange()
}
