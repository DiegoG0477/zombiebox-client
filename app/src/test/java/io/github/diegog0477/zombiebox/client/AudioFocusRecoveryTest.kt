package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth
import io.github.diegog0477.zombiebox.client.features.playback.platform.AudioFocusController
import io.github.diegog0477.zombiebox.client.features.playback.platform.RecoveringAudioFocus
import org.junit.Assert.*
import org.junit.Test

class AudioFocusRecoveryTest {
    @Test
    fun denialIsRespectedAndOperationFailuresUseBoundedFallback() {
        var health = StrategyHealth()
        var now = 100L
        var broken = false
        var nativeCalls = 0
        var fallbackCalls = 0
        val native =
            object : AudioFocusController {
                override fun acquire(): Boolean {
                    nativeCalls++
                    if (broken) error("OEM failure")
                    return false
                }

                override fun release() {}
            }
        val fallback =
            object : AudioFocusController {
                override fun acquire(): Boolean {
                    fallbackCalls++
                    return true
                }

                override fun release() {}
            }
        val focus = RecoveringAudioFocus(native, fallback, { health }, { health = it }, { now })
        assertFalse(focus.acquire())
        assertEquals(0, fallbackCalls)
        assertEquals(0, health.failures)
        broken = true
        assertTrue(focus.acquire())
        assertTrue(focus.acquire())
        val calls = nativeCalls
        assertTrue(focus.acquire())
        assertEquals(calls, nativeCalls)
        now += 300001
        assertTrue(focus.acquire())
        assertEquals(calls + 1, nativeCalls)
    }
}
