package io.github.diegog0477.zombiebox.client.features.diagnostics.data

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.CodecHint
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.HardwareReport
import org.json.JSONArray
import org.json.JSONObject

object HardwareWire {
    private fun codecs(values: List<CodecHint>) =
        JSONArray().apply {
            values.forEach { codec ->
                put(
                    JSONObject()
                        .put("name", codec.name)
                        .put("types", JSONArray(codec.types))
                        .put("probeCandidates", JSONArray(codec.probeCandidates))
                        .put("acceleration", codec.acceleration)
                        .put(
                            "profiles",
                            JSONArray().apply {
                                codec.profiles.forEach {
                                    put(
                                        JSONObject()
                                            .put("mime", it.mime)
                                            .put("profile", it.profile)
                                            .put("level", it.level)
                                    )
                                }
                            },
                        )
                )
            }
        }

    fun encode(report: HardwareReport): JSONObject =
        JSONObject()
            .put("scannerVersion", 1)
            .put("fingerprint", report.fingerprint)
            .put("product", report.product)
            .put("device", report.device)
            .put("abis", JSONArray(report.abis))
            .put("cpuCores", report.cores)
            .put("physicalMb", report.memoryMb)
            .put("storageFreeMb", report.storageFreeMb)
            .put("glesVersion", report.gles)
            .put("keyboard", report.keyboard)
            .put("mouse", report.mouse)
            .put("network", report.network)
            .put("gatewayLatencyMs", report.latencyMs)
            .put("decoders", codecs(report.decoders))
            .put("encoders", codecs(report.encoders))
            .put("inventoryLimited", report.inventoryLimited)
            .put("externalPlayers", JSONArray(report.externalPlayers))
            .put("integrationHints", JSONArray(report.integrationHints))
            .put("nativeDial", "UNKNOWN")
            .put("multicast", "UNKNOWN")
            .put(
                "displays",
                JSONArray().apply {
                    report.displays.forEach { display ->
                        put(
                            JSONObject()
                                .put("id", display.id)
                                .put("defaultDisplay", display.defaultDisplay)
                                .put("presentation", display.presentation)
                                .put("width", display.width)
                                .put("height", display.height)
                                .put("refreshMilliHz", display.refreshMilliHz)
                                .put("activeModeId", display.activeModeId)
                                .put(
                                    "modes",
                                    JSONArray().apply {
                                        display.modes.forEach { mode ->
                                            put(
                                                JSONObject()
                                                    .put("id", mode.id)
                                                    .put("width", mode.width)
                                                    .put("height", mode.height)
                                                    .put("refreshMilliHz", mode.refreshMilliHz)
                                            )
                                        }
                                    },
                                )
                        )
                    }
                },
            )
}
