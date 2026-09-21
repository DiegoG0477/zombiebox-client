package io.github.diegog0477.zombiebox.client.data

import io.github.diegog0477.zombiebox.client.model.*
import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONArray
import org.json.JSONObject

class GatewayProbeRepository(private val api: GatewayApi) : ProbeRepository {
    override fun assets(): List<ProbeAsset> {
        val data = api.request("GET", "/v1/probes").getJSONArray("probes")
        return (0 until data.length().coerceAtMost(32)).map {
            val item = data.getJSONObject(it)
            val path = item.getString("url")
            require(path.startsWith("/v1/probes/") && !path.contains(".."))
            ProbeAsset(item.getString("id"), api.base + path, item.getBoolean("video"))
        }
    }

    override fun save(results: List<ProbeResult>) {
        val previous =
            api.request("GET", "/v1/device").getJSONObject("capabilities").optJSONArray("probes")
                ?: JSONArray()
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
            )
        }
        api.request(
            "PUT",
            "/v1/device/capabilities",
            JSONObject()
                .put("capabilitiesVersion", 1)
                .put("deviceId", api.device)
                .put("probes", values),
        )
    }
}
