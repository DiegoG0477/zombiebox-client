package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.HardwareReport
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.DiagnosticsRepository
import io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.viewmodel.DiagnosticsViewModel
import org.junit.Assert.*
import org.junit.Test

class DiagnosticsViewModelTest {
    @Test
    fun repeatedRescansAreBoundedAndClosedScreenGetsNoResult() {
        val tasks = ArrayList<() -> Unit>()
        var calls = 0
        var updates = 0
        val repository =
            object : DiagnosticsRepository {
                override fun scanAndSave(): HardwareReport {
                    calls++
                    throw IllegalStateException("offline")
                }
            }
        val model = DiagnosticsViewModel(repository, { tasks.add(it) }, { it() })
        model.observer = { _, _ -> updates++ }
        repeat(20) { model.scan() }
        assertEquals(1, tasks.size)
        model.close()
        tasks.removeAt(0)()
        assertEquals(1, calls)
        assertEquals(0, updates)
    }

    @Test
    fun failureAllowsAnExplicitRetry() {
        val tasks = ArrayList<() -> Unit>()
        var failed = false
        val repository =
            object : DiagnosticsRepository {
                override fun scanAndSave(): HardwareReport = throw IllegalStateException("offline")
            }
        val model = DiagnosticsViewModel(repository, { tasks.add(it) }, { it() })
        model.observer = { _, error -> failed = error }
        model.scan()
        tasks.removeAt(0)()
        assertTrue(failed)
        model.scan()
        assertEquals(1, tasks.size)
    }
}
