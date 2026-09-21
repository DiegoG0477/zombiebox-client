package io.github.diegog0477.zombiebox.client.model

data class CodecHint(val name: String, val types: List<String>)

interface CodecDiscovery {
    fun decoders(): List<CodecHint>
}

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
)

interface HardwareSource {
    fun scan(): HardwareReport
}

interface DiagnosticsRepository {
    fun scanAndSave(): HardwareReport
}
