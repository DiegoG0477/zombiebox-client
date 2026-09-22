package io.github.diegog0477.zombiebox.client.features.hdmi.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.hdmi.domain.repository.HdmiControl
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth

/** A single bounded operation, with late OEM callbacks fenced after timeout/dismissal. */
class HdmiViewModel(
    private val control: HdmiControl,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
    private val later: (Long, () -> Unit) -> Unit,
    private val clock: () -> Long,
    private val load: () -> StrategyHealth,
    private val save: (StrategyHealth) -> Unit,
) {
    var state = "UNKNOWN"
        private set

    var observer: ((String) -> Unit)? = null
    @Volatile private var generation = 0
    @Volatile private var closed = false
    private var enabled = true
    private var active = false

    fun enabled(value: Boolean) {
        enabled = value
        generation++
        active = false
        update(if (value) "UNKNOWN" else "DISABLED")
    }

    fun query() = run(false)

    fun activate() = run(true)

    private fun run(activate: Boolean) {
        if (closed || active || !enabled) return
        if (!load().available(clock())) {
            update("COOLDOWN")
            return
        }
        val request = ++generation
        active = true
        update(if (activate) "ACTIVATING" else "QUERYING")
        fun finish(value: String, failure: Boolean) {
            if (closed || request != generation || !active) return
            active = false
            generation++
            if (failure) save(load().failed(clock()))
            else if (value in listOf("ON", "STANDBY", "TRANSITIONING", "SUCCESS"))
                save(load().succeeded())
            update(value)
        }
        later(8000) { finish("TIMEOUT", true) }
        execute {
            if (closed || request != generation) return@execute
            try {
                val available = control.availability()
                if (available != "AVAILABLE") {
                    deliver { finish(available, false) }
                } else {
                    if (closed || request != generation) return@execute
                    val callback: (String) -> Unit = { value ->
                        deliver { finish(value, value == "FAILED") }
                    }
                    if (activate) control.activate(callback) else control.query(callback)
                }
            } catch (_: Exception) {
                deliver { finish("FAILED", true) }
            } catch (_: LinkageError) {
                deliver { finish("FAILED", true) }
            }
        }
    }

    fun close() {
        closed = true
        generation++
        active = false
        observer = null
    }

    private fun update(value: String) {
        state = value
        observer?.invoke(value)
    }
}
