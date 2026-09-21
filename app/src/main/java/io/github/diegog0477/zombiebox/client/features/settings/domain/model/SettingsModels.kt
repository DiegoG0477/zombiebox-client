package io.github.diegog0477.zombiebox.client.features.settings.domain.model

data class GatewayProfile(val address: String, val device: String, val token: String)

data class ProviderSettings(
    val id: String,
    val enabled: Boolean,
    val managed: Boolean,
    val implemented: Boolean,
    val configured: Boolean,
    val hasToken: Boolean,
)

data class ProviderPatch(val enabled: Boolean, val values: Map<String, String>)
