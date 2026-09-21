package io.github.diegog0477.zombiebox.client.features.playback.data

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONObject

class GatewayPlaybackRepository(private val api: GatewayApi) : PlaybackRepository {
    override fun start(itemId: String, mode: String, positionMs: Int?): PlaybackPlan {
        val request = JSONObject().put("itemId", itemId).put("mode", mode)
        if (positionMs != null) request.put("positionMs", positionMs)
        val plan = api.request("POST", "/v1/playback", request)
        return PlaybackPlanDecoder.decode(api.base, plan)
    }

    override fun recover(itemId: String, positionMs: Int, attempt: Int): PlaybackPlan {
        require(attempt in 1..3)
        val request =
            JSONObject()
                .put("itemId", itemId)
                .put("positionMs", positionMs)
                .put("mode", if (attempt == 1) "AUTO" else "TRANSCODE")
        if (attempt == 3) request.put("quality", "LOW")
        return PlaybackPlanDecoder.decode(api.base, api.request("POST", "/v1/playback", request))
    }

    override fun progress(sessionId: String, progress: PlaybackProgress) {
        api.request(
            "PUT",
            "/v1/playback/$sessionId/progress",
            JSONObject()
                .put("state", progress.state)
                .put("positionMs", progress.positionMs)
                .put("durationMs", progress.durationMs),
        )
    }

    override fun stop(sessionId: String) {
        api.request("DELETE", "/v1/playback/$sessionId")
    }
}
