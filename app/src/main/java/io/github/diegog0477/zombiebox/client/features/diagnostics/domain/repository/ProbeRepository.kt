package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeAsset
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeResult

interface ProbeRepository {
    fun assets(): List<ProbeAsset>

    fun save(results: List<ProbeResult>)
}
