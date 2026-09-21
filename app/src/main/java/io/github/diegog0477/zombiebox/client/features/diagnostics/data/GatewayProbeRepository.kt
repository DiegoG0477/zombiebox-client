package io.github.diegog0477.zombiebox.client.features.diagnostics.data

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeAsset
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeResult
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.ProbeRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONArray
import org.json.JSONObject

class GatewayProbeRepository(
    private val api: GatewayApi,
    private val localAssets: () -> List<ProbeAsset> = { emptyList() },
    private val recordEvidence: (List<ProbeResult>) -> Unit = {},
    private val refreshInventory: () -> Unit = {},
) : ProbeRepository {
    private var cacheKey = ""
    private var suiteVersion = 1

    override fun assets(): List<ProbeAsset> {
        refreshInventory()
        val manifest = api.request("GET", "/v1/probes?suite=2&extended=1")
        cacheKey = manifest.optString("cacheKey")
        suiteVersion = manifest.optInt("suiteVersion", 1)
        val data = manifest.getJSONArray("probes")
        val assets =
            (0 until data.length().coerceAtMost(31)).map {
                val item = data.getJSONObject(it)
                val path = item.getString("url")
                require(path.startsWith("/v1/probes/") && !path.contains(".."))
                ProbeAsset(
                    item.getString("id"),
                    api.base + path,
                    item.getBoolean("video"),
                    item.optString("kind", "playback"),
                    item.optString("requires"),
                )
            }
        val texture =
            assets
                .firstOrNull { it.id == "h264-baseline-360" }
                ?.copy(id = "texture-output", kind = "texture-output")
        return assets + listOfNotNull(texture) + localAssets()
    }

    override fun save(results: List<ProbeResult>) {
        val capabilities = api.request("GET", "/v1/device").getJSONObject("capabilities")
        val previous =
            if (capabilities.optString("cacheKey") == cacheKey)
                capabilities.optJSONArray("probes") ?: JSONArray()
            else JSONArray()
        val replaced = results.map { it.id }.toSet()
        val values = JSONArray()
        for (i in 0 until previous.length()) if (
            previous.getJSONObject(i).optString("id") !in replaced
        )
            values.put(previous.getJSONObject(i))
        results.forEach {
            values.put(
                JSONObject()
                    .put("id", it.id)
                    .put("status", it.status)
                    .put("prepareMs", it.prepareMs)
                    .put("firstFrameMs", it.firstFrameMs)
                    .put("positionMs", it.positionMs)
                    .put("completed", it.completed)
                    .put("droppedOrStalled", it.stalled)
                    .put("testedAt", System.currentTimeMillis() / 1000L)
            )
        }
        api.request(
            "PUT",
            "/v1/device/capabilities",
            JSONObject()
                .put("capabilitiesVersion", 1)
                .put("deviceId", api.device)
                .apply {
                    if (cacheKey.isNotEmpty()) {
                        put("cacheKey", cacheKey)
                        put("suiteVersion", suiteVersion)
                    }
                }
                .put("probes", values),
        )
        recordEvidence(results)
    }
}
