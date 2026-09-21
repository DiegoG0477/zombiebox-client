package io.github.diegog0477.zombiebox.client.presentation

import io.github.diegog0477.zombiebox.client.model.DiagnosticsRepository
import io.github.diegog0477.zombiebox.client.model.HardwareReport

/** A screen owns at most one scan; closing it discards late results. */
class DiagnosticsViewModel(
    private val repository: DiagnosticsRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    var observer: ((HardwareReport?, Boolean) -> Unit)? = null
    private var busy = false
    private var closed = false

    fun scan() {
        if (closed || busy) return
        busy = true
        execute {
            val report =
                try {
                    repository.scanAndSave()
                } catch (_: Exception) {
                    null
                }
            deliver {
                busy = false
                if (!closed) observer?.invoke(report, report == null)
            }
        }
    }

    fun close() {
        closed = true
        observer = null
    }
}
