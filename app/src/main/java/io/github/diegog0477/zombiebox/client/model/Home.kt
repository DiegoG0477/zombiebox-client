package io.github.diegog0477.zombiebox.client.model

data class Programme(val title: String, val start: Long)
data class MediaItem(val id: String, val provider: String, val title: String, val subtitle: String = "",
    val description: String = "", val positionMs: Int = 0, val programmes: List<Programme> = emptyList(), val imageUrl: String = "")
data class MediaSection(val id: String, val items: List<MediaItem>)
data class ServiceModule(val id: String, val state: String)
data class HomeSnapshot(val hero: MediaItem? = null, val sections: List<MediaSection> = emptyList(), val modules: List<ServiceModule> = emptyList())
data class HomeScope(val provider: String = "", val query: String = "")

interface HomeRepository { fun load(scope: HomeScope): HomeSnapshot }
