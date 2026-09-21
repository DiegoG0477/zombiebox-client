package io.github.diegog0477.zombiebox.client.features.companion.domain.repository

import io.github.diegog0477.zombiebox.shared.companion.*

interface CompanionRepository {
    fun invite(): PairingInvitation

    fun inventory(): CompanionInventory

    fun decide(id: String, accept: Boolean)

    fun revoke(id: String)

    fun poll(active: Boolean): List<RemoteCommand>

    fun acknowledge(id: String, status: String)
}
