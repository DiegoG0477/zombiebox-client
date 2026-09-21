package io.github.diegog0477.zombiebox.client.features.playback.domain.policy

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth

object SurfacePolicy {
    fun texture(
        api: Int,
        policy: String,
        textureProbe: String,
        reattachProbe: String,
        health: StrategyHealth,
        now: Long,
        handheld: Boolean,
    ): Boolean =
        api >= 14 &&
            policy != "SURFACE" &&
            textureProbe == "PASS" &&
            health.available(now) &&
            (policy == "TEXTURE" || handheld || reattachProbe == "FAIL")
}
