package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.AutomaticRecovery
import org.junit.Assert.*
import org.junit.Test

class AutomaticRecoveryTest {
    @Test
    fun briefPlaybackDoesNotResetRetryBudget() {
        val policy = AutomaticRecovery()
        var now = 1000L
        for (attempt in 1..3) {
            assertTrue(policy.schedule(now))
            assertNull(policy.take(now))
            now += 10000
            assertEquals(attempt, policy.take(now))
            assertNull(policy.take(now))
            policy.observe("PLAYING", now)
            policy.observe("PLAYING", now + 1000)
            policy.observe("FAILED", now + 1001)
        }
        assertFalse(policy.schedule(now))
        policy.observe("PLAYING", now)
        policy.observe("PLAYING", now + 60000)
        assertTrue(policy.schedule(now + 60001))
        assertEquals(1, policy.take(now + 70000))
    }
}
