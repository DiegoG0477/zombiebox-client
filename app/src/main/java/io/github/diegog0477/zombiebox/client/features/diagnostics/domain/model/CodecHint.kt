package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model

data class CodecHint(
    val name: String,
    val types: List<String>,
    val probeCandidates: List<String> = emptyList(),
)
