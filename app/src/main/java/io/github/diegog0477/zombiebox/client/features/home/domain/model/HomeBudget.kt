package io.github.diegog0477.zombiebox.client.features.home.domain.model

/** Heap and physical memory bound realized views independently of screen resolution. */
object HomeBudget {
    fun rowCapacity(heapMb: Int, physicalMb: Int, television: Boolean): Int {
        val low = heapMb <= 64 || physicalMb in 1..512
        val moderate = heapMb <= 128 || physicalMb in 1..1024
        return when {
            low -> 3
            moderate -> if (television) 5 else 3
            else -> if (television) 7 else 5
        }
    }
}
