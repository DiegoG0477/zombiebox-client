package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel.GuideViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class GuideViewModelTest {
    @Test
    fun restoredTimeAndNavigationStayInsideRolling48HourWindow() {
        val model = GuideViewModel(emptyList(), 1000)
        model.restore(1000 + 36 * 3600)
        assertEquals(1000L + 36 * 3600, model.time)
        model.shift(24 * 3600)
        assertEquals(1000L + 48 * 3600, model.time)
        model.restore(500)
        assertEquals(1000L, model.time)
    }
}
