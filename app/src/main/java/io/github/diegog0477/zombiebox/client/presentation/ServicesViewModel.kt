package io.github.diegog0477.zombiebox.client.presentation

import io.github.diegog0477.zombiebox.client.model.*

data class ServicesState(val snapshot: ServicesSnapshot = ServicesSnapshot(emptyList(), null),
    val loading: Boolean = false, val failed: Boolean = false, val prompt: AuthorizationPrompt? = null)

/** Serialized work; operator codes are transient closure arguments, never state. */
class ServicesViewModel(private val repository: ServicesRepository, private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit) {
    var state = ServicesState()
        private set
    var observer: ((ServicesState) -> Unit)? = null
    private var closed = false
    private var generation = 0
    fun refresh() = run { base -> base.copy(snapshot = repository.load(), prompt = null) }
    fun authorize(code: String) = run { base -> base.copy(prompt = repository.authorize(code)) }
    fun command(action: String, code: String) = run { base ->
        repository.command(action, code)
        base.copy(snapshot = repository.load(), prompt = null)
    }
    private fun run(work: (ServicesState) -> ServicesState) {
        if (closed || state.loading) return
        val request = ++generation
        state = state.copy(loading = true, failed = false, prompt = null)
        observer?.invoke(state)
        val base = state
        execute {
            val value = try { work(base).copy(loading = false, failed = false) }
                catch (_: Exception) { ServicesState(loading = false, failed = true) }
            deliver { if (!closed && request == generation) { state = value; observer?.invoke(state) } }
        }
    }
    fun close() { closed = true; generation++; state = ServicesState(); observer = null }
}
