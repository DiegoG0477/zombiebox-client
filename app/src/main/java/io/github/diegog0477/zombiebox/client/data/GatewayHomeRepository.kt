package io.github.diegog0477.zombiebox.client.data

import io.github.diegog0477.zombiebox.client.model.*
import io.github.diegog0477.zombiebox.shared.GatewayApi
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/** Gateway JSON stops here; presentation observes semantic, immutable values. */
class GatewayHomeRepository(private val api: GatewayApi) : HomeRepository {
    override fun load(scope: HomeScope): HomeSnapshot {
        val home =
            api.request(
                "GET",
                "/v1/home?provider=" +
                    URLEncoder.encode(scope.provider, "UTF-8") +
                    "&q=" +
                    URLEncoder.encode(scope.query, "UTF-8"),
            )
        val modules = api.request("GET", "/v1/modules").optJSONArray("modules") ?: JSONArray()
        val sections = home.optJSONArray("sections") ?: JSONArray()
        return HomeSnapshot(
            home.optJSONObject("hero")?.optJSONObject("item")?.let { decodeItem(it) },
            (0 until sections.length()).map { i ->
                val section = sections.getJSONObject(i)
                val items = section.optJSONArray("items") ?: JSONArray()
                MediaSection(
                    section.optString("id"),
                    (0 until items.length()).map { decodeItem(items.getJSONObject(it)) },
                )
            },
            (0 until modules.length()).map {
                ServiceModule(
                    modules.getJSONObject(it).optString("id"),
                    modules.getJSONObject(it).optString("state"),
                )
            },
        )
    }

    companion object {
        fun decodeItem(item: JSONObject): MediaItem {
            val programmes = item.optJSONArray("programmes") ?: JSONArray()
            return MediaItem(
                item.getString("id"),
                item.optString("provider"),
                item.optString("title"),
                item.optString("subtitle"),
                item.optString("description"),
                item.optInt("positionMs"),
                (0 until programmes.length()).map {
                    val programme = programmes.getJSONObject(it)
                    Programme(programme.optString("title"), programme.optLong("start"))
                },
                item.optString("imageUrl"),
            )
        }
    }
}
