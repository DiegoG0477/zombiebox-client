package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.*
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.CatalogRepository
import io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel.CatalogViewModel
import org.junit.Assert.*
import org.junit.Test

class CatalogViewModelTest {
    private val folder =
        MediaItem(
            id = "folder",
            provider = "plex",
            title = "Library",
            browseId = "opaque",
            playable = false,
        )

    private class Repository(val folder: MediaItem) : CatalogRepository {
        val requests = mutableListOf<CatalogLocation>()
        var fail = false

        override fun page(
            provider: String,
            query: String,
            offset: Int,
            parent: String,
        ): CatalogPage {
            requests.add(CatalogLocation(provider, parent, query, offset))
            if (fail) throw IllegalStateException("unavailable")
            return CatalogPage(listOf(folder), if (offset == 0) 80 else 100, "Library")
        }
    }

    @Test
    fun recreationReloadsSemanticPathAndBackViewport() {
        val repository = Repository(folder)
        val first = CatalogViewModel(repository, ScreenTasks({ it() }, { it() }))
        first.open("plex", "", {}, { throw it })
        first.rememberViewport(CatalogViewport("folder", "folder", -22))
        first.enter(folder, {}, { throw it })
        first.next({}, { throw it })
        val bookmarks = first.bookmarks()
        first.close()
        val restored = CatalogViewModel(repository, ScreenTasks({ it() }, { it() }))
        restored.restore(bookmarks, {}, { throw it })
        assertEquals(80, restored.screen!!.location.offset)
        restored.back({}, { throw it })
        assertEquals("opaque", restored.screen!!.location.parent)
        restored.back({}, { throw it })
        assertEquals(-22, restored.screen!!.viewport.firstTop)
        assertFalse(restored.canBack)
    }

    @Test
    fun nestedBackRestoresStableFocusAndScrollWithoutFetchingAgain() {
        val repository = Repository(folder)
        val model = CatalogViewModel(repository, ScreenTasks({ it() }, { it() }))
        model.open("plex", "", {}, { throw it })
        val viewport = CatalogViewport("folder", "folder", -12, -12)
        model.rememberViewport(viewport)
        model.enter(folder, {}, { throw it })
        assertEquals("opaque", model.screen!!.location.parent)
        assertTrue(model.back {})
        assertEquals(viewport, model.screen!!.viewport)
        assertEquals(2, repository.requests.size)
        assertFalse(model.canBack)
    }

    @Test
    fun nextUsesReturnedOffsetAndFailedLoadPreservesPreviousScreen() {
        val repository = Repository(folder)
        val model = CatalogViewModel(repository, ScreenTasks({ it() }, { it() }))
        model.open("plex", "", {}, { throw it })
        model.next({}, { throw it })
        assertEquals(80, model.screen!!.location.offset)
        val before = model.screen
        repository.fail = true
        var failed = false
        model.next({ fail("unexpected success") }, { failed = true })
        assertTrue(failed)
        assertEquals(before, model.screen)
        model.back {}
        assertEquals(0, model.screen!!.location.offset)
    }

    @Test
    fun cancelledAndSupersededRequestsCannotOverwriteNavigation() {
        val queue = mutableListOf<() -> Unit>()
        val model = CatalogViewModel(Repository(folder), ScreenTasks({ queue.add(it) }, { it() }))
        model.open("plex", "", { fail("stale open") }, { throw it })
        model.open("jellyfin", "", {}, { throw it })
        queue.removeAt(1)()
        queue.removeAt(0)()
        assertEquals("jellyfin", model.screen!!.location.provider)
        model.enter(folder, { fail("cancelled request") }, { throw it })
        model.cancelPending()
        queue.removeAt(0)()
        assertEquals("", model.screen!!.location.parent)
        assertFalse(model.canBack)
    }

    @Test
    fun searchKeepsProviderAndParentScopeAndResetsPage() {
        val repository = Repository(folder)
        val model = CatalogViewModel(repository, ScreenTasks({ it() }, { it() }))
        model.open("plex", "", {}, { throw it })
        model.enter(folder, {}, { throw it })
        model.next({}, { throw it })
        model.search("nature", {}, { throw it })
        assertEquals(CatalogLocation("plex", "opaque", "nature", 0), repository.requests.last())
    }
}
