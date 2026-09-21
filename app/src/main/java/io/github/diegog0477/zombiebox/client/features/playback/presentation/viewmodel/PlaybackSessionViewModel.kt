package io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.CatalogRepository
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.AutomaticRecovery
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackResumeRepository

/**
 * Service-scoped semantic state, progress and finite paged queue. No Activity/platform references.
 */
class PlaybackSessionViewModel(
    private val repository: PlaybackRepository,
    private val catalog: CatalogRepository,
    private val stopIncoming: (String) -> Unit,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val resumeRepository: PlaybackResumeRepository? = null,
) {
    var state = PlaybackSession()
        private set

    var observer: ((PlaybackSession) -> Unit)? = null
    var play: ((PlaybackPlan, MediaItem) -> Unit)? = null
    var stopPlayer: (() -> Unit)? = null
    var autoplay = true
    var automaticRecovery = true
    private val recovery = AutomaticRecovery()
    private var recoveryActive = false
    private var recoveryPaused = false
    @Volatile private var generation = 0
    @Volatile private var closed = false
    private var reported = 0L
    private var ended = ""
    private var interrupted: PlaybackSession? = null

    fun adopt(
        plan: PlaybackPlan,
        item: MediaItem,
        items: List<MediaItem>? = null,
        cursor: QueueCursor? = null,
        incoming: Boolean = false,
        recovering: Boolean = false,
        paused: Boolean = false,
    ) {
        generation++
        if (!recovering) recovery.reset()
        recoveryActive = false
        recoveryPaused = false
        val rest = if (items == null) state.queue else items.dropWhile { it.id != item.id }.drop(1)
        state =
            PlaybackSession(
                plan,
                item,
                PlaybackProgress(
                    if (paused) "PAUSED" else "BUFFERING",
                    plan.resumePositionMs + plan.timelineOffsetMs,
                    0,
                ),
                rest.filter { it.playable && it.browseId.isEmpty() }.take(200),
                if (items == null) state.cursor else cursor,
                incoming,
                subtitleId = if (items == null) state.subtitleId else plan.subtitleId,
            )
        checkpoint()
        ended = ""
        reported = 0
        observer?.invoke(state)
    }

    fun subtitle(id: Int?) {
        state = state.copy(subtitleId = id)
        checkpoint()
    }

    fun mediaState(status: String, position: Int, duration: Int) {
        val plan = state.plan ?: return
        if (state.loading) return
        val value =
            PlaybackProgress(
                status,
                position + plan.timelineOffsetMs,
                if (duration > 0) duration + plan.timelineOffsetMs else 0,
            )
        val previous = state.progress.state
        state = state.copy(progress = value)
        if (!state.incoming && (status != previous || clock() - reported >= 10000)) {
            reported = clock()
            checkpoint()
            execute {
                try {
                    repository.progress(plan.sessionId, value)
                } catch (_: Exception) {}
            }
        }
        recovery.observe(status, clock())
        if (status == "FAILED") scheduleRecovery()
        observer?.invoke(state)
        if (status == "ENDED" && ended != plan.sessionId) {
            ended = plan.sessionId
            if (autoplay && !plan.live && !state.incoming && state.canNext) next()
        }
    }

    fun suspendForReplacement() {
        recoveryActive = false
        recoveryPaused = false
        recovery.reset()
        generation++
        state = state.copy(loading = true)
        stopPlayer?.invoke()
        observer?.invoke(state)
    }

    fun replacementFailed() {
        state = state.copy(loading = false, progress = state.progress.copy(state = "FAILED"))
        observer?.invoke(state)
    }

    fun next() {
        if (closed || !state.canNext) return
        recovery.reset()
        recoveryActive = false
        recoveryPaused = false
        val before = state
        val request = ++generation
        state = state.copy(loading = true, error = false)
        observer?.invoke(state)
        execute {
            try {
                var items = before.queue
                var cursor = before.cursor
                var pages = 0
                while (items.isEmpty() && cursor != null && pages++ < 3) {
                    val page =
                        catalog.page(cursor.provider, cursor.query, cursor.offset, cursor.parent)
                    items = page.items.filter { it.playable && it.browseId.isEmpty() }
                    cursor =
                        if (page.nextOffset > cursor.offset) cursor.copy(offset = page.nextOffset)
                        else null
                }
                if (closed || request != generation) return@execute
                val item = items.firstOrNull()
                if (item == null) {
                    deliver {
                        if (!closed && request == generation) {
                            state = state.copy(loading = false, cursor = cursor)
                            observer?.invoke(state)
                        }
                    }
                    return@execute
                }
                // Revocation before conversion releases the previous worker slot.
                before.plan?.let { old ->
                    try {
                        repository.progress(old.sessionId, before.progress)
                    } catch (_: Exception) {}
                    try {
                        repository.stop(old.sessionId)
                    } catch (_: Exception) {}
                }
                if (closed || request != generation) return@execute
                val plan = repository.start(item.id, "AUTO", 0)
                if (closed || request != generation) {
                    discard(plan.sessionId)
                    return@execute
                }
                deliver {
                    if (closed || request != generation) execute { discard(plan.sessionId) }
                    else {
                        stopPlayer?.invoke()
                        adopt(plan, item, items, cursor)
                        play?.invoke(plan, item)
                    }
                }
            } catch (_: Exception) {
                deliver {
                    if (!closed && request == generation) {
                        state = state.copy(loading = false, error = true)
                        observer?.invoke(state)
                    }
                }
            }
        }
    }

    private fun scheduleRecovery() {
        if (!automaticRecovery || state.incoming || state.plan?.mode == "EXTERNAL_PLAYER") return
        if (recovery.schedule(clock())) {
            recoveryActive = true
            state =
                state.copy(
                    loading = true,
                    progress =
                        state.progress.copy(state = if (recoveryPaused) "PAUSED" else "BUFFERING"),
                )
        }
    }

    fun setRecoveryPaused(paused: Boolean): Boolean {
        if (!recoveryActive || !state.loading) return false
        recoveryPaused = paused
        state =
            state.copy(
                progress = state.progress.copy(state = if (paused) "PAUSED" else "BUFFERING")
            )
        observer?.invoke(state)
        return true
    }

    /** Service clock tick; no Activity is needed for bounded reconnection. */
    fun recoveryTick() {
        if (closed || !automaticRecovery || state.incoming || !state.loading || recoveryPaused)
            return
        val before = state
        val old = before.plan ?: return
        val item = before.item ?: return
        val attempt = recovery.take(clock()) ?: return
        val request = ++generation
        stopPlayer?.invoke()
        execute {
            try {
                try {
                    repository.progress(old.sessionId, before.progress.copy(state = "FAILED"))
                } catch (_: Exception) {}
                discard(old.sessionId)
                if (closed || request != generation) return@execute
                val plan =
                    repository.recover(
                        item.id,
                        if (old.live) 0 else before.progress.positionMs,
                        attempt,
                    )
                deliver {
                    if (closed || request != generation) execute { discard(plan.sessionId) }
                    else {
                        adopt(plan, item, recovering = true, paused = recoveryPaused)
                        play?.invoke(plan, item)
                    }
                }
            } catch (_: Exception) {
                deliver {
                    if (!closed && request == generation) {
                        state =
                            before.copy(
                                loading = false,
                                progress = before.progress.copy(state = "FAILED"),
                            )
                        scheduleRecovery()
                        observer?.invoke(state)
                    }
                }
            }
        }
    }

    private fun discard(id: String) {
        try {
            repository.stop(id)
        } catch (_: Exception) {}
    }

    fun rememberInterruption() {
        if (!state.incoming && state.plan != null) interrupted = state
    }

    fun metadata(item: MediaItem) {
        if (!state.incoming || state.item == item) return
        state = state.copy(item = item)
        observer?.invoke(state)
    }

    fun restoreInterrupted(): Boolean {
        val previous = interrupted ?: return false
        val item = previous.item ?: return false
        interrupted = null
        val incoming = state.plan
        val request = ++generation
        state = state.copy(loading = true)
        stopPlayer?.invoke()
        observer?.invoke(state)
        execute {
            try {
                incoming?.let {
                    try {
                        stopIncoming(it.sessionId)
                    } catch (_: Exception) {}
                }
                val plan =
                    repository.start(
                        item.id,
                        "AUTO",
                        if (previous.plan?.live == true) 0 else previous.progress.positionMs,
                    )
                deliver {
                    if (closed || request != generation) {
                        execute { discard(plan.sessionId) }
                    } else {
                        state =
                            PlaybackSession(
                                plan = plan,
                                item = item,
                                queue = previous.queue,
                                cursor = previous.cursor,
                                subtitleId = previous.subtitleId,
                                progress =
                                    PlaybackProgress(
                                        if (previous.progress.state == "PAUSED") "PAUSED"
                                        else "BUFFERING",
                                        plan.resumePositionMs + plan.timelineOffsetMs,
                                        previous.progress.durationMs,
                                    ),
                            )
                        reported = 0
                        ended = ""
                        observer?.invoke(state)
                        play?.invoke(plan, item)
                    }
                }
            } catch (_: Exception) {
                deliver {
                    if (!closed && request == generation) {
                        state = PlaybackSession(error = true)
                        observer?.invoke(state)
                    }
                }
            }
        }
        return true
    }

    fun stop(preserveInterrupted: Boolean = false, preserveResume: Boolean = false) {
        recoveryActive = false
        recoveryPaused = false
        recovery.reset()
        if (!preserveInterrupted && !preserveResume && resumeRepository != null)
            execute {
                try {
                    resumeRepository.clear()
                } catch (_: Exception) {}
            }
        if (!preserveInterrupted) interrupted = null
        generation++
        val previous = state
        state = PlaybackSession()
        stopPlayer?.invoke()
        observer?.invoke(state)
        previous.plan?.let { plan ->
            execute {
                if (previous.incoming) {
                    try {
                        stopIncoming(plan.sessionId)
                    } catch (_: Exception) {}
                } else {
                    try {
                        repository.progress(
                            plan.sessionId,
                            previous.progress.copy(
                                state =
                                    if (previous.progress.state == "ENDED") "ENDED" else "STOPPED"
                            ),
                        )
                    } catch (_: Exception) {}
                    discard(plan.sessionId)
                }
            }
        }
    }

    private fun checkpoint() {
        val persistence = resumeRepository ?: return
        val current = state
        val item = current.item ?: return
        val plan = current.plan ?: return
        if (current.incoming || current.loading) return
        if (current.progress.state == "ENDED") {
            execute {
                try {
                    persistence.clear()
                } catch (_: Exception) {}
            }
            return
        }
        val bookmark =
            PlaybackBookmark(
                item,
                if (plan.live) 0 else current.progress.positionMs,
                current.queue,
                current.cursor,
                current.subtitleId,
            )
        execute {
            try {
                persistence.save(bookmark)
            } catch (_: Exception) {}
        }
    }

    fun resumeSaved(missing: () -> Unit) {
        val persistence =
            resumeRepository
                ?: run {
                    missing()
                    return
                }
        if (closed || state.plan != null || state.loading) return
        val request = ++generation
        state = state.copy(loading = true, error = false)
        observer?.invoke(state)
        execute {
            try {
                val bookmark = persistence.load()
                if (closed || request != generation) return@execute
                if (bookmark == null) {
                    deliver {
                        if (!closed && request == generation) {
                            state = PlaybackSession()
                            observer?.invoke(state)
                            missing()
                        }
                    }
                    return@execute
                }
                val plan = repository.start(bookmark.item.id, "AUTO", bookmark.positionMs)
                deliver {
                    if (closed || request != generation) execute { discard(plan.sessionId) }
                    else {
                        val restored = plan.copy(subtitleId = bookmark.subtitleId)
                        adopt(
                            restored,
                            bookmark.item,
                            listOf(bookmark.item) + bookmark.queue,
                            bookmark.cursor,
                        )
                        play?.invoke(restored, bookmark.item)
                    }
                }
            } catch (_: Exception) {
                deliver {
                    if (!closed && request == generation) {
                        state = PlaybackSession(error = true)
                        observer?.invoke(state)
                    }
                }
            }
        }
    }

    fun close() {
        checkpoint()
        closed = true
        stop(preserveResume = true)
        observer = null
        play = null
        stopPlayer = null
    }
}
