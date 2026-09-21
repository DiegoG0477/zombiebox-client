package io.github.diegog0477.zombiebox.client.data

import io.github.diegog0477.zombiebox.client.model.*
import io.github.diegog0477.zombiebox.shared.GatewayApi

class GatewayReceiverRepository(private val api:GatewayApi):ReceiverRepository {
    override fun active():ReceiverPlan? {
        val plan=api.request("GET","/v1/cast/active").optJSONObject("plan") ?: return null
        val path=plan.getString("url")
        require(path.startsWith("/v1/streams/") && !path.contains("\\") && !path.contains("#"))
        return ReceiverPlan(plan.getString("sessionId"),path,plan.getString("mimeType"))
    }
    override fun stop(sessionId:String){api.request("DELETE","/v1/playback/$sessionId")}
    override fun enabled()=api.request("GET","/v1/device/preferences").optBoolean("allowCasting")
    override fun setEnabled(enabled:Boolean) {
        val preferences=api.request("GET","/v1/device/preferences").put("allowCasting",enabled)
        api.request("PUT","/v1/device/preferences",preferences)
    }
}
