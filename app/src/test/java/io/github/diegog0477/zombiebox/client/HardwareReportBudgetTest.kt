package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class HardwareReportBudgetTest {
    private fun report() =
        HardwareReport(
            "f",
            "product",
            "device",
            emptyList(),
            4,
            1024,
            100,
            0,
            false,
            false,
            "WIFI",
            listOf(
                CodecHint(
                    "decoder",
                    listOf("video/hevc"),
                    listOf("hevc-2160-main"),
                    listOf(CodecProfileHint("video/hevc", 1, 1024)),
                )
            ),
            emptyList(),
            encoders = listOf(CodecHint("encoder", listOf("video/avc"))),
        )

    @Test
    fun optionalInventoryIsDroppedBeforePlaybackProbeCandidates() {
        val original = report()
        val result =
            HardwareReportBudget.fit(original) { if (it.encoders.isNotEmpty()) 100000 else 1000 }
        assertTrue(result.inventoryLimited)
        assertTrue(result.encoders.isEmpty())
        assertTrue(result.decoders.first().profiles.isEmpty())
        assertEquals(listOf("hevc-2160-main"), result.decoders.first().probeCandidates)
        assertEquals(1, original.encoders.size)
    }

    @Test
    fun boundedInventoryIsUnchangedAndOversizeMandatoryDataFails() {
        val original = report()
        assertEquals(original, HardwareReportBudget.fit(original) { 1000 })
        try {
            HardwareReportBudget.fit(original) { 100000 }
            fail("unbounded report")
        } catch (_: IllegalArgumentException) {}
    }
}
