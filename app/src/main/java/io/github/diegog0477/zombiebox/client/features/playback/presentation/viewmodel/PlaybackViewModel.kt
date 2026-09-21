package io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.PlaybackRecovery
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository

/** Owns pending playback requests and releases plans abandoned by a newer screen action. */
class PlaybackViewModel(
    private val repository: PlaybackRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    @Volatile private var closed = false
    @Volatile private var generation = 0
    private val recovery = PlaybackRecovery()

    fun adopt(plan: PlaybackPlan) = recovery.adopt(plan)

    fun canRetry(positionMs: Int): Boolean = recovery.next(positionMs) != null

    fun retry(
        itemId: String,
        sessionId: String,
        progress: PlaybackProgress,
        done: (PlaybackPlan) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        val mode = recovery.next(progress.positionMs) ?: return
        recovery.attempted(mode)
        request(itemId, mode, progress.positionMs, sessionId, progress, done, failed)
    }

    fun start(
        itemId: String,
        mode: String,
        done: (PlaybackPlan) -> Unit,
        failed: (Exception) -> Unit,
        positionMs: Int? = null,
    ) {
        request(itemId, mode, positionMs, "", null, done, failed)
    }

    private fun request(
        itemId: String,
        mode: String,
        positionMs: Int?,
        oldSession: String,
        progress: PlaybackProgress?,
        done: (PlaybackPlan) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        if (closed) return
        val request = ++generation
        execute {
            try {
                if (oldSession.isNotEmpty()) {
                    try {
                        if (progress != null) repository.progress(oldSession, progress)
                    } catch (_: Exception) {} finally {
                        discard(oldSession)
                    }
                }
                if (closed || request != generation) return@execute
                val plan = repository.start(itemId, mode, positionMs)
                if (closed || request != generation) {
                    discard(plan.sessionId)
                    return@execute
                }
                deliver {
                    if (!closed && request == generation) {
                        recovery.adopt(plan)
                        done(plan)
                    } else execute { discard(plan.sessionId) }
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
