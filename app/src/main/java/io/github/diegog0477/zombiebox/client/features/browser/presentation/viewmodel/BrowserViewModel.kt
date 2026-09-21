package io.github.diegog0477.zombiebox.client.features.browser.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.browser.domain.repository.BrowserRepository

class BrowserViewModel(
    private val repository: BrowserRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    var state = BrowserState()
        private set

    var observer: ((BrowserState) -> Unit)? = null
    @Volatile private var closed = false
    private val lifetime = Any()
    private var activeID = ""

    fun open(url: String) {
        if (closed || state.loading) return
        if (state.session.isNotEmpty()) {
            input("navigate", url)
            return
        }
        state = state.copy(loading = true, failed = false)
        observer?.invoke(state)
        execute {
            var id = ""
            try {
                id = repository.start(url)
                val abandoned =
                    synchronized(lifetime) {
                        if (closed) true
                        else {
                            activeID = id
                            false
                        }
                    }
                if (abandoned) {
                    repository.stop(id)
                    return@execute
                }
                val frame = repository.frame(id)
                deliver {
                    if (!closed) {
                        state = BrowserState(id, frame)
                        observer?.invoke(state)
                    }
                }
            } catch (_: Exception) {
                if (id.isNotEmpty())
                    try {
                        repository.stop(id)
                    } catch (_: Exception) {}
                synchronized(lifetime) { if (activeID == id) activeID = "" }
                deliver {
                    if (!closed) {
                        state = BrowserState(failed = true)
                        observer?.invoke(state)
                    }
                }
            }
        }
    }

    fun refresh() = update(null, "")

    fun input(action: String, text: String = "") = update(action, text)

    private fun update(action: String?, text: String) {
        if (closed || state.loading || state.session.isEmpty()) return
        val previous = state
        state = state.copy(loading = true, failed = false)
        observer?.invoke(state)
        execute {
            val next =
                try {
                    if (action != null) repository.input(previous.session, action, text)
                    previous.copy(
                        frame = repository.frame(previous.session),
                        loading = false,
                        failed = false,
                    )
                } catch (_: Exception) {
                    previous.copy(loading = false, failed = true)
                }
            deliver {
                if (!closed) {
                    state = next
                    observer?.invoke(state)
                }
            }
        }
    }

    fun close() {
        if (closed) return
        val id =
            synchronized(lifetime) {
                closed = true
                val current = activeID
                activeID = ""
                current
            }
        state = BrowserState()
        observer = null
        if (id.isNotEmpty())
            execute {
                try {
                    repository.stop(id)
                } catch (_: Exception) {}
            }
    }
}
