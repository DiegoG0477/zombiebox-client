package io.github.diegog0477.zombiebox.client.core.data

import io.github.diegog0477.zombiebox.shared.GatewayApi
import io.github.diegog0477.zombiebox.shared.GatewayFailure
import java.util.concurrent.ExecutorService

/** Long-poll transport lifecycle is separate from rendering and semantic screen state. */
class GatewayEvents(
    private val api: GatewayApi,
    private val executor: ExecutorService,
    private val deliver: (() -> Unit) -> Unit,
) {
    @Volatile private var closed = false

    fun start(companionChanged: () -> Unit = {}, changed: (Boolean) -> Unit) {
        executor.execute {
            var cursor = ""
            while (!closed && !Thread.currentThread().isInterrupted) {
                try {
                    if (api.token.isEmpty()) {
                        Thread.sleep(1000)
                        continue
                    }
                    val result = api.request("GET", "/v1/events?cursor=$cursor")
                    cursor = result.optString("cursor")
                    val events = result.optJSONArray("events")
                    val hasEvents =
                        events != null &&
                            (0 until events.length()).any {
                                events.optJSONObject(it)?.optString("type") != "companion.changed"
                            }
                    val companionEvent =
                        events != null &&
                            (0 until events.length()).any {
                                events.optJSONObject(it)?.optString("type") == "companion.changed"
                            }
                    if (companionEvent) deliver { if (!closed) companionChanged() }
                    if (events != null && events.length() > 0 && !hasEvents) continue
                    deliver { if (!closed) changed(hasEvents) }
                } catch (error: Exception) {
                    if (error is GatewayFailure && error.status == 409) cursor = ""
                    try {
                        Thread.sleep(3000)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }
    }

    fun close() {
        closed = true
        executor.shutdownNow()
    }
}
