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

    @Test
    fun returnReusesBoundedResultsAndSelectedViewportUntilExpiry() {
        var clock = 100L
        var requests = 0
        val model =
            SearchViewModel(
                object : SearchRepository {
                    override fun search(query: String): List<SearchSection> {
                        requests++
                        return listOf(SearchSection("plex", "READY", emptyList(), false))
                    }
                },
                { it() },
                { it() },
                { _, _ -> {} },
                { clock },
            )
        model.open("nature")
        val viewport =
            io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogViewport(
                "chosen",
                "first",
                -12,
                70,
            )
        model.rememberViewport(viewport, true)
        model.dismiss()
        var published = 0
        model.observer = { published++ }
        model.open("nature")
        assertEquals(1, requests)
        assertEquals(1, published)
        assertEquals(viewport, model.bookmark.viewport)
        assertTrue(model.bookmark.resultsFocused)
        clock += 60_000
        model.open("nature")
        assertEquals(2, requests)
        assertEquals(viewport, model.bookmark.viewport)
        model.edit("other", true)
        assertFalse(model.bookmark.resultsFocused)
        assertEquals("", model.bookmark.viewport.selectedId)
    }

    @Test
    fun restoredBookmarkReloadsWithoutSavingResultObjects() {
        val model =
            SearchViewModel(
                object : SearchRepository {
                    override fun search(query: String) = emptyList<SearchSection>()
                },
                { it() },
                { it() },
                { _, _ -> {} },
            )
        val saved =
            io.github.diegog0477.zombiebox.client.features.catalog.domain.model.SearchBookmark(
                "nature",
                io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogViewport(
                    "chosen"
                ),
                true,
            )
        model.restoreBookmark(saved)
        model.open(saved.query)
        assertEquals(saved, model.bookmark)
        assertEquals("READY", model.state.phase)
    }

    @Test
    fun returnDuringOldRequestWaitsAndNeverDisplaysItsResults() {
        val work = mutableListOf<() -> Unit>()
        val requests = mutableListOf<String>()
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
                { _, _ -> {} },
            )
        model.open("old")
        model.dismiss()
        model.open("new")
        assertEquals(1, work.size)
        work.removeAt(0)()
        assertTrue(model.state.sections.isEmpty())
        assertEquals(1, work.size)
        work.removeAt(0)()
        assertEquals(listOf("old", "new"), requests)
        assertEquals("new", model.state.sections.single().provider)
    }

    @Test
    fun gatewayResetDropsCachedResultsAndFencesOutstandingWork() {
        val work = mutableListOf<() -> Unit>()
        val model =
            SearchViewModel(
                object : SearchRepository {
                    override fun search(query: String) =
                        listOf(SearchSection(query, "READY", emptyList(), false))
                },
                { work.add(it) },
                { it() },
                { _, _ -> {} },
            )
        model.open("private")
        model.reset()
        work.removeAt(0)()
        assertEquals("IDLE", model.state.phase)
        assertTrue(model.state.sections.isEmpty())
        assertEquals("", model.bookmark.query)
        model.open("public")
        work.removeAt(0)()
        assertEquals("public", model.state.sections.single().provider)
        model.reset()
        assertTrue(model.state.sections.isEmpty())
    }
}
