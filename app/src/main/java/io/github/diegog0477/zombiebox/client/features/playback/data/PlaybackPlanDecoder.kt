package io.github.diegog0477.zombiebox.client.features.playback.data

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackPlan
import org.json.JSONObject

internal object PlaybackPlanDecoder {
    fun decode(base: String, plan: JSONObject) =
        PlaybackPlan(
            plan.getString("sessionId"),
            base + plan.getString("url"),
            plan.optString("mimeType", "video/mp4"),
            plan.optString("mode"),
            plan.optInt("resumePositionMs"),
            plan.optInt("timelineOffsetMs"),
            plan.optBoolean("live"),
            plan.optBoolean("seekable", true),
            if (plan.has("subtitleId")) plan.getInt("subtitleId") else null,
        )
}
