package io.github.diegog0477.zombiebox.client.presentation

import io.github.diegog0477.zombiebox.client.model.*

data class HomeState(
    val scope: HomeScope = HomeScope(),
    val snapshot: HomeSnapshot = HomeSnapshot(),
    val loading: Boolean = false,
    val failure: Exception? = null,
)

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
