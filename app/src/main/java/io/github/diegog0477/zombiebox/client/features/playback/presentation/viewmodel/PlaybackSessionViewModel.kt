package io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.CatalogRepository
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository

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
) {
    var state = PlaybackSession()
        private set

    var observer: ((PlaybackSession) -> Unit)? = null
    var play: ((PlaybackPlan, MediaItem) -> Unit)? = null
    var stopPlayer: (() -> Unit)? = null
    var autoplay = true
    @Volatile private var generation = 0
    @Volatile private var closed = false
    private var reported = 0L
    private var ended = ""

    fun adopt(
        plan: PlaybackPlan,
        item: MediaItem,
        items: List<MediaItem>? = null,
        cursor: QueueCursor? = null,
        incoming: Boolean = false,
    ) {
        generation++
        val rest = if (items == null) state.queue else items.dropWhile { it.id != item.id }.drop(1)
        state =
            PlaybackSession(
                plan,
                item,
                PlaybackProgress("BUFFERING", plan.resumePositionMs + plan.timelineOffsetMs, 0),
                rest.filter { it.playable && it.browseId.isEmpty() }.take(200),
                if (items == null) state.cursor else cursor,
                incoming,
                subtitleId = if (items == null) state.subtitleId else null,
            )
        ended = ""
        reported = 0
        observer?.invoke(state)
    }

    fun subtitle(id: Int?) {
        state = state.copy(subtitleId = id)
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
            execute {
                try {
                    repository.progress(plan.sessionId, value)
                } catch (_: Exception) {}
            }
        }
        observer?.invoke(state)
        if (status == "ENDED" && ended != plan.sessionId) {
            ended = plan.sessionId
            if (autoplay && !plan.live && !state.incoming && state.canNext) next()
        }
    }

    fun suspendForReplacement() {
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

    private fun discard(id: String) {
        try {
            repository.stop(id)
        } catch (_: Exception) {}
    }

    fun stop() {
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

    fun close() {
        closed = true
        stop()
        observer = null
        play = null
        stopPlayer = null
    }
}
