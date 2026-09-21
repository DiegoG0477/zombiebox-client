package io.github.diegog0477.zombiebox.client.features.settings.domain.repository

import io.github.diegog0477.zombiebox.client.features.settings.domain.model.*

interface SettingsRepository {
    fun mediaPreferences(): MediaPreferences

    fun saveMediaPreferences(value: MediaPreferences)

    fun preferences(): ClientPreferences

    fun saveLocalPreferences(value: ClientPreferences)

    fun pair(address: String, code: String): GatewayProfile

    fun activate(profile: GatewayProfile)

    fun providers(): List<ProviderSettings>

    fun saveProvider(id: String, patch: ProviderPatch, code: String)

    fun savePreferences(mode: String, language: String)
}
