package io.github.diegog0477.zombiebox.client.features.artwork.domain.repository

interface ArtworkRepository {
    fun image(path: String, hero: Boolean): ByteArray
}
