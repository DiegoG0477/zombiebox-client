package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.core.model.ItemWindow
import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeBudgetTest {
    @Test
    fun budgetsRespectBothMemoryMeasurements() {
        assertEquals(3, HomeBudget.rowCapacity(64, 8192, true))
        assertEquals(3, HomeBudget.rowCapacity(256, 512, true))
        assertEquals(5, HomeBudget.rowCapacity(96, 1024, true))
        assertEquals(3, HomeBudget.rowCapacity(96, 1024, false))
        assertEquals(7, HomeBudget.rowCapacity(256, 8192, true))
        assertEquals(5, HomeBudget.rowCapacity(256, 8192, false))
    }

    @Test
    fun everyItemRemainsReachableAtEveryBudget() {
        for (capacity in listOf(3, 5, 7)) {
            val window = ItemWindow(capacity)
            for (total in 1..80) {
                for (index in 0 until total) {
                    val first = window.first(index, total)
                    assertTrue(first >= 0)
                    assertTrue(index in first until minOf(first + capacity, total))
                }
            }
        }
    }
}
