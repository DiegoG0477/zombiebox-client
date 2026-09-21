package io.github.diegog0477.zombiebox.client.features.artwork.data

import io.github.diegog0477.zombiebox.client.features.artwork.domain.repository.ArtworkRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi

class GatewayArtworkRepository(private val api: GatewayApi) : ArtworkRepository {
    override fun image(path: String, hero: Boolean): ByteArray {
        require(
            path.startsWith("/v1/artwork/") &&
                !path.contains("..") &&
                ('?' !in path || path.substringAfter('?').matches(Regex("rev=[a-f0-9]{16}"))) &&
                !path.contains('#')
        )
        val bytes =
            api.frame(path + if (hero) (if ('?' in path) "&size=hero" else "?size=hero") else "")
        require(bytes.size <= 256 * 1024)
        return bytes
    }
}
