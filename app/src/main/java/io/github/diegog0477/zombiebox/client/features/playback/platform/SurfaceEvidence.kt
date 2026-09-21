package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.content.Context
import android.os.Build
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth

class SurfaceEvidence(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences("surface-health", Context.MODE_PRIVATE)

    init {
        val identity =
            Build.FINGERPRINT +
                "|" +
                context.packageManager.getPackageInfo(context.packageName, 0).versionName +
                "|surface-1"
        if (prefs.getString("identity", "") != identity)
            prefs.edit().clear().putString("identity", identity).commit()
    }

    fun probe(id: String): String = prefs.getString(id, "UNKNOWN") ?: "UNKNOWN"

    fun record(id: String, status: String) {
        if (id in listOf("texture-output", "surface-reattach")) {
            val edit = prefs.edit().putString(id, status)
            if (id == "texture-output" && status == "PASS")
                edit.putInt("failures", 0).putLong("retryAfter", 0)
            edit.commit()
        }
    }

    fun health() = StrategyHealth(prefs.getInt("failures", 0), prefs.getLong("retryAfter", 0))

    fun failed() {
        val next = health().failed(System.currentTimeMillis())
        prefs
            .edit()
            .putInt("failures", next.failures)
            .putLong("retryAfter", next.retryAfter)
            .commit()
    }
}
