package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogPage
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.CatalogRepository
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackResumeRepository
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.PlaybackSessionViewModel
import org.junit.Assert.*
import org.junit.Test

class PlaybackSessionViewModelTest {
    @Test
    fun manualReplacementSuspendsNextAndPreservesQueueAndSubtitleChoice() {
        val r = Repository()
        val vm = model(r)
        vm.adopt(plan("a"), item("a"), listOf(item("a"), item("b")))
        vm.subtitle(3)
        vm.suspendForReplacement()
        vm.next()
        assertFalse(vm.state.canNext)
        assertFalse(r.events.any { it.startsWith("start:") })
        vm.adopt(plan("replacement"), item("a"))
        assertEquals(3, vm.state.subtitleId)
        assertTrue(vm.state.canNext)
        vm.next()
        assertEquals("b", vm.state.item?.id)
        assertNull(vm.state.subtitleId)
    }

    @Test
    fun receiverEndRestoresQueuePositionPauseAndSubtitleWithoutActivity() {
        val repository = Repository()
        val vm = model(repository)
        vm.adopt(plan("original"), item("original"), listOf(item("original"), item("next")))
        vm.mediaState("PAUSED", 12000, 60000)
        vm.subtitle(2)
        vm.rememberInterruption()
        vm.stop(preserveInterrupted = true)
        vm.adopt(plan("incoming"), item("incoming"), emptyList(), incoming = true)
        var played = false
        vm.play = { _, _ -> played = true }
        assertTrue(vm.restoreInterrupted())
        assertTrue(played)
        assertFalse(vm.state.incoming)
        assertEquals("original", vm.state.item?.id)
        assertEquals("PAUSED", vm.state.progress.state)
        assertEquals(2, vm.state.subtitleId)
        assertEquals(listOf("next"), vm.state.queue.map { it.id })
        assertTrue(repository.events.contains("start:original:12000"))
        assertFalse(vm.restoreInterrupted())
    }

    @Test
    fun receiverToReceiverChangesPreserveTheOriginalInterruptedQueue() {
        val repository = Repository()
        val vm = model(repository)
        vm.adopt(plan("original"), item("original"), listOf(item("original"), item("next")))
        vm.mediaState("PAUSED", 12000, 60000)
        vm.subtitle(2)
        for (provider in listOf("youtube", "android_mirror", "airplay")) {
            vm.rememberInterruption()
            vm.stop(preserveInterrupted = true)
            vm.adopt(
                plan(provider),
                MediaItem(provider, provider, provider),
                emptyList(),
                incoming = true,
            )
        }
        assertTrue(vm.restoreInterrupted())
        assertEquals("original", vm.state.item?.id)
        assertEquals("PAUSED", vm.state.progress.state)
        assertEquals(2, vm.state.subtitleId)
        assertTrue(repository.events.contains("start:original:12000"))
    }

    private class Resume : PlaybackResumeRepository {
        var bookmark: PlaybackBookmark? = null

        override fun load() = bookmark

        override fun save(value: PlaybackBookmark) {
            bookmark = value
        }

        override fun clear() {
            bookmark = null
        }
    }

    @Test
    fun processRestartRequiresExplicitResumeAndKeepsQueuePositionAndSubtitleOff() {
        val repository = Repository()
        val saved = Resume()
        val original =
            PlaybackSessionViewModel(
                repository,
                repository,
                {},
                { it() },
                { it() },
                resumeRepository = saved,
            )
        original.adopt(plan("old").copy(subtitleId = 3), item("a"), listOf(item("a"), item("b")))
        original.subtitle(null)
        original.mediaState("PAUSED", 42000, 100000)
        original.close()
        assertEquals(42000, saved.bookmark?.positionMs)
        assertNull(saved.bookmark?.subtitleId)
        val restored =
            PlaybackSessionViewModel(
                repository,
                repository,
                {},
                { it() },
                { it() },
                resumeRepository = saved,
            )
        assertNull(restored.state.plan)
        var played = false
        restored.play = { _, _ -> played = true }
        restored.resumeSaved { fail("checkpoint missing") }
        assertTrue(played)
        assertTrue(repository.events.contains("start:a:42000"))
        assertEquals(listOf("b"), restored.state.queue.map { it.id })
        assertNull(restored.state.subtitleId)
        restored.stop()
        assertNull(saved.bookmark)
    }

