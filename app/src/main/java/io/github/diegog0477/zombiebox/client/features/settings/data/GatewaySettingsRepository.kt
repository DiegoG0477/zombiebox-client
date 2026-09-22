package io.github.diegog0477.zombiebox.client.features.settings.data

import android.content.Context
import io.github.diegog0477.zombiebox.client.features.settings.domain.model.*
import io.github.diegog0477.zombiebox.client.features.settings.domain.repository.SettingsRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class GatewaySettingsRepository(context: Context, private val api: GatewayApi) :
    SettingsRepository {
    private val prefs =
        context.applicationContext.getSharedPreferences("zombie", Context.MODE_PRIVATE)
    private val registration = RegistrationPayload(context.applicationContext)

    override fun preferences() =
        ClientPreferences(
            prefs.getString("language", "en") ?: "en",
            prefs.getString("mode", "AUTO") ?: "AUTO",
            prefs.getString("playbackMode", "AUTO") ?: "AUTO",
            prefs.getBoolean("automaticRecovery", true),
            prefs.getBoolean("networkAdaptation", true),
            prefs.getBoolean("systemMediaControls", true),
            prefs.getString("surfaceBackend", "AUTO") ?: "AUTO",
        )

    override fun saveLocalPreferences(value: ClientPreferences) {
        prefs
            .edit()
            .putString("language", value.language)
            .putString("mode", value.mode)
            .putString("playbackMode", value.playbackMode)
            .putBoolean("automaticRecovery", value.automaticRecovery)
            .putBoolean("networkAdaptation", value.networkAdaptation)
            .putBoolean("systemMediaControls", value.systemMediaControls)
            .putString("surfaceBackend", value.surfaceBackend)
            .commit()
    }

    override fun pair(address: String, code: String): GatewayProfile {
        val identifier =
            prefs.getString("installation", null)
                ?: UUID.randomUUID().toString().also {
                    prefs.edit().putString("installation", it).commit()
                }
        val candidate = GatewayApi().apply { base = address }
        return try {
            val result =
                candidate.request(
                    "POST",
                    "/v1/devices/register",
                    registration.create(identifier).put("pairingCode", code),
                )
            GatewayProfile(address, result.getString("deviceId"), result.getString("deviceToken"))
        } finally {
            candidate.close()
        }
    }

    override fun activate(profile: GatewayProfile) {
        api.disconnect()
        api.configure(profile.address, profile.device, profile.token)
        prefs
            .edit()
            .putString("gateway", profile.address)
            .putString("device", profile.device)
            .putString("token", profile.token)
            .commit()
    }

    override fun providers(): List<ProviderSettings> {
        val values = api.request("GET", "/v1/providers").getJSONArray("providers")
        return (0 until values.length()).map { index ->
            val value = values.getJSONObject(index)
            ProviderSettings(
                value.getString("id"),
                value.optBoolean("enabled"),
                value.optBoolean("managedByServer"),
                value.optBoolean("implemented"),
                value.optBoolean("configured"),
                value.optBoolean("hasToken"),
            )
        }
    }

    override fun saveProvider(id: String, patch: ProviderPatch, code: String) {
        val value = JSONObject().put("enabled", patch.enabled)
        patch.values.forEach { (key, content) ->
            if (key == "epgMappings") {
                val mappings = JSONObject()
                val lines = content.lines().filter { it.isNotBlank() }
                require(lines.size <= 256)
                lines.forEach { line ->
                    val parts = line.split("=", limit = 2)
                    require(
                        parts.size == 2 &&
                            parts.all { it.trim().isNotEmpty() && it.trim().length <= 200 }
                    )
                    mappings.put(parts[0].trim(), parts[1].trim())
                }
                value.put(key, mappings)
            } else value.put(key, content)
        }
        api.request("PUT", "/v1/providers/$id", value, code)
    }

    override fun savePreferences(mode: String, language: String) {
        val current = api.request("GET", "/v1/device/preferences")
        current.put("mode", mode).put("uiLanguage", language)
        api.request("PUT", "/v1/device/preferences", current)
    }

    override fun mediaPreferences(): MediaPreferences {
        val value = api.request("GET", "/v1/device/preferences")
        fun languages(key: String): List<String> {
            val list = value.optJSONArray(key) ?: return emptyList()
            return (0 until list.length()).map { list.getString(it) }
        }
        return MediaPreferences(
            languages("audioLanguages"),
            languages("subtitleLanguages"),
            value.getString("subtitleMode"),
        )
    }

    override fun saveMediaPreferences(value: MediaPreferences) {
        val current = api.request("GET", "/v1/device/preferences")
        current
            .put("audioLanguages", JSONArray(value.audioLanguages))
            .put("subtitleLanguages", JSONArray(value.subtitleLanguages))
            .put("subtitleMode", value.subtitleMode)
        api.request("PUT", "/v1/device/preferences", current)
    }
}
