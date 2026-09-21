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

    @Test
    fun navigationReusesCacheButExpiryAndConnectionResetRefetch() {
        var calls = 0
        var clock = 0L
        val repository =
            object : ArtworkRepository {
                override fun image(path: String, hero: Boolean): ByteArray {
                    calls++
                    return byteArrayOf(1)
                }
            }
        val model = ArtworkViewModel(repository, { it() }, { it() }, 2, { clock })
        model.load("/v1/artwork/a", false) {}
        model.reset(clearCache = false)
        model.load("/v1/artwork/a", false) {}
        assertEquals(1, calls)
        clock = 300001
        model.load("/v1/artwork/a", false) {}
        assertEquals(2, calls)
        model.reset()
        model.load("/v1/artwork/a", false) {}
        assertEquals(3, calls)
        model.load("/v1/artwork/b", false) {}
        model.load("/v1/artwork/c", false) {}
        model.load("/v1/artwork/a", false) {}
        assertEquals(6, calls)
    }
}
