package io.github.diegog0477.zombiebox.client.data

import io.github.diegog0477.zombiebox.shared.GatewayApi
import io.github.diegog0477.zombiebox.client.model.BrowserRepository
import org.json.JSONObject

class GatewayBrowserRepository(private val api: GatewayApi): BrowserRepository {
    override fun start(url: String): String {
        val id=api.request("POST","/v1/browser",JSONObject().put("url",url)).getString("sessionId")
        require(id.matches(Regex("[a-f0-9]{32}")));return id
    }
    override fun frame(id: String)=api.frame("/v1/browser/$id/frame")
    override fun input(id: String,action: String,text: String){api.request("POST","/v1/browser/$id/input",JSONObject().put("action",action).put("text",text))}
    override fun stop(id: String){api.request("DELETE","/v1/browser/$id")}
}
