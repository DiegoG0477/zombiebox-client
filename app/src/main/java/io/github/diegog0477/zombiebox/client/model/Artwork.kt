package io.github.diegog0477.zombiebox.client.model
interface ArtworkRepository {
    fun image(path: String, hero: Boolean): ByteArray
}
