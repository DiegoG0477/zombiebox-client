package io.github.diegog0477.zombiebox.client.presentation
import io.github.diegog0477.zombiebox.client.model.ArtworkRepository

/** At most twelve images per screen; no bitmap/cache survives screen replacement. */
class ArtworkViewModel(private val repository: ArtworkRepository, private val execute: (() -> Unit) -> Unit,
                       private val deliver: (() -> Unit) -> Unit) {
    private var generation = 0
    private var requested = 0
    private var closed = false
    fun reset() { generation++; requested = 0 }
    fun load(path: String, hero: Boolean, display: (ByteArray) -> Unit) {
        if (closed || path.isEmpty() || requested >= 12) return
        requested++; val screen = generation
        execute {
            val bytes = try { repository.image(path, hero) } catch (_: Exception) { null }
            if (bytes != null) deliver { if (!closed && screen == generation) display(bytes) }
        }
    }
    fun close() { closed = true; reset() }
}
