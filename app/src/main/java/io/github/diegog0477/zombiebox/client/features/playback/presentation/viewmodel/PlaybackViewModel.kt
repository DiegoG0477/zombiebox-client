package io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository

/** Owns pending playback requests and releases plans abandoned by a newer screen action. */
class PlaybackViewModel(
    private val repository: PlaybackRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    @Volatile private var closed = false
    @Volatile private var generation = 0

    fun start(
        itemId: String,
        mode: String,
        done: (PlaybackPlan) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        if (closed) return
        val request = ++generation
        execute {
            try {
                val plan = repository.start(itemId, mode)
                if (closed || request != generation) {
                    discard(plan.sessionId)
                    return@execute
                }
                deliver {
                    if (!closed && request == generation) done(plan)
                    else execute { discard(plan.sessionId) }
                }
            } catch (error: Exception) {
                deliver { if (!closed && request == generation) failed(error) }
            }
        }
    }

    fun progress(id: String, value: PlaybackProgress) {
        if (!closed)
            execute {
                try {
                    repository.progress(id, value)
                } catch (_: Exception) {}
            }
    }

    fun stop(id: String, value: PlaybackProgress?) {
        generation++
        if (id.isNotEmpty())
            execute {
                try {
                    if (value != null) repository.progress(id, value)
                } catch (_: Exception) {} finally {
                    discard(id)
                }
            }
    }

    private fun discard(id: String) {
        try {
            repository.stop(id)
        } catch (_: Exception) {}
    }

    fun close() {
        closed = true
        generation++
    }
}
