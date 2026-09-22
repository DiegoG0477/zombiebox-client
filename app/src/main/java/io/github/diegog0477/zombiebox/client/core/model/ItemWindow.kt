package io.github.diegog0477.zombiebox.client.core.model

/** A bounded realized window; all semantic items remain addressable by focus. */
data class ItemWindow(val capacity: Int) {
    init {
        require(capacity in 1..9)
    }

    fun first(index: Int, total: Int): Int {
        if (total <= capacity) return 0
        return (index.coerceIn(0, total - 1) - capacity / 2).coerceIn(0, total - capacity)
    }
}
