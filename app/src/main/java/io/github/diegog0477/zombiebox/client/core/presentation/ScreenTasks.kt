package io.github.diegog0477.zombiebox.client.core.presentation

/** Executes screen work and suppresses callbacks after the screen closes; no Android dependency. */
class ScreenTasks(
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    private var closed = false

    fun <T> run(work: () -> T, done: (T) -> Unit, failed: (Exception) -> Unit) {
        if (closed) return
        execute {
            try {
                val result = work()
                deliver { if (!closed) done(result) }
            } catch (error: Exception) {
                deliver { if (!closed) failed(error) }
            }
        }
    }

    fun close() {
        closed = true
    }
}
