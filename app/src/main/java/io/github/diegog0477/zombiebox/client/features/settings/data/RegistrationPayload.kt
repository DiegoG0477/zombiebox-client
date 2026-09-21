package io.github.diegog0477.zombiebox.client.features.settings.data

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

class RegistrationPayload(private val context: Context) {
    fun create(id: String): JSONObject {
        val display = context.resources.displayMetrics
        val touch = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        val memory = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return JSONObject()
            .put(
                "clientVersion",
                context.packageManager.getPackageInfo(context.packageName, 0).versionName,
            )
            .put("protocolVersion", 1)
            .put("installationId", id)
            .put(
                "platform",
                JSONObject()
                    .put("androidApi", Build.VERSION.SDK_INT)
                    .put("release", Build.VERSION.RELEASE)
                    .put("manufacturer", Build.MANUFACTURER)
                    .put("model", Build.MODEL)
                    .put("abis", JSONArray().put(Build.CPU_ABI)),
            )
            .put(
                "display",
                JSONObject()
                    .put("width", display.widthPixels)
                    .put("height", display.heightPixels)
                    .put("dpi", display.densityDpi)
                    .put("touch", touch)
                    .put(
                        "dpad",
                        context.resources.configuration.navigation ==
                            Configuration.NAVIGATION_DPAD || !touch,
                    ),
            )
            .put(
                "memory",
                JSONObject()
                    .put("memoryClassMb", memory.memoryClass)
                    .put(
                        "physicalMb",
                        io.github.diegog0477.zombiebox.client.features.diagnostics.platform
                            .HardwareMemory
                            .physicalMb(),
                    ),
            )
    }
}
