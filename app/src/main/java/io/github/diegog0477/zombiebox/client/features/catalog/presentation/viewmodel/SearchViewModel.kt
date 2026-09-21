package io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.SearchState
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.SearchRepository

/** One request in flight plus the latest draft; obsolete responses cannot replace results. */
class SearchViewModel(
    private val repository: SearchRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
    private val schedule: (Long, () -> Unit) -> (() -> Unit),
) {
    var state = SearchState()
        private set

    var observer: ((SearchState) -> Unit)? = null
    private var generation = 0
    private var loading = false
    private var settled = false
    private var closed = false
    private var cancelDelay: (() -> Unit)? = null

    fun edit(draft: String, immediate: Boolean = false) {
        if (closed) return
        var query = draft.trim().take(100)
        while (query.toByteArray(Charsets.UTF_8).size > 200) query = query.dropLast(1)
        generation++
        cancelDelay?.invoke()
        settled = false
        state = SearchState(query, if (query.length < 2) "IDLE" else "WAITING")
        observer?.invoke(state)
        if (query.length < 2) return
        val request = generation
        if (immediate) {
            settled = true
            load()
        } else
            cancelDelay =
                schedule(400) {
                    if (!closed && generation == request) {
                        settled = true
                        load()
                    }
                }
    }

    private fun load() {
        if (closed || loading || !settled || state.query.length < 2) return
        loading = true
        val request = generation
        val query = state.query
        state = state.copy(phase = "LOADING")
        observer?.invoke(state)
        execute {
            val result =
                try {
                    repository.search(query)
                } catch (_: Exception) {
                    null
                }
            deliver {
                loading = false
                if (!closed && request == generation) {
                    settled = false
                    state =
                        state.copy(
                            phase = if (result == null) "ERROR" else "READY",
                            sections = result ?: emptyList(),
                        )
                    observer?.invoke(state)
                } else load()
            }
        }
    }

    fun dismiss() {
        generation++
        settled = false
        cancelDelay?.invoke()
        cancelDelay = null
        observer = null
    }

    fun close() {
        dismiss()
        closed = true
    }
}
