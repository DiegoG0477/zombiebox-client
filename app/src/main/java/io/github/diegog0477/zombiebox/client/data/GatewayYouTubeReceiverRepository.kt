package io.github.diegog0477.zombiebox.client.data

import io.github.diegog0477.zombiebox.client.model.*
import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONObject

class GatewayYouTubeReceiverRepository(private val api: GatewayApi) : YouTubeReceiverRepository {
    private fun decode(value: JSONObject): YouTubeReceiver {
        val command =
            value.optJSONObject("command")?.let {
                YouTubeCommand(
                    it.getString("id"),
                    it.getString("action"),
                    it.optString("itemId"),
                    it.optInt("positionMs"),
                    it.optInt("volume", 100),
                    it.optBoolean("muted"),
                )
            }
        return YouTubeReceiver(
            value.getString("receiverId"),
            value.getString("state"),
            value.optString("tvCode"),
            command,
        )
    }

    override fun open() = decode(api.request("POST", "/v1/youtube/receiver", JSONObject()))

    override fun poll(id: String) = decode(api.request("GET", "/v1/youtube/receiver/$id"))

    override fun feedback(id: String, value: ReceiverFeedback) {
        api.request(
            "POST",
            "/v1/youtube/receiver/$id/state",
            JSONObject()
                .put("commandId", value.commandId)
                .put("success", value.success)
                .put("state", value.state)
                .put("positionMs", value.positionMs)
                .put("durationMs", value.durationMs)
                .put("volume", value.volume)
                .put("muted", value.muted),
        )
    }

    override fun close(id: String) {
        api.request("DELETE", "/v1/youtube/receiver/$id")
    }
}
