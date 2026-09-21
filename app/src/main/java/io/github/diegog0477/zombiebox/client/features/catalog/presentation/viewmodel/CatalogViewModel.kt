package io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.*
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.CatalogRepository

/** Owns bounded navigation snapshots, page offsets and stable return focus. */
class CatalogViewModel(private val repository: CatalogRepository, private val tasks: ScreenTasks) {
    private var generation = 0
    private val history = ArrayList<CatalogScreen>()
    var screen: CatalogScreen? = null
        private set

    val canBack: Boolean
        get() = history.isNotEmpty()

    fun open(
        provider: String,
        query: String,
        done: (CatalogScreen) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        history.clear()
        screen = null
        load(CatalogLocation(provider, query = query), false, done, failed)
    }

    fun rememberViewport(viewport: CatalogViewport) {
        screen = screen?.copy(viewport = viewport)
    }

    fun enter(item: MediaItem, done: (CatalogScreen) -> Unit, failed: (Exception) -> Unit) {
        val current = screen ?: return
        if (item.browseId.isNotEmpty())
            load(
                current.location.copy(parent = item.browseId, offset = 0, query = ""),
                true,
                done,
                failed,
            )
    }

    fun next(done: (CatalogScreen) -> Unit, failed: (Exception) -> Unit) {
        val current = screen ?: return
        if (current.page.nextOffset >= 0)
            load(current.location.copy(offset = current.page.nextOffset), true, done, failed)
    }

    fun back(done: (CatalogScreen) -> Unit): Boolean = back(done, {})

    fun back(done: (CatalogScreen) -> Unit, failed: (Exception) -> Unit): Boolean {
        generation++
        if (history.isEmpty()) return false
        screen = history.removeAt(history.lastIndex)
        val target = screen!!
        if (target.page.items.isEmpty())
            restorePage(CatalogBookmark(target.location, target.viewport), done, failed)
        else done(target)
        return true
    }

    fun bookmarks(): List<CatalogBookmark> =
        (history + listOfNotNull(screen)).takeLast(25).map {
            CatalogBookmark(it.location, it.viewport)
        }

    fun restore(
        bookmarks: List<CatalogBookmark>,
        done: (CatalogScreen) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        dismiss()
        val path = bookmarks.takeLast(25)
        if (path.isEmpty()) return
        history.addAll(
            path.dropLast(1).map {
                CatalogScreen(it.location, CatalogPage(emptyList(), -1), it.viewport)
            }
        )
        restorePage(path.last(), done, failed)
    }

    private fun restorePage(
        bookmark: CatalogBookmark,
        done: (CatalogScreen) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        load(
            bookmark.location,
            false,
            { loaded ->
                val restored = loaded.copy(viewport = bookmark.viewport)
                screen = restored
                done(restored)
            },
            failed,
        )
    }

    fun search(query: String, done: (CatalogScreen) -> Unit, failed: (Exception) -> Unit) {
        val current = screen ?: return
        load(current.location.copy(query = query, offset = 0), true, done, failed)
    }

    private fun load(
        location: CatalogLocation,
        remember: Boolean,
        done: (CatalogScreen) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        val request = ++generation
        val previous = screen
        tasks.run(
            {
                repository.page(location.provider, location.query, location.offset, location.parent)
            },
            { page ->
                if (request == generation) {
                    if (remember && previous != null) {
                        if (history.size == 24) history.removeAt(0)
                        history.add(previous)
                    }
                    val next = CatalogScreen(location, page)
                    screen = next
                    done(next)
                }
            },
            { if (request == generation) failed(it) },
        )
    }

    fun cancelPending() {
        generation++
    }

    fun dismiss() {
        generation++
        history.clear()
        screen = null
    }

    fun close() {
        dismiss()
        tasks.close()
    }
}
