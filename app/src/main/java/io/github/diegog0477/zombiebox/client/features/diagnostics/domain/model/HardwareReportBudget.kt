package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model

/** Preserve decoder probe candidates before optional verbose inventory. */
object HardwareReportBudget {
    fun fit(report: HardwareReport, size: (HardwareReport) -> Int): HardwareReport {
        var value = report
        if (size(value) <= 60 * 1024) return value
        value =
            value.copy(
                inventoryLimited = true,
                encoders = emptyList(),
                decoders = value.decoders.map { it.copy(profiles = emptyList()) },
            )
        while (size(value) > 60 * 1024 && value.decoders.isNotEmpty()) value =
            value.copy(decoders = value.decoders.dropLast(1))
        require(size(value) <= 60 * 1024)
        return value
    }
}
