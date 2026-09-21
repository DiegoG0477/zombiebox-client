package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.artwork.domain.repository.ArtworkRepository
import io.github.diegog0477.zombiebox.client.features.artwork.presentation.viewmodel.ArtworkViewModel
import org.junit.Assert.*
import org.junit.Test

class ArtworkViewModelTest {
    @Test
    fun oldScreenCannotReceiveImagesAndWorkIsBounded() {
        val work = ArrayList<() -> Unit>()
        var shown = 0
        val repository =
            object : ArtworkRepository {
                override fun image(path: String, hero: Boolean) = byteArrayOf(1)
            }
        val model = ArtworkViewModel(repository, { work.add(it) }, { it() })
        repeat(30) { model.load("/v1/artwork/test", false) { shown++ } }
        assertEquals(12, work.size)
        model.reset()
        work.forEach { it() }
        assertEquals(0, shown)
        work.clear()
        model.load("/v1/artwork/new", true) { shown++ }
        work[0]()
        assertEquals(1, shown)
        model.close()
        model.load("/v1/artwork/closed", false) { shown++ }
        assertEquals(1, work.size)
    }
}
