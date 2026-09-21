package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.browser.domain.repository.BrowserRepository
import io.github.diegog0477.zombiebox.client.features.browser.presentation.viewmodel.BrowserViewModel
import org.junit.Assert.*
import org.junit.Test

class BrowserViewModelTest {
    private class Repository : BrowserRepository {
        val stopped = ArrayList<String>()

        override fun start(url: String) = "browser"

        override fun frame(id: String) = byteArrayOf(1)

        override fun input(id: String, action: String, text: String, x: Int?, y: Int?) {}

        override fun stop(id: String) {
            stopped.add(id)
        }
    }

    @Test
    fun closeBeforeCreationReturnsReleasesLateSession() {
        val repository = Repository()
        val work = ArrayList<() -> Unit>()
        val model = BrowserViewModel(repository, { work.add(it) }, { it() })
        model.open("https://example.org")
        model.close()
        work.removeAt(0)()
        assertEquals(listOf("browser"), repository.stopped)
        assertEquals("", model.state.session)
    }

    @Test
    fun closeBetweenWorkAndDeliveryStillReleasesSession() {
        val repository = Repository()
        val work = ArrayList<() -> Unit>()
        val delivery = ArrayList<() -> Unit>()
        val model = BrowserViewModel(repository, { work.add(it) }, { delivery.add(it) })
        model.open("https://example.org")
        work.removeAt(0)()
        model.close()
        work.removeAt(0)()
        delivery.removeAt(0)()
        assertEquals(listOf("browser"), repository.stopped)
        assertEquals("", model.state.session)
    }
}
