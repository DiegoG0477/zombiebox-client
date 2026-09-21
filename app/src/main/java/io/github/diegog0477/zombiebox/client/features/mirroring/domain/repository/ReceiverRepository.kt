package io.github.diegog0477.zombiebox.client.features.mirroring.domain.repository

import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.ReceiverPlan

interface ReceiverRepository {
    fun active(): ReceiverPlan?

    fun stop(sessionId: String)

    fun enabled(): Boolean

    fun setEnabled(enabled: Boolean)
}
