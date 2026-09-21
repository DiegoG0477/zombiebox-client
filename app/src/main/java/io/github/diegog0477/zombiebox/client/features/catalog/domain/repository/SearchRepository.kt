package io.github.diegog0477.zombiebox.client.features.catalog.domain.repository

import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.SearchSection

interface SearchRepository {
    fun search(query: String): List<SearchSection>
}
