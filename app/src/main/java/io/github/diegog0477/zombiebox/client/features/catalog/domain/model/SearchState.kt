package io.github.diegog0477.zombiebox.client.features.catalog.domain.model

import io.github.diegog0477.zombiebox.client.core.model.MediaItem

data class SearchSection(
    val provider: String,
    val state: String,
    val items: List<MediaItem>,
    val more: Boolean,
)

data class SearchState(
    val query: String = "",
    val phase: String = "IDLE",
    val sections: List<SearchSection> = emptyList(),
)

/** Small return location; result pages and artwork never enter saved instance state. */
data class SearchBookmark(
    val query: String,
    val viewport: CatalogViewport = CatalogViewport(),
    val resultsFocused: Boolean = false,
)
