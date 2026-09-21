package io.github.diegog0477.zombiebox.client.features.mirroring.domain.model

sealed class ReceiverChange {
    data class Begin(val plan: ReceiverPlan) : ReceiverChange()

    data class Reconnect(val plan: ReceiverPlan) : ReceiverChange()

    data class Update(val plan: ReceiverPlan) : ReceiverChange()

    data class Restore(val previous: PlaybackContext?) : ReceiverChange()
}
