package io.github.diegog0477.zombiebox.client.data

import io.github.diegog0477.zombiebox.client.model.ArtworkRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi

class GatewayArtworkRepository(private val api: GatewayApi) : ArtworkRepository {
    override fun image(path: String, hero: Boolean): ByteArray {
        require(
            path.startsWith("/v1/artwork/") &&
                !path.contains("..") &&
                !path.contains('?') &&
                !path.contains('#')
        )
        val bytes = api.frame(path + if (hero) "?size=hero" else "")
        require(bytes.size <= 256 * 1024)
        return bytes
    }
}
