package io.github.diegog0477.zombiebox.client.features.browser.presentation.viewmodel

data class BrowserState(
    val session: String = "",
    val frame: ByteArray? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
)
