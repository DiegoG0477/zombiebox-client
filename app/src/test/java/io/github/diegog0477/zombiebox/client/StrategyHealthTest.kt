package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth
import org.junit.Assert.*
import org.junit.Test

class StrategyHealthTest {
    @Test
    fun twoOperationFailuresOpenCircuitAndSuccessResetsIt() {
        val once = StrategyHealth().failed(100)
        assertTrue(once.available(200))
        val twice = once.failed(200)
        assertFalse(twice.available(300))
        assertTrue(twice.available(300200))
        assertTrue(twice.succeeded().available(300))
        assertEquals(0, twice.succeeded().failures)
    }
}
