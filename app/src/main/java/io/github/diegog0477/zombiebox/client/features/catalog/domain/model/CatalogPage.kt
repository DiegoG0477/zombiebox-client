package io.github.diegog0477.zombiebox.client.features.catalog.domain.model

import io.github.diegog0477.zombiebox.client.core.model.MediaItem

data class CatalogPage(val items: List<MediaItem>, val nextOffset: Int)
