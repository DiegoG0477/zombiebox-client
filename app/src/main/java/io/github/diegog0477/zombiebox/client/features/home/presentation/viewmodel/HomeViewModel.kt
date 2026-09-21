package io.github.diegog0477.zombiebox.client.features.home.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeScope
import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeSnapshot
import io.github.diegog0477.zombiebox.client.features.home.domain.repository.HomeRepository

/**
 * UI-thread state; scheduling is injected so neither Android nor a lifecycle library is required.
 */
class HomeViewModel(
    private val repository: HomeRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    var state = HomeState()
        private set

    var observer: ((HomeState) -> Unit)? = null
    private var generation = 0
    private var closed = false

    fun refresh(scope: HomeScope = state.scope) {
        if (closed || (state.loading && scope == state.scope)) return
        val request = ++generation
        state = state.copy(scope = scope, loading = true, failure = null)
        observer?.invoke(state)
        execute {
            var value: HomeSnapshot? = null
            var failure: Exception? = null
            try {
                value = repository.load(scope)
            } catch (e: Exception) {
                failure = e
            }
            deliver {
                if (!closed && request == generation) {
                    state =
                        state.copy(
                            snapshot = value ?: state.snapshot,
                            loading = false,
                            failure = failure,
                        )
                    observer?.invoke(state)
                }
            }
        }
    }

    fun reset() {
        generation++
        state = HomeState()
        observer?.invoke(state)
    }

    fun close() {
        closed = true
        generation++
        observer = null
    }
}