    @Test
    fun cancelledResumeRevokesLatePlanAndIncomingMediaNeverOverwritesCheckpoint() {
        val repository = Repository()
        val saved = Resume()
        val vm =
            PlaybackSessionViewModel(
                repository,
                repository,
                {},
                { it() },
                { it() },
                resumeRepository = saved,
            )
        vm.adopt(plan("original"), item("a"), listOf(item("a"), item("b")))
        vm.mediaState("PAUSED", 12000, 60000)
        vm.adopt(plan("receiver"), item("cast"), emptyList(), incoming = true)
        vm.mediaState("PLAYING", 0, 0)
        assertEquals("a", saved.bookmark?.item?.id)
        vm.stop(preserveResume = true)
        repository.onStart = { vm.stop() }
        vm.resumeSaved { fail("checkpoint missing") }
        assertNull(vm.state.plan)
        assertTrue(repository.events.contains("stop:a"))
    }

    private fun item(id: String) = MediaItem(id, "plex", id)

    private fun plan(id: String) = PlaybackPlan(id, "/stream/$id", "video/mp4", "REMUX", 0)

    private class Repository : PlaybackRepository, CatalogRepository {
        val events = mutableListOf<String>()
        var page = CatalogPage(emptyList(), -1)
        var onStart: (() -> Unit)? = null

        override fun start(itemId: String, mode: String, positionMs: Int?): PlaybackPlan {
            events.add("start:$itemId:$positionMs")
            onStart?.invoke()
            return PlaybackPlan(itemId, "/stream", "video/mp4", mode, 0)
        }

        override fun stop(sessionId: String) {
            events.add("stop:$sessionId")
        }

        override fun progress(sessionId: String, progress: PlaybackProgress) {
            events.add("progress:$sessionId:${progress.state}:${progress.positionMs}")
        }

        override fun page(
            provider: String,
            query: String,
            offset: Int,
            parent: String,
        ): CatalogPage {
            events.add("page:$provider:$parent:$offset")
            return page
        }
    }

    private fun model(
        r: Repository,
        execute: (() -> Unit) -> Unit = { it() },
        deliver: (() -> Unit) -> Unit = { it() },
    ) = PlaybackSessionViewModel(r, r, { r.events.add("receiver-stop:$it") }, execute, deliver)

    @Test
    fun rebindingObserverKeepsPositionPlanAndPausedIntentWithoutRestarting() {
        val r = Repository()
        val vm = model(r)
        vm.adopt(plan("a").copy(timelineOffsetMs = 1000), item("a"), listOf(item("a"), item("b")))
        vm.subtitle(2)
        vm.mediaState("PAUSED", 42000, 100000)
        vm.observer = null
        var restored = PlaybackSession()
        vm.observer = { restored = it }
        vm.observer?.invoke(vm.state)
        assertEquals("a", restored.plan?.sessionId)
        assertEquals(43000, restored.progress.positionMs)
        assertEquals("PAUSED", restored.progress.state)
        assertEquals(2, restored.subtitleId)
        assertEquals(listOf(item("b")), restored.queue)
        assertFalse(r.events.any { it.startsWith("start:") || it.startsWith("stop:") })
    }

    @Test
    fun duplicateCompletionStartsOneNextItemAfterRevokingTheOldPlan() {
        val r = Repository()
        val work = mutableListOf<() -> Unit>()
        val vm = model(r, { work.add(it) })
        val played = mutableListOf<String>()
        vm.play = { p, _ -> played.add(p.sessionId) }
        vm.adopt(
            plan("a"),
            item("a"),
            listOf(item("a"), item("folder").copy(browseId = "folder"), item("b")),
        )
        vm.mediaState("ENDED", 5000, 5000)
        vm.mediaState("ENDED", 5000, 5000)
        while (work.isNotEmpty()) work.removeAt(0)()
        assertEquals(listOf("b"), played)
        assertTrue(r.events.indexOf("stop:a") < r.events.indexOf("start:b:0"))
        assertFalse(vm.state.canNext)
    }

    @Test
    fun queuePagesInTheSameProviderAndParent() {
        val r = Repository()
        r.page = CatalogPage(listOf(item("b")), -1)
        val vm = model(r)
        vm.adopt(plan("a"), item("a"), listOf(item("a")), QueueCursor("plex", "season-1", "", 50))
        vm.next()
        assertTrue(r.events.contains("page:plex:season-1:50"))
        assertEquals("b", vm.state.item?.id)
        assertFalse(vm.state.canNext)
    }

    @Test
    fun emptyPagesAreBoundedAndStalledOffsetsAreDiscarded() {
        val r = Repository()
        r.page = CatalogPage(emptyList(), 10)
        val vm = model(r)
        vm.adopt(plan("a"), item("a"), listOf(item("a")), QueueCursor("plex", "season", "", 0))
        vm.next()
        assertEquals(2, r.events.count { it.startsWith("page:") })
        assertFalse(vm.state.canNext)
        assertFalse(vm.state.loading)
    }

