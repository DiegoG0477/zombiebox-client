package io.github.diegog0477.zombiebox.client.features.diagnostics.data

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.HardwareReport
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.DiagnosticsRepository
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.HardwareSource
import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONArray
import org.json.JSONObject

class GatewayDiagnosticsRepository(
    private val api: GatewayApi,
    private val source: HardwareSource,
) : DiagnosticsRepository {
    override fun scanAndSave(): HardwareReport {
        val result = source.scan()
        val started = System.nanoTime()
        api.request("GET", "/health")
        val report =
            result.copy(
                latencyMs = ((System.nanoTime() - started) / 1000000).toInt().coerceIn(0, 60000)
            )
        val codecs = JSONArray()
        report.decoders.forEach {
            codecs.put(JSONObject().put("name", it.name).put("types", JSONArray(it.types)))
        }
        val value =
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
                .put("decoders", codecs)
                .put("externalPlayers", JSONArray(report.externalPlayers))
                .put("integrationHints", JSONArray(report.integrationHints))
                .put("nativeDial", "UNKNOWN")
                .put("multicast", "UNKNOWN")
        api.request("PUT", "/v1/device/hardware", value)
        return report
    }
}
