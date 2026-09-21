package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.TracksRepository
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.TracksViewModel
import org.junit.Assert.*
import org.junit.Test

class TracksViewModelTest {
    private class Repository : TracksRepository {
        val released = mutableListOf<String>()

        override fun inventory(session: String) = MediaTracks(true, emptyList())

        override fun subtitles(session: String, track: Int) =
            listOf(SubtitleCue(100, 1000, "$session:$track"))

        override fun audio(session: String, track: Int, positionMs: Int) =
            PlaybackPlan(
                "$session-new",
                "/stream",
                "video/mp4",
                "TRANSCODE",
                0,
                positionMs,
                false,
                false,
            )

        override fun release(session: String) {
            released.add(session)
        }
    }

    @Test
    fun subtitlesDisappearOnOffAndCannotReturnFromAnOlderRequest() {
        val queue = mutableListOf<() -> Unit>()
        val model = TracksViewModel(Repository(), { queue.add(it) }, { it() })
        model.attach("one")
        model.subtitles(1, {}, { throw it })
        model.subtitles(null, {}, { throw it })
        queue.removeAt(0)()
        assertEquals("", model.textAt(500))
        assertNull(model.subtitleId)
        model.subtitles(2, {}, { throw it })
        model.attach("two")
        queue.removeAt(0)()
        assertEquals("", model.textAt(500))
    }

    @Test
    fun lateAudioPlanIsReleasedWhenPlaybackChanges() {
        val repository = Repository()
        val background = mutableListOf<() -> Unit>()
        val ui = mutableListOf<() -> Unit>()
        val model = TracksViewModel(repository, { background.add(it) }, { ui.add(it) })
        model.attach("one")
        model.audio(1, 500, { fail("stale plan") }, { throw it })
        background.removeAt(0)()
        model.attach("two")
        ui.removeAt(0)()
        background.removeAt(0)()
        assertEquals(listOf("one-new"), repository.released)
    }

    @Test
    fun audioSwitchKeepsSubtitlesAndReleasesPreviousSessionBeforeDelivery() {
        val repository = Repository()
        val model = TracksViewModel(repository, { it() }, { it() })
        model.attach("one")
        model.subtitles(2, {}, { throw it })
        model.audio(
            1,
            500,
            { plan ->
                assertEquals(500, plan.timelineOffsetMs)
                assertFalse(plan.seekable)
                assertEquals(listOf("one"), repository.released)
                assertEquals("one:2", model.textAt(500))
            },
            { throw it },
        )
        model.close()
        assertEquals("", model.textAt(500))
    }

    @Test
    fun timelineHandlesOverlapsSeekingAndExclusiveEnds() {
        val timeline =
            SubtitleTimeline(
                listOf(SubtitleCue(200, 400, "second"), SubtitleCue(100, 300, "first"))
            )
        assertEquals("", timeline.textAt(99))
        assertEquals("first\nsecond", timeline.textAt(250))
        assertEquals("second", timeline.textAt(300))
        assertEquals("", timeline.textAt(400))
        assertEquals("first", timeline.textAt(100))
    }
}
