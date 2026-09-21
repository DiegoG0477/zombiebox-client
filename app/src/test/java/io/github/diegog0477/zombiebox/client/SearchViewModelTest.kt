package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.SearchSection
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.SearchRepository
import io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel.SearchViewModel
import org.junit.Assert.*
import org.junit.Test

class SearchViewModelTest {
    @Test
    fun debounceAndOneInFlightDiscardObsoleteQueries() {
        val requests = mutableListOf<String>()
        val work = mutableListOf<() -> Unit>()
        val delays = mutableListOf<() -> Unit>()
        val model =
            SearchViewModel(
                object : SearchRepository {
                    override fun search(query: String): List<SearchSection> {
                        requests.add(query)
                        return listOf(SearchSection(query, "READY", emptyList(), false))
                    }
                },
                { work.add(it) },
                { it() },
                { _, task ->
                    delays.add(task);
                    {
                        delays.remove(task)
                        Unit
                    }
                },
            )
        model.edit("a")
        assertTrue(delays.isEmpty())
        model.edit("alpha")
        model.edit("beta")
        assertEquals(1, delays.size)
        delays.removeAt(0)()
        assertEquals(1, work.size)
        model.edit("gamma")
        delays.removeAt(0)()
        assertEquals(1, work.size)
        work.removeAt(0)()
        assertTrue(model.state.sections.isEmpty())
        assertEquals(1, work.size)
        work.removeAt(0)()
        assertEquals(listOf("beta", "gamma"), requests)
        assertEquals("gamma", model.state.sections.single().provider)
        assertEquals("READY", model.state.phase)
    }

    @Test
    fun DismissAndFailureNeverPublishOldResults() {
        val work = mutableListOf<() -> Unit>()
        val model =
            SearchViewModel(
                object : SearchRepository {
                    override fun search(query: String): List<SearchSection> =
                        throw IllegalStateException()
                },
                { work.add(it) },
                { it() },
                { _, _ -> {} },
            )
        var published = 0
        model.observer = { published++ }
        model.edit("query", true)
        work.removeAt(0)()
        assertEquals("ERROR", model.state.phase)
        model.edit("retry", true)
        val before = published
        model.dismiss()
        work.removeAt(0)()
        assertEquals(before, published)
        model.close()
        model.edit("closed", true)
        assertTrue(work.isEmpty())
    }
}
