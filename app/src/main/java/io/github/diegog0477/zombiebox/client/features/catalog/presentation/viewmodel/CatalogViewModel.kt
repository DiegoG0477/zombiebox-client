package io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogPage
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.CatalogRepository

/** A newer catalog request supersedes any result still queued for delivery. */
class CatalogViewModel(private val repository: CatalogRepository, private val tasks: ScreenTasks) {
    private var generation = 0

    fun page(
        provider: String,
        query: String,
        offset: Int,
        done: (CatalogPage) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        val request = ++generation
        tasks.run(
            { repository.page(provider, query, offset) },
            { if (request == generation) done(it) },
            { if (request == generation) failed(it) },
        )
    }

    fun close() {
        generation++
        tasks.close()
    }
}
