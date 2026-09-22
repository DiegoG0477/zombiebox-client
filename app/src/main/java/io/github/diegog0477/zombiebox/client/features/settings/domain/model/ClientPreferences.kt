package io.github.diegog0477.zombiebox.client.features.settings.domain.model

data class ClientPreferences(
    val language: String = "en",
    val mode: String = "AUTO",
    val playbackMode: String = "AUTO",
    val automaticRecovery: Boolean = true,
    val networkAdaptation: Boolean = true,
    val systemMediaControls: Boolean = true,
    val surfaceBackend: String = "AUTO",
)
