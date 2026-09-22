package io.github.diegog0477.zombiebox.client.features.hdmi.platform

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.github.diegog0477.zombiebox.client.features.hdmi.domain.repository.HdmiControl
import java.lang.reflect.Proxy

/** Optional AOSP SystemApi. Reflection does not bypass permission or hidden-API restrictions. */
class FrameworkHdmiControl(context: Context) : HdmiControl {
    private val context = context.applicationContext

    override fun availability(): String {
        if (Build.VERSION.SDK_INT < 21) return "MISSING_API"
        if (
            context.checkCallingOrSelfPermission("android.permission.HDMI_CEC") !=
                PackageManager.PERMISSION_GRANTED
        )
            return "NEEDS_PRIVILEGE"
        return try {
            if (client() == null) "UNAVAILABLE" else "AVAILABLE"
        } catch (_: Exception) {
            "UNAVAILABLE"
        } catch (_: LinkageError) {
            "UNAVAILABLE"
        }
    }

    private fun client(): Any? {
        val manager = context.getSystemService("hdmi_control") ?: return null
        return Class.forName("android.hardware.hdmi.HdmiControlManager")
            .getMethod("getPlaybackClient")
            .invoke(manager)
    }

    override fun query(result: (String) -> Unit) {
        invoke("queryDisplayStatus", "DisplayStatusCallback") { code ->
            result(
                when (code) {
                    0 -> "ON"
                    1 -> "STANDBY"
                    2,
                    3 -> "TRANSITIONING"
                    else -> "UNKNOWN"
                }
            )
        }
    }

    override fun activate(result: (String) -> Unit) {
        invoke("oneTouchPlay", "OneTouchPlayCallback") {
            result(if (it == 0) "SUCCESS" else "FAILED")
        }
    }

    private fun invoke(method: String, callback: String, result: (Int) -> Unit) {
        check(availability() == "AVAILABLE")
        val type = Class.forName("android.hardware.hdmi.HdmiPlaybackClient")
        val callbackType = Class.forName("android.hardware.hdmi.HdmiPlaybackClient\$$callback")
        val proxy =
            Proxy.newProxyInstance(callbackType.classLoader, arrayOf(callbackType)) {
                instance,
                called,
                args ->
                when (called.name) {
                    "onComplete" -> {
                        result((args?.get(0) as? Int) ?: -1)
                        null
                    }
                    "hashCode" -> System.identityHashCode(instance)
                    "equals" -> instance === args?.get(0)
                    "toString" -> "ZombieHdmiCallback"
                    else -> null
                }
            }
        type.getMethod(method, callbackType).invoke(client(), proxy)
    }
}
