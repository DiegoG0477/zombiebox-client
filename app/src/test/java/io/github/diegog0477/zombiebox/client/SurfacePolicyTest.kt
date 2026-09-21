package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth
import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.SurfacePolicy
import org.junit.Assert.*
import org.junit.Test

class SurfacePolicyTest {
    @Test
    fun proofApiPolicyAndHealthMustAgree() {
        fun selected(
            api: Int = 14,
            policy: String = "AUTO",
            proof: String = "PASS",
            reattach: String = "PASS",
            health: StrategyHealth = StrategyHealth(),
            handheld: Boolean = true,
        ) = SurfacePolicy.texture(api, policy, proof, reattach, health, 1000, handheld)
        assertTrue(selected())
        assertFalse(selected(api = 13, policy = "TEXTURE"))
        assertFalse(selected(policy = "TEXTURE", proof = "UNKNOWN"))
        assertFalse(selected(policy = "TEXTURE", proof = "FAIL"))
        assertFalse(selected(policy = "SURFACE"))
        assertFalse(selected(handheld = false))
        assertTrue(selected(handheld = false, reattach = "FAIL"))
        assertFalse(selected(health = StrategyHealth(2, 10000)))
        assertTrue(selected(health = StrategyHealth(2, 999)))
    }
}
