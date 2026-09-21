package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.HardwareReport

interface HardwareSource {
    fun scan(): HardwareReport
}
