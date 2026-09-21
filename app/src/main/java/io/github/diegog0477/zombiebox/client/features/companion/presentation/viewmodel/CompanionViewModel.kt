package io.github.diegog0477.zombiebox.client.features.companion.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.features.companion.domain.repository.CompanionRepository
import io.github.diegog0477.zombiebox.shared.companion.*

class CompanionViewModel(
    private val repository: CompanionRepository,
    private val tasks: ScreenTasks,
    private val now: () -> Long,
    private val scope: () -> String = { "" },
) {
    private var polling = false

    fun invite(done: (PairingInvitation) -> Unit, failed: (Exception) -> Unit) =
        tasks.run({ repository.invite() }, done, failed)

    fun inventory(done: (CompanionInventory) -> Unit, failed: (Exception) -> Unit) =
        tasks.run({ repository.inventory() }, done, failed)

    fun decide(id: String, accept: Boolean, failed: (Exception) -> Unit) =
        tasks.run({ repository.decide(id, accept) }, {}, failed)

    fun revoke(id: String, done: () -> Unit, failed: (Exception) -> Unit) =
        tasks.run({ repository.revoke(id) }, { done() }, failed)

    fun poll(
        active: Boolean,
        commands: (List<RemoteCommand>) -> Unit,
        pending: (List<PairingRequest>) -> Unit,
    ) {
        if (polling) return
        polling = true
        val started = now()
        val target = scope()
        tasks.run(
            { Pair(repository.poll(active), repository.inventory()) },
            { result ->
                polling = false
                if (scope() != target) return@run
                val elapsed = now() - started
                commands(
                    result.first.map {
                        it.copy(remainingMs = (it.remainingMs - elapsed).coerceAtLeast(0))
                    }
                )
                pending(result.second.requests)
            },
            { polling = false },
        )
    }

    fun acknowledge(id: String, status: String) =
        tasks.run({ repository.acknowledge(id, status) }, {}, {})

    fun close() = tasks.close()
}
