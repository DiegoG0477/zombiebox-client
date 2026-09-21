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
        val events = mutableListOf<String>()
        var failProgress = false
        var onStart: (() -> Unit)? = null

        override fun start(itemId: String, mode: String, positionMs: Int?): PlaybackPlan {
            events.add("start:$mode:$positionMs")
            onStart?.invoke()
            return PlaybackPlan(itemId, "/stream", "video/mp4", mode, positionMs ?: 0)
        }

        override fun receive(itemId: String, receiverId: String, positionMs: Int?): PlaybackPlan {
            events.add("receiver:$receiverId:$positionMs")
            return start(itemId, "AUTO", positionMs)
        }

        override fun progress(sessionId: String, progress: PlaybackProgress) {
            events.add("progress:${progress.positionMs}")
            if (failProgress) throw IllegalStateException("offline")
        }

        override fun stop(sessionId: String) {
            events.add("stop:$sessionId")
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
        assertTrue(repository.stopped.isEmpty())
        assertTrue(repository.events.isEmpty())
        assertTrue(background.isEmpty())
    }

    @Test
    fun compatibleRetryRetainsPositionEvenIfHistoryWriteFails() {
        val repository = Repository().apply { failProgress = true }
        val model = PlaybackViewModel(repository, { it() }, { it() })
        model.adopt(PlaybackPlan("old", "/stream", "video/mp4", "DIRECT_PLAY", 0))
        var replacement: PlaybackPlan? = null
        model.retry(
            "movie",
            "old",
            PlaybackProgress("FAILED", 42000, 120000),
            { replacement = it },
            { throw it },
        )
        assertEquals(
            listOf("progress:42000", "stop:old", "start:TRANSCODE:42000"),
            repository.events,
        )
        assertEquals(42000, replacement?.resumePositionMs)
        assertFalse(model.canRetry(42000))
    }

    @Test
    fun eachFailedCompatibleRequestAdvancesWithoutLooping() {
        val repository = Repository()
        val model = PlaybackViewModel(repository, { it() }, { it() })
        model.adopt(PlaybackPlan("old", "/stream", "video/mp4", "DIRECT_PLAY", 0))
        model.retry("movie", "old", PlaybackProgress("FAILED", 0, 0), {}, { throw it })
        assertTrue(model.canRetry(0))
        model.retry("movie", "movie", PlaybackProgress("FAILED", 0, 0), {}, { throw it })
        assertFalse(model.canRetry(0))
        assertEquals(
            listOf("start:REMUX:0", "start:TRANSCODE:0"),
            repository.events.filter { it.startsWith("start:") },
        )
    }

    @Test
    fun closingWhileRequestIsInFlightDiscardsItsNewSession() {
        val repository = Repository()
        val model = PlaybackViewModel(repository, { it() }, { it() })
        repository.onStart = { model.close() }
        model.start("late", "AUTO", { fail("late delivery") }, { throw it })
        assertEquals(listOf("late"), repository.stopped)
    }

    @Test
    fun leaseLossCancelsPendingReceiverResolutionAndDiscardsLatePlan() {
        val repository = Repository()
        val background = mutableListOf<() -> Unit>()
        val ui = mutableListOf<() -> Unit>()
        val model = PlaybackViewModel(repository, { background.add(it) }, { ui.add(it) })
        model.start("video", "AUTO", { fail("stale receiver plan") }, { throw it }, 12000, "lease")
        assertTrue(model.receiverPending)
        background.removeAt(0)()
        assertTrue(repository.events.contains("receiver:lease:12000"))
        model.stop("", null)
        assertFalse(model.receiverPending)
        ui.removeAt(0)()
        while (background.isNotEmpty()) background.removeAt(0)()
        assertEquals(listOf("video"), repository.stopped)
    }
}
