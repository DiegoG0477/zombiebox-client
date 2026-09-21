package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.PlaybackContext
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.ReceiverChange
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.ReceiverPlan
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.repository.ReceiverRepository
import io.github.diegog0477.zombiebox.client.features.mirroring.presentation.viewmodel.ReceiverViewModel
import org.junit.Assert.*
import org.junit.Test

class ReceiverViewModelTest {
    private class Repo : ReceiverRepository {
        val stopped = mutableListOf<String>()

        override fun active() =
            ReceiverPlan("session", "/v1/streams/session", "application/vnd.apple.mpegurl")

        override fun stop(sessionId: String) {
            stopped.add(sessionId)
        }

        override fun enabled() = true

        override fun setEnabled(enabled: Boolean) {}
    }

    @Test
    fun dismissedCastCannotRestartFromAnInFlightPoll() {
        val work = mutableListOf<() -> Unit>()
        val ui = mutableListOf<() -> Unit>()
        val repo = Repo()
        val model = ReceiverViewModel(repo, { work.add(it) }, { ui.add(it) })
        var deliveries = 0
        model.observer = { deliveries++ }
        model.refresh()
        work.removeAt(0)()
        model.dismiss("session")
        ui.removeAt(0)()
        assertEquals(0, deliveries)
        model.refresh()
        work.removeAt(0)()
        ui.removeAt(0)()
        assertEquals(listOf("session"), repo.stopped)
        assertEquals(1, deliveries)
    }

    @Test
    fun closedReceiverDoesNotDeliverLatePlans() {
        val work = mutableListOf<() -> Unit>()
        val model = ReceiverViewModel(Repo(), { work.add(it) }, { it() })
        model.observer = { fail("closed observer called") }
        model.refresh()
        model.close()
        work.removeAt(0)()
    }

    @Test
    fun remoteStopRestoresPausedPlaybackContextButReceiverStopDiscardsIt() {
        val model = ReceiverViewModel(Repo(), { it() }, { it() })
        val previous = PlaybackContext(MediaItem("movie", "local", "Movie"), false, false)
        val cast = ReceiverPlan("cast", "/v1/streams/cast", "application/vnd.apple.mpegurl")
        assertTrue(model.transition(cast, previous) is ReceiverChange.Begin)
        assertNull(model.transition(cast, PlaybackContext(null, true, true)))
        assertEquals(
            ReceiverChange.Restore(previous),
            model.transition(null, PlaybackContext(null, true, true)),
        )
        model.transition(cast, previous)
        model.dismiss("cast")
        assertNull(model.transition(cast, previous))
        assertNull(model.transition(null, previous))
    }
}
