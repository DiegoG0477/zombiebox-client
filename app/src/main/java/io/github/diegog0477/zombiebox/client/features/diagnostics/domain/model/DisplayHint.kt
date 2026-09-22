package io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model

/** Declared output inventory, never decode/encode or physical HDMI evidence. */
data class DisplayModeHint(val id: Int, val width: Int, val height: Int, val refreshMilliHz: Int)

data class DisplayHint(
    val id: Int,
    val defaultDisplay: Boolean,
    val presentation: Boolean,
    val width: Int,
    val height: Int,
    val refreshMilliHz: Int,
    val activeModeId: Int = 0,
    val modes: List<DisplayModeHint> = emptyList(),
)
