package io.github.diegog0477.zombiebox.client.features.playback.domain.policy

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.SystemPlayback
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.SystemMediaControls

/** Optional system integration cannot terminate playback when an OEM API fails. */
class SystemControlCoordinator(
    private val create: () -> SystemMediaControls,
    private val load: () -> StrategyHealth,
    private val save: (StrategyHealth) -> Unit,
    private val clock: () -> Long,
) {
    private var backend: SystemMediaControls? = null
    private var closed = false

    fun update(state: SystemPlayback, enabled: Boolean) {
        if (closed) return
        if (!enabled || !state.active) {
            release()
            return
        }
        if (backend == null && !load().available(clock())) return
        try {
            val current = backend ?: create().also { backend = it }
            current.update(state)
            if (load().failures != 0) save(load().succeeded())
        } catch (_: Exception) {
            failed()
        } catch (_: LinkageError) {
            failed()
        }
    }

    private fun failed() {
        save(load().failed(clock()))
        release()
    }

    private fun release() {
        val previous = backend
        backend = null
        try {
            previous?.close()
        } catch (_: Exception) {} catch (_: LinkageError) {}
    }

    fun close() {
        closed = true
        release()
    }
}
