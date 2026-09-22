package io.github.diegog0477.zombiebox.client.features.hdmi.domain.repository

/** Optional display control; discovery never implies a successful HDMI transaction. */
interface HdmiControl {
    fun availability(): String

    fun query(result: (String) -> Unit)

    fun activate(result: (String) -> Unit)
}
