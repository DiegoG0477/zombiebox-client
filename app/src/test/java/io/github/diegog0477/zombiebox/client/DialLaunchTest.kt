package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.client.features.dial.domain.model.DialLaunch
import org.junit.Assert.*
import org.junit.Test

class DialLaunchTest {
    @Test
    fun nativeLaunchAcceptsOnlyHomeWithoutTrustingParameters() {
        assertTrue(DialLaunch.accepts(DialLaunch.ACTION, null))
        assertTrue(DialLaunch.accepts(DialLaunch.ACTION, "screen=home"))
        for (payload in
            listOf(
                "https://media.example/movie",
                "token=secret",
                "screen=home&play=1",
                "x".repeat(65536),
            )) assertFalse(DialLaunch.accepts(DialLaunch.ACTION, payload))
        assertFalse(DialLaunch.accepts("android.intent.action.MAIN", ""))
    }
}
