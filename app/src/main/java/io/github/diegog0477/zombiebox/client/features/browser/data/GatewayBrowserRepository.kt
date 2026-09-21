package io.github.diegog0477.zombiebox.client.features.browser.data

import io.github.diegog0477.zombiebox.client.features.browser.domain.repository.BrowserRepository
import io.github.diegog0477.zombiebox.client.features.browser.domain.repository.BrowserSessionExpired
import io.github.diegog0477.zombiebox.shared.GatewayApi
import io.github.diegog0477.zombiebox.shared.GatewayFailure
import org.json.JSONObject

class GatewayBrowserRepository(private val api: GatewayApi) : BrowserRepository {
    override fun start(url: String): String {
        val id =
            api.request("POST", "/v1/browser", JSONObject().put("url", url)).getString("sessionId")
        require(id.matches(Regex("[a-f0-9]{32}")))
        return id
    }

    override fun frame(id: String): ByteArray = expired { api.frame("/v1/browser/$id/frame") }

    private fun <T> expired(work: () -> T): T =
        try {
            work()
        } catch (failure: GatewayFailure) {
            if (failure.status == 404 || failure.status == 410) throw BrowserSessionExpired()
            throw failure
        }

    override fun input(id: String, action: String, text: String, x: Int?, y: Int?) {
        expired {
            api.request(
                "POST",
                "/v1/browser/$id/input",
                JSONObject().put("action", action).put("text", text).apply {
                    if (x != null && y != null) {
                        put("x", x)
                        put("y", y)
                    }
                },
            )
        }
    }

    override fun stop(id: String) {
        api.request("DELETE", "/v1/browser/$id")
    }
}
