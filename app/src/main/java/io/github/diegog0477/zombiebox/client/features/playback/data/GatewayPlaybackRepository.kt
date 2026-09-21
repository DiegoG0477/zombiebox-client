package io.github.diegog0477.zombiebox.client.features.playback.data

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONObject

class GatewayPlaybackRepository(private val api: GatewayApi) : PlaybackRepository {
    override fun start(itemId: String, mode: String): PlaybackPlan {
        val plan =
            api.request(
                "POST",
                "/v1/playback",
                JSONObject().put("itemId", itemId).put("mode", mode),
            )
        return PlaybackPlanDecoder.decode(api.base, plan)
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
