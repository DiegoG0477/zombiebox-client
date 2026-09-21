package io.github.diegog0477.zombiebox.client.features.playback.domain.model

/**
 * Denied focus/network uncertainty are not backend failures. Call failed only on operation errors.
 */
data class StrategyHealth(val failures: Int = 0, val retryAfter: Long = 0) {
    fun available(now: Long): Boolean =
        failures < 2 || now >= retryAfter || retryAfter - now > 300000

    fun failed(now: Long) = StrategyHealth((failures + 1).coerceAtMost(2), now + 300000)

    fun succeeded() = StrategyHealth()
}
