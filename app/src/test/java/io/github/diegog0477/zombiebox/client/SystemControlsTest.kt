package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.SystemControlCoordinator
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.SystemMediaControls
import org.junit.Assert.*
import org.junit.Test

class SystemControlsTest {
    private fun session(
        live: Boolean = false,
        incoming: Boolean = false,
        provider: String = "plex",
    ) =
        PlaybackSession(
            plan =
                PlaybackPlan(
                    "session",
                    "http://private/token",
                    "video/mp4",
                    "DIRECT_PLAY",
                    0,
                    timelineOffsetMs = 1000,
                    live = live,
                ),
            item = MediaItem("id", provider, "Title", "Artist"),
            progress = PlaybackProgress("PLAYING", 4000, 9000),
            queue = listOf(MediaItem("next", provider, "Next")),
            incoming = incoming,
        )

    @Test
    fun vodExposesOnlySupportedActionsAndClampsTimelineSeek() {
        val session = session()
        val state = SystemPlayback.from(session)
        assertTrue(state.allows("play"))
        assertTrue(state.allows("seek"))
        assertTrue(state.allows("next"))
        assertFalse(state.allows("previous"))
        assertFalse(state.allows("arbitrary"))
        assertEquals(3000, SystemPlayback.localSeek(session, 4000))
        assertEquals(0, SystemPlayback.localSeek(session, 0))
        assertEquals(8000, SystemPlayback.localSeek(session, Long.MAX_VALUE))
        assertNull(SystemPlayback.localSeek(session, -1))
        assertFalse(state.toString().contains("http"))
    }

    @Test
    fun receiverAndLiveControlsRemainTruthful() {
        val mirror = SystemPlayback.from(session(live = true, incoming = true, provider = "cast"))
        assertTrue(mirror.allows("stop"))
        assertFalse(mirror.canPause)
        assertFalse(mirror.canNext)
        assertEquals(0L, mirror.durationMs)
        assertFalse(mirror.canSeek)
        val spotify =
            SystemPlayback.from(session(live = true, incoming = true, provider = "spotify"), true)
        assertTrue(spotify.canPause)
        assertEquals("PAUSED", spotify.status)
        assertFalse(spotify.canSeek)
        assertFalse(spotify.canNext)
        assertFalse(SystemPlayback.from(PlaybackSession()).allows("stop"))
    }

    @Test
    fun bufferingAndUnseekablePlansCannotSeek() {
        assertFalse(SystemPlayback.from(session().copy(loading = true)).canSeek)
        val value = session()
        assertFalse(
            SystemPlayback.from(value.copy(plan = value.plan!!.copy(seekable = false))).canSeek
        )
        assertFalse(
            SystemPlayback.from(value.copy(progress = PlaybackProgress("BUFFERING", 0, 0))).canSeek
        )
    }

    @Test
    fun optionalBackendFailureClosesAndOpensCircuitWithoutStoppingPlayback() {
        var health = StrategyHealth()
        var now = 1000L
        var attempts = 0
        var closes = 0
        val coordinator =
            SystemControlCoordinator(
                {
                    attempts++
                    object : SystemMediaControls {
                        override fun update(state: SystemPlayback) {
                            throw LinkageError("OEM")
                        }

                        override fun close() {
                            closes++
                        }
                    }
                },
                { health },
                { health = it },
                { now },
            )
        val active = SystemPlayback.from(session())
        repeat(10) { coordinator.update(active, true) }
        assertEquals(2, attempts)
        assertEquals(2, closes)
        assertEquals(2, health.failures)
        now += 300001
        coordinator.update(active, true)
        assertEquals(3, attempts)
        coordinator.close()
        coordinator.update(active, true)
        assertEquals(3, attempts)
    }

    @Test
    fun disablingOrStoppingReleasesOnceAndDoesNotRecreate() {
        var created = 0
        var closed = 0
        val coordinator =
            SystemControlCoordinator(
                {
                    created++
                    object : SystemMediaControls {
                        override fun update(state: SystemPlayback) {}

                        override fun close() {
                            closed++
                        }
                    }
                },
                { StrategyHealth() },
                {},
                { 0L },
            )
        val active = SystemPlayback.from(session())
        coordinator.update(active, false)
        assertEquals(0, created)
        coordinator.update(active, true)
        coordinator.update(active, false)
        coordinator.update(SystemPlayback(), true)
        assertEquals(1, created)
        assertEquals(1, closed)
        coordinator.update(active, true)
        coordinator.close()
        coordinator.close()
        assertEquals(2, created)
        assertEquals(2, closed)
    }
}
