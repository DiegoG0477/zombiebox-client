package io.github.diegog0477.zombiebox.client

import org.junit.Assert.*
import org.junit.Test

class FocusModelTest {
    @Test
    fun remembersRowsAndClampsEdges() {
        val model = FocusModel()
        model.rebuild(
            listOf(
                FocusModel.Row("nav", listOf("home", "youtube")),
                FocusModel.Row("empty", emptyList()),
                FocusModel.Row("cards", listOf("a", "b", "c")),
            )
        )
        assertEquals("home", model.move(-1, 0))
        model.move(1, 0)
        model.move(0, 1)
        model.move(1, 0)
        assertEquals("c", model.move(1, 0))
        assertEquals("youtube", model.move(0, -1))
        assertEquals("c", model.move(0, 1))
    }

    @Test
    fun refreshRestoresKeyAndFallsBackAfterRemoval() {
        val model = FocusModel()
        model.rebuild(listOf(FocusModel.Row("cards", listOf("a", "b", "c"))))
        model.select("b")
        model.rebuild(listOf(FocusModel.Row("cards", listOf("c", "b", "a"))))
        assertEquals("b", model.selected)
        model.rebuild(listOf(FocusModel.Row("cards", listOf("c"))))
        assertEquals("c", model.selected)
        model.rebuild(emptyList())
        assertNull(model.selected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAmbiguousKeys() {
        FocusModel()
            .rebuild(
                listOf(FocusModel.Row("a", listOf("same")), FocusModel.Row("b", listOf("same")))
            )
    }
}
