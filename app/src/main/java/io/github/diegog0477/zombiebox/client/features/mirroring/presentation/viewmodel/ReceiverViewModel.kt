package io.github.diegog0477.zombiebox.client.features.mirroring.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.PlaybackContext
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.ReceiverChange
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.ReceiverPlan
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.repository.ReceiverRepository

/** One foreground poll at a time; network errors preserve the last confirmed session. */
class ReceiverViewModel(
    private val repository: ReceiverRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    private val settingsTasks = ScreenTasks(execute, deliver)

    fun readMediaProvider(done: (String) -> Unit, failed: (Exception) -> Unit) =
        settingsTasks.run({ repository.mediaProvider() }, done, failed)

    fun selectMediaProvider(provider: String, failed: (Exception) -> Unit) =
        settingsTasks.run({ repository.selectMediaProvider(provider) }, { refresh() }, failed)

    fun command(action: String, failed: (Exception) -> Unit) =
        settingsTasks.run({ repository.command(action) }, { refresh() }, failed)

    fun readEnabled(done: (Boolean) -> Unit, failed: (Exception) -> Unit) =
        settingsTasks.run({ repository.enabled() }, done, failed)

    fun setEnabled(enabled: Boolean, failed: (Exception) -> Unit) =
        settingsTasks.run({ repository.setEnabled(enabled) }, { refresh() }, failed)

    var observer: ((ReceiverPlan?) -> Unit)? = null
    private var generation = 0
    private var loading = false
    private var closed = false
    private var dismissed = ""
    var activeSession = ""
        private set

    private var playbackFailed = false
    private var attempts = 0
    private var retryAt = 0L

    fun playbackState(state: String) {
        if (state == "PLAYING") {
            playbackFailed = false
            attempts = 0
        }
        if ((state == "FAILED" || state == "ENDED") && !playbackFailed) {
            playbackFailed = true
            retryAt = clock() + 2000L * (1L shl attempts)
        }
    }

    private var lastPlan: ReceiverPlan? = null
    private var interrupted: PlaybackContext? = null

    fun transition(plan: ReceiverPlan?, current: PlaybackContext): ReceiverChange? {
        if (plan == null) {
            if (activeSession.isEmpty()) return null
            val previous = interrupted
            activeSession = ""
            lastPlan = null
            interrupted = null
            return ReceiverChange.Restore(previous)
        }
        if (plan.sessionId == dismissed) return null
        if (plan.sessionId == activeSession) {
            if (playbackFailed && plan.state == "PLAYING" && attempts < 3 && clock() >= retryAt) {
                attempts++
                playbackFailed = false
                lastPlan = plan
                return ReceiverChange.Reconnect(plan)
            }
            if (plan == lastPlan) return null
            lastPlan = plan
            return ReceiverChange.Update(plan)
        }
        lastPlan = plan
        playbackFailed = false
        attempts = 0
        if (activeSession.isEmpty()) interrupted = current
        activeSession = plan.sessionId
        return ReceiverChange.Begin(plan)
    }

    fun restore(plan: ReceiverPlan) {
        activeSession = plan.sessionId
        lastPlan = plan
        interrupted = null
        playbackFailed = false
    }

    fun dismiss(sessionId: String) {
        dismissed = sessionId
        activeSession = ""
        interrupted = null
        generation++
        loading = false
    }

    fun resumeForeground() {
        lastPlan = null
        refresh()
    }

    fun refresh() {
        if (closed || loading) return
        loading = true
        val request = ++generation
        val ignored = dismissed
        execute {
            try {
                var plan = repository.active()
                if (plan?.sessionId == ignored) {
                    repository.stop(ignored)
                    plan = null
                }
                val result = plan
                deliver {
                    if (!closed && request == generation) {
                        loading = false
                        observer?.invoke(result)
                    }
                }
            } catch (_: Exception) {
                deliver { if (request == generation) loading = false }
            }
        }
    }

    fun reset() {
        generation++
        loading = false
        activeSession = ""
        interrupted = null
        dismissed = ""
        lastPlan = null
    }

    fun close() {
        settingsTasks.close()
        closed = true
        reset()
        observer = null
    }
}
