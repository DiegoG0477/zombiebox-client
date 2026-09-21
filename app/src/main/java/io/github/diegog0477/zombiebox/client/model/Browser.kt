package io.github.diegog0477.zombiebox.client.model

interface BrowserRepository {
    fun start(url: String): String
    fun frame(id: String): ByteArray
    fun input(id: String, action: String, text: String)
    fun stop(id: String)
}
