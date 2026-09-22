package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.hdmi.domain.repository.HdmiControl
import io.github.diegog0477.zombiebox.client.features.hdmi.presentation.viewmodel.HdmiViewModel
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth
import org.junit.Assert.*
import org.junit.Test

class HdmiViewModelTest {
    private class Control : HdmiControl {
        var availability = "AVAILABLE"
        var queries = 0
        var activations = 0
        var callback: ((String) -> Unit)? = null

        override fun availability() = availability

        override fun query(result: (String) -> Unit) {
            queries++
            callback = result
        }

        override fun activate(result: (String) -> Unit) {
            activations++
            callback = result
        }
    }

    private class Harness {
        val control = Control()
        val work = mutableListOf<() -> Unit>()
        val timeouts = mutableListOf<() -> Unit>()
        var health = StrategyHealth()
        var now = 1000L
        val model =
            HdmiViewModel(
                control,
                { work.add(it) },
                { it() },
                { _, task -> timeouts.add(task) },
                { now },
                { health },
                { health = it },
            )

        fun run() {
            while (work.isNotEmpty()) work.removeAt(0)()
        }
    }

    @Test
    fun deniedPermissionIsNotAFunctionalFailureOrActivation() {
        val h = Harness()
        h.control.availability = "NEEDS_PRIVILEGE"
        h.model.query()
        h.run()
        assertEquals("NEEDS_PRIVILEGE", h.model.state)
        assertEquals(0, h.control.queries)
        assertEquals(0, h.control.activations)
        assertEquals(0, h.health.failures)
    }

    @Test
    fun queryNeverActivatesAndOnlyAnActualReplyConfirmsStatus() {
        val h = Harness()
        h.model.query()
        h.run()
        assertEquals("QUERYING", h.model.state)
        h.control.callback!!("ON")
        assertEquals("ON", h.model.state)
        assertEquals(0, h.control.activations)
        h.model.activate()
        h.run()
        assertEquals(1, h.control.activations)
        h.control.callback!!("SUCCESS")
        assertEquals("SUCCESS", h.model.state)
    }

    @Test
    fun timeoutsTripCooldownAndLateSuccessCannotEraseFailure() {
        val h = Harness()
        repeat(2) {
            h.model.query()
            h.run()
            h.timeouts.removeAt(0)()
            h.control.callback!!("ON")
            assertEquals("TIMEOUT", h.model.state)
        }
        h.model.activate()
        h.run()
        assertEquals("COOLDOWN", h.model.state)
        assertEquals(0, h.control.activations)
        h.now += 300001
        h.model.query()
        h.run()
        h.control.callback!!("STANDBY")
        assertEquals(0, h.health.failures)
    }

    @Test
    fun queuedCommandsDoNotExecuteAfterDismissalDisableOrDeadline() {
        for (stop in 0..2) {
            val h = Harness()
            h.model.activate()
            when (stop) {
                0 -> h.model.close()
                1 -> h.model.enabled(false)
                2 -> h.timeouts.removeAt(0)()
            }
            h.run()
            assertEquals(0, h.control.activations)
        }
    }

    @Test
    fun unknownPowerDoesNotManufactureHealthEvidence() {
        val h = Harness()
        h.health = StrategyHealth(1, 0)
        h.model.query()
        h.run()
        h.control.callback!!("UNKNOWN")
        assertEquals("UNKNOWN", h.model.state)
        assertEquals(1, h.health.failures)
    }
}
