package io.github.diegog0477.zombiebox.client.features.browser.domain.repository

interface BrowserRepository {
    fun start(url: String): String

    fun frame(id: String): ByteArray

    fun input(id: String, action: String, text: String, x: Int?, y: Int?)

    fun stop(id: String)
}

class BrowserSessionExpired : Exception()
