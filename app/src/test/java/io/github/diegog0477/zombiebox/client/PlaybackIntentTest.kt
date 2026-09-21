package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.PlaybackIntent
import org.junit.Assert.*
import org.junit.Test

class PlaybackIntentTest {
    @Test
    fun surfaceAndForegroundLossPreserveIntent() {
        val intent = PlaybackIntent()
        intent.begin(true, true)
        assertFalse(intent.canPlay)
        intent.surfaceAvailable = true
        assertTrue(intent.canPlay)
        intent.foreground = false
        intent.surfaceAvailable = false
        assertTrue(intent.wantsPlayback)
        intent.foreground = true
        assertFalse(intent.canPlay)
        intent.surfaceAvailable = true
        assertTrue(intent.canPlay)
    }

    @Test
    fun explicitPauseWinsOverRestorationAndDelayedPreparation() {
        val intent = PlaybackIntent()
        intent.begin(true, true)
        intent.pause()
        intent.surfaceAvailable = true
        intent.foreground = false
        intent.foreground = true
        assertFalse(intent.canPlay)
        intent.resume()
        assertTrue(intent.canPlay)
    }

    @Test
    fun audioDoesNotNeedVideoSurfaceButStillHonorsForeground() {
        val intent = PlaybackIntent()
        intent.begin(true, false)
        assertTrue(intent.canPlay)
        intent.foreground = false
        assertFalse(intent.canPlay)
        intent.foreground = true
        assertTrue(intent.canPlay)
        intent.begin(false, false)
        assertFalse(intent.canPlay)
    }
}
