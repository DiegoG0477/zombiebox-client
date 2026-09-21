package io.github.diegog0477.zombiebox.client.features.catalog.data

import io.github.diegog0477.zombiebox.client.core.data.MediaItemDecoder
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.SearchSection
import io.github.diegog0477.zombiebox.client.features.catalog.domain.repository.SearchRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import java.net.URLEncoder

class GatewaySearchRepository(private val api: GatewayApi) : SearchRepository {
    override fun search(query: String): List<SearchSection> {
        val sections =
            api.request("GET", "/v1/search?q=" + URLEncoder.encode(query, "UTF-8"))
                .getJSONArray("sections")
        require(sections.length() <= 6)
        return (0 until sections.length()).map { index ->
            val section = sections.getJSONObject(index)
            val items = section.getJSONArray("items")
            require(items.length() <= 20)
            SearchSection(
                section.getString("provider"),
                section.getString("state"),
                (0 until items.length()).map {
                    MediaItemDecoder.decodeItem(items.getJSONObject(it))
                },
                section.optBoolean("more"),
            )
        }
    }
}
