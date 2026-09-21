package io.github.diegog0477.zombiebox.client.features.settings.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.features.settings.domain.model.*
import io.github.diegog0477.zombiebox.client.features.settings.domain.repository.SettingsRepository
import java.net.URI

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val tasks: ScreenTasks,
) {
    var preferences = repository.preferences()
        private set

    fun language(value: String) {
        require(value in listOf("en", "es"))
        update(preferences.copy(language = value))
    }

    fun mode(value: String) {
        require(value in listOf("AUTO", "TV", "DOCKED", "HANDHELD"))
        update(preferences.copy(mode = value))
    }

    fun playbackMode(value: String) {
        require(value in listOf("AUTO", "DIRECT_PLAY", "REMUX", "TRANSCODE", "EXTERNAL_PLAYER"))
        update(preferences.copy(playbackMode = value))
    }

    fun surfaceBackend(value: String) {
        require(value in listOf("AUTO", "SURFACE", "TEXTURE"))
        update(preferences.copy(surfaceBackend = value))
    }

    fun networkAdaptation(enabled: Boolean) {
        update(preferences.copy(networkAdaptation = enabled))
    }

    fun automaticRecovery(value: Boolean) = update(preferences.copy(automaticRecovery = value))

    private fun update(value: ClientPreferences) {
        repository.saveLocalPreferences(value)
        preferences = value
    }

    fun validAddress(address: String): Boolean =
        try {
            val uri = URI(address)
            uri.scheme in listOf("http", "https") &&
                !uri.host.isNullOrEmpty() &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null
        } catch (_: Exception) {
            false
        }

    fun pair(
        address: String,
        code: String,
        done: (GatewayProfile) -> Unit,
        failed: (Exception) -> Unit,
    ) {
        require(validAddress(address))
        tasks.run({ repository.pair(address, code) }, done, failed)
    }

    fun activate(profile: GatewayProfile) = repository.activate(profile)

    fun providers(done: (List<ProviderSettings>) -> Unit, failed: (Exception) -> Unit) =
        tasks.run({ repository.providers() }, done, failed)

    fun saveProvider(
        id: String,
        patch: ProviderPatch,
        code: String,
        done: () -> Unit,
        failed: (Exception) -> Unit,
    ) = tasks.run({ repository.saveProvider(id, patch, code) }, { done() }, failed)

    fun savePreferences(mode: String, language: String, failed: (Exception) -> Unit) =
        tasks.run({ repository.savePreferences(mode, language) }, {}, failed)

    fun mediaPreferences(done: (MediaPreferences) -> Unit, failed: (Exception) -> Unit) =
        tasks.run({ repository.mediaPreferences() }, done, failed)

    fun saveMediaPreferences(
        value: MediaPreferences,
        done: () -> Unit,
        failed: (Exception) -> Unit,
    ) {
        require(value.valid())
        tasks.run({ repository.saveMediaPreferences(value) }, { done() }, failed)
    }

    fun close() = tasks.close()
}
