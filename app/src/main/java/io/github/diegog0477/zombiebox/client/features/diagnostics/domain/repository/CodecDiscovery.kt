package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.CodecHint

interface CodecDiscovery {
    fun decoders(): List<CodecHint>
}