    @Test
    fun stoppingWhileNextResolvesDiscardsTheLateSession() {
        val r = Repository()
        val vm = model(r)
        vm.adopt(plan("a"), item("a"), listOf(item("a"), item("b")))
        r.onStart = { vm.stop() }
        vm.next()
        assertNull(vm.state.plan)
        assertTrue(r.events.contains("stop:b"))
    }

    @Test
    fun closingAfterAPlanWaitsForDeliveryRevokesIt() {
        val r = Repository()
        val deliveries = mutableListOf<() -> Unit>()
        val vm = model(r, deliver = { deliveries.add(it) })
        vm.adopt(plan("a"), item("a"), listOf(item("a"), item("b")))
        vm.next()
        vm.close()
        deliveries.forEach { it() }
        assertTrue(r.events.contains("stop:b"))
        assertNull(vm.state.plan)
    }

    @Test
    fun liveAndIncomingSessionsNeverAutoAdvanceAndReceiverStopUsesItsOwnPort() {
        val r = Repository()
        val vm = model(r)
        vm.adopt(plan("live").copy(live = true), item("live"), listOf(item("live"), item("b")))
        vm.mediaState("ENDED", 0, 0)
        assertFalse(r.events.any { it.startsWith("start:") })
        r.events.clear()
        vm.adopt(
            plan("receiver"),
            item("receiver"),
            listOf(item("receiver"), item("b")),
            incoming = true,
        )
        vm.mediaState("PLAYING", 50, 0)
        vm.next()
        vm.stop()
        assertEquals(listOf("receiver-stop:receiver"), r.events)
    }

    @Test
    fun automaticRecoveryIsBoundedAndPreservesPositionQueueAndSubtitle() {
        val repository = Repository()
        var now = 1000L
        val vm = PlaybackSessionViewModel(repository, repository, {}, { it() }, { it() }, { now })
        vm.adopt(plan("old"), item("a"), listOf(item("a"), item("b")))
        vm.subtitle(4)
        for (attempt in 1..3) {
            vm.mediaState("FAILED", 42000, 100000)
            assertTrue(vm.state.loading)
            vm.recoveryTick()
            assertEquals(attempt - 1, repository.events.count { it.startsWith("start:") })
            now += 10000
            vm.recoveryTick()
            assertFalse(vm.state.loading)
            assertTrue(repository.events.contains("start:a:42000"))
            assertEquals(listOf("b"), vm.state.queue.map { it.id })
            assertEquals(4, vm.state.subtitleId)
        }
        vm.mediaState("FAILED", 42000, 100000)
        assertFalse(vm.state.loading)
        assertEquals("FAILED", vm.state.progress.state)
        now += 10000
        vm.recoveryTick()
        assertEquals(3, repository.events.count { it.startsWith("start:") })
    }

    @Test
    fun stoppingDuringRecoveryRevokesLatePlanAndIncomingNeverUsesThisRecovery() {
        val repository = Repository()
        var now = 1000L
        val vm = PlaybackSessionViewModel(repository, repository, {}, { it() }, { it() }, { now })
        vm.adopt(plan("old"), item("a"), listOf(item("a")))
        vm.mediaState("FAILED", 100, 0)
        repository.onStart = { vm.stop() }
        now += 10000
        vm.recoveryTick()
        assertNull(vm.state.plan)
        assertTrue(repository.events.contains("stop:a"))
        repository.events.clear()
        vm.adopt(plan("incoming"), item("a"), emptyList(), incoming = true)
        vm.mediaState("FAILED", 0, 0)
        now += 10000
        vm.recoveryTick()
        assertFalse(vm.state.loading)
        assertTrue(repository.events.isEmpty())
    }

    @Test
    fun pauseHoldsScheduledRecoveryAndLateReplacementKeepsPausedIntent() {
        val repository = Repository()
        var now = 1000L
        val vm = PlaybackSessionViewModel(repository, repository, {}, { it() }, { it() }, { now })
        vm.adopt(plan("old"), item("a"), listOf(item("a")))
        vm.mediaState("FAILED", 42000, 100000)
        assertTrue(vm.setRecoveryPaused(true))
        now += 10000
        vm.recoveryTick()
        assertFalse(repository.events.any { it.startsWith("start:") })
        assertTrue(vm.setRecoveryPaused(false))
        repository.onStart = { vm.setRecoveryPaused(true) }
        vm.recoveryTick()
        assertEquals("PAUSED", vm.state.progress.state)
        assertFalse(vm.state.loading)
    }
}
