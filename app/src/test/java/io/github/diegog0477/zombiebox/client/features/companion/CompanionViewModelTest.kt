package io.github.diegog0477.zombiebox.client.features.companion

import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.features.companion.domain.repository.CompanionRepository
import io.github.diegog0477.zombiebox.client.features.companion.presentation.viewmodel.CompanionViewModel
import io.github.diegog0477.zombiebox.shared.companion.*
import org.junit.Assert.*
import org.junit.Test

class CompanionViewModelTest {
    private class Fake : CompanionRepository {
        var polls = 0

        override fun invite() = PairingInvitation("123456", byteArrayOf(), 120000)

        override fun inventory() = CompanionInventory(emptyList(), emptyList())

        override fun decide(id: String, accept: Boolean) {}

        override fun revoke(id: String) {}

        override fun poll(active: Boolean): List<RemoteCommand> {
            polls++
            return listOf(RemoteCommand("a".repeat(32), "OK", "", 500))
        }

        override fun acknowledge(id: String, status: String) {}
    }

    @Test
    fun delayedUiDeliveryCannotExtendCommandLifetime() {
        var clock = 1000L
        val deliveries = mutableListOf<() -> Unit>()
        val repo = Fake()
        val model =
            CompanionViewModel(repo, ScreenTasks({ it() }, { deliveries.add(it) }), { clock })
        var lifetime = -1L
        model.poll(true, { lifetime = it.single().remainingMs }, {})
        model.poll(true, { fail("overlapping poll") }, {})
        assertEquals(1, repo.polls)
        clock += 600
        deliveries.removeAt(0)()
        assertEquals(0L, lifetime)
    }

    @Test
    fun closedTargetDoesNotReceiveLateRemoteInput() {
        val deliveries = mutableListOf<() -> Unit>()
        val model = CompanionViewModel(Fake(), ScreenTasks({ it() }, { deliveries.add(it) }), { 0 })
        model.poll(true, { fail("input after close") }, { fail("consent after close") })
        model.close()
        deliveries.removeAt(0)()
    }

    @Test
    fun changingGatewayDropsOldCommandsAndConsent() {
        var target = "first"
        val deliveries = mutableListOf<() -> Unit>()
        val model =
            CompanionViewModel(
                Fake(),
                ScreenTasks({ it() }, { deliveries.add(it) }),
                { 0 },
                { target },
            )
        model.poll(true, { fail("old target command") }, { fail("old target consent") })
        target = "second"
        deliveries.removeAt(0)()
    }
}
