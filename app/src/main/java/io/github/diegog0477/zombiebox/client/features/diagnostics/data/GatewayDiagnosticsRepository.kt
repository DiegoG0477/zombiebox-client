package io.github.diegog0477.zombiebox.client.features.diagnostics.data

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.HardwareReport
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.HardwareReportBudget
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.DiagnosticsRepository
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.HardwareSource
import io.github.diegog0477.zombiebox.shared.GatewayApi

class GatewayDiagnosticsRepository(
    private val api: GatewayApi,
    private val source: HardwareSource,
) : DiagnosticsRepository {
    override fun exportReport(): String = api.request("GET", "/v1/diagnostics").toString(2)

    override fun scanAndSave(): HardwareReport {
        val result = source.scan()
        val started = System.nanoTime()
        api.request("GET", "/health")
        val report =
            result.copy(
                latencyMs = ((System.nanoTime() - started) / 1000000).toInt().coerceIn(0, 60000)
            )
        val bounded =
            HardwareReportBudget.fit(report) {
                HardwareWire.encode(it).toString().toByteArray(Charsets.UTF_8).size
            }
        val value = HardwareWire.encode(bounded)
        api.request("PUT", "/v1/device/hardware", value)
        return bounded
    }
}
