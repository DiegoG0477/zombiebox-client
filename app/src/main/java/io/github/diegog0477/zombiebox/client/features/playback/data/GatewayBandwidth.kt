package io.github.diegog0477.zombiebox.client.features.playback.data

import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONObject

/** Optional LAN evidence. Missing support/errors leave playback policy unchanged. */
class GatewayBandwidth(private val api: GatewayApi) {
    private var lastAttempt = 0L
    private var identity = ""

    @Synchronized
    fun refresh() {
        val current = api.base + "|" + api.device + "|" + api.token
        val now = System.nanoTime()
        if (identity == current && now - lastAttempt in 0 until 60000000000L) return
        identity = current
        lastAttempt = now
        try {
            val sample = api.downloadSample()
            if (current != api.base + "|" + api.device + "|" + api.token) return
            api.request(
                "POST",
                "/v1/device/network",
                JSONObject()
                    .put("sampleId", sample.id)
                    .put("bytes", sample.bytes)
                    .put("elapsedMs", sample.elapsedMs),
            )
        } catch (_: Exception) {
            // Network uncertainty must not prevent a fresh playback resolution.
        }
    }
}
