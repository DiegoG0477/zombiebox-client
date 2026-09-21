package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeAsset
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeResult
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.ProbePlayback
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.ProbeRepository
import io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.viewmodel.ProbeViewModel
import org.junit.Assert.*
import org.junit.Test

class ProbeViewModelTest {
    @Test
    fun cancellationDiscardsLateDecoderResultAndDoesNotSavePartialRun() {
        var saved = false
        var callback: ((ProbeResult) -> Unit)? = null
        val repository =
            object : ProbeRepository {
                override fun assets() = listOf(ProbeAsset("aac", "http://fixture", false))

                override fun save(results: List<ProbeResult>) {
                    saved = true
                }
            }
        val player =
            object : ProbePlayback {
                override fun start(asset: ProbeAsset, result: (ProbeResult) -> Unit) {
                    callback = result
                }

                override fun cancel() {}
            }
        val model = ProbeViewModel(repository, player, { it() }, { it() })
        model.start()
        assertTrue(model.state.running)
        model.cancel()
        callback!!(ProbeResult("aac", "PASS"))
        assertFalse(saved)
        assertTrue(model.state.results.isEmpty())
        assertFalse(model.state.running)
    }

    @Test
    fun failedProbeDoesNotStopRemainingSuiteAndSaveFailureIsVisible() {
        val repository =
            object : ProbeRepository {
                override fun assets() =
                    listOf(
                        ProbeAsset("aac", "http://fixture", false),
                        ProbeAsset("h264", "http://fixture", true),
                    )

                override fun save(results: List<ProbeResult>) {
                    assertEquals(2, results.size)
                    throw IllegalStateException("offline")
                }
            }
        val player =
            object : ProbePlayback {
                override fun start(asset: ProbeAsset, result: (ProbeResult) -> Unit) {
                    result(ProbeResult(asset.id, if (asset.video) "PASS" else "FAIL"))
                }

                override fun cancel() {}
            }
        val model = ProbeViewModel(repository, player, { it() }, { it() })
        model.start()
        assertTrue(model.state.failed)
        assertFalse(model.state.saved)
        assertEquals(2, model.state.results.size)
    }
}
