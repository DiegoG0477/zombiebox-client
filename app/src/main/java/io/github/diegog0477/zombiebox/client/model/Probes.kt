package io.github.diegog0477.zombiebox.client.model

data class ProbeAsset(val id: String, val url: String, val video: Boolean)
data class ProbeResult(val id: String, val status: String, val prepareMs: Int = 0, val firstFrameMs: Int = 0, val positionMs: Int = 0, val completed: Boolean = false, val stalled: Boolean = false)
interface ProbeRepository {
    fun assets(): List<ProbeAsset>
    fun save(results: List<ProbeResult>)
}
interface ProbePlayback {
    fun start(asset: ProbeAsset, result: (ProbeResult) -> Unit)
    fun cancel()
}
data class ProbeState(val running: Boolean = false, val current: String = "", val results: List<ProbeResult> = emptyList(), val saved: Boolean = false, val failed: Boolean = false)
