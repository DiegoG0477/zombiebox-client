package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model

data class CodecHint(
    val name: String,
    val types: List<String>,
    val probeCandidates: List<String> = emptyList(),
    val profiles: List<CodecProfileHint> = emptyList(),
    val acceleration: String = "UNKNOWN",
)

data class CodecProfileHint(val mime: String, val profile: Int, val level: Int)
