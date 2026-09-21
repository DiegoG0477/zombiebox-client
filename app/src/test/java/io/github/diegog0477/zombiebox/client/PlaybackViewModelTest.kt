package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackPlan
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackProgress
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.PlaybackViewModel
import org.junit.Assert.*
import org.junit.Test

class PlaybackViewModelTest {
    private class Repository : PlaybackRepository {
        val stopped = mutableListOf<String>()

        override fun start(itemId: String, mode: String) =
            PlaybackPlan(itemId, "/stream", "video/mp4", mode, 0)

        override fun progress(sessionId: String, progress: PlaybackProgress) {}

        override fun stop(sessionId: String) {
            stopped.add(sessionId)
        }
    }

    @Test
    fun newerSelectionReleasesAnOlderPlanWaitingForTheUi() {
        val repository = Repository()
        val background = mutableListOf<() -> Unit>()
        val ui = mutableListOf<() -> Unit>()
        val model = PlaybackViewModel(repository, { background.add(it) }, { ui.add(it) })
        val played = mutableListOf<String>()
        model.start("first", "AUTO", { played.add(it.sessionId) }, { throw it })
        background.removeAt(0)()
        model.start("second", "AUTO", { played.add(it.sessionId) }, { throw it })
        ui.removeAt(0)()
        while (background.isNotEmpty()) background.removeAt(0)()
        while (ui.isNotEmpty()) ui.removeAt(0)()
        assertEquals(listOf("first"), repository.stopped)
        assertEquals(listOf("second"), played)
    }

    @Test
    fun closedScreenReleasesLatePlansAndRejectsNewWork() {
        val repository = Repository()
        val background = mutableListOf<() -> Unit>()
        val model = PlaybackViewModel(repository, { background.add(it) }, { it() })
        model.start("pending", "AUTO", { fail("delivered to closed screen") }, { throw it })
        model.close()
        background.removeAt(0)()
        model.start("ignored", "AUTO", { fail("started after close") }, { throw it })
        assertEquals(listOf("pending"), repository.stopped)
        assertTrue(background.isEmpty())
    }
}
