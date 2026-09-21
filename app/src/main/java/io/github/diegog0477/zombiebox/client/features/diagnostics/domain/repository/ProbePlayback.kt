package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeAsset
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.ProbeResult

interface ProbePlayback {
    fun start(asset: ProbeAsset, result: (ProbeResult) -> Unit)

    fun cancel()
}
