package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model

data class HardwareReport(
    val fingerprint: String,
    val product: String,
    val device: String,
    val abis: List<String>,
    val cores: Int,
    val memoryMb: Int,
    val storageFreeMb: Long,
    val gles: Int,
    val keyboard: Boolean,
    val mouse: Boolean,
    val network: String,
    val decoders: List<CodecHint>,
    val externalPlayers: List<String>,
    val latencyMs: Int = 0,
    val integrationHints: List<String> = emptyList(),
    val encoders: List<CodecHint> = emptyList(),
    val displays: List<DisplayHint> = emptyList(),
    val inventoryLimited: Boolean = false,
)
