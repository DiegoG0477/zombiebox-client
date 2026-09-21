package io.github.diegog0477.zombiebox.client.features.artwork.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.features.artwork.domain.repository.ArtworkRepository

/** Bounded in-flight work and a four MiB encoded-image LRU; views own decoded bitmaps. */
class ArtworkViewModel(
    private val repository: ArtworkRepository,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    private var generation = 0
    private var requested = 0
    private val cache = LinkedHashMap<String, ByteArray>()
    private var cacheBytes = 0
    private var closed = false

    fun reset() {
        generation++
        requested = 0
        cache.clear()
        cacheBytes = 0
    }

    fun load(path: String, hero: Boolean, display: (ByteArray) -> Unit) {
        if (closed || path.isEmpty()) return
        val key = "$hero:$path"
        cache.remove(key)?.let { bytes ->
            cache[key] = bytes
            display(bytes)
            return
        }
        if (requested >= 12) return
        requested++
        val screen = generation
        execute {
            val bytes =
                try {
                    repository.image(path, hero)
                } catch (_: Exception) {
                    null
                }
            deliver {
                if (!closed && screen == generation) {
                    requested--
                    if (bytes != null) {
                        if (bytes.size <= 1024 * 1024) {
                            cache.remove(key)?.let { cacheBytes -= it.size }
                            while (
                                cacheBytes + bytes.size > 4 * 1024 * 1024 && cache.isNotEmpty()
                            ) {
                                val oldest = cache.keys.first()
                                cacheBytes -= cache.remove(oldest)!!.size
                            }
                            cache[key] = bytes
                            cacheBytes += bytes.size
                        }
                        display(bytes)
                    }
                }
            }
        }
    }

    fun close() {
        closed = true
        cache.clear()
        cacheBytes = 0
        reset()
    }
}
