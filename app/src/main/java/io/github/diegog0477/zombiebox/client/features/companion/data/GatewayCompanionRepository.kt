package io.github.diegog0477.zombiebox.client.features.companion.data

import io.github.diegog0477.zombiebox.client.features.companion.domain.repository.CompanionRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import io.github.diegog0477.zombiebox.shared.companion.*

class GatewayCompanionRepository(private val api: GatewayApi) : CompanionRepository {
    override fun invite() = CompanionWire.invite(api)

    override fun inventory() = CompanionWire.inventory(api)

    override fun decide(id: String, accept: Boolean, ignore24h: Boolean) =
        CompanionWire.decide(api, id, accept, ignore24h)

    override fun revoke(id: String) = CompanionWire.revoke(api, id)

    override fun poll(active: Boolean, inputId: String) = CompanionWire.poll(api, active, inputId)

    override fun acknowledge(id: String, status: String) =
        CompanionWire.acknowledge(api, id, status)
}
