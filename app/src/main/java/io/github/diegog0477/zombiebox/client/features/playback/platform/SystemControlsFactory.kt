package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.content.Context
import android.os.Build
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth
import io.github.diegog0477.zombiebox.client.features.playback.domain.policy.SystemControlCoordinator
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.SystemMediaControls

object SystemControlsFactory {
    fun create(
        context: Context,
        command: (String, Long) -> Unit,
        publishToken: (Any?) -> Unit,
    ): SystemControlCoordinator {
        val preferences =
            context.getSharedPreferences("system-controls-health", Context.MODE_PRIVATE)
        val identity =
            Build.FINGERPRINT +
                ":" +
                context.packageManager.getPackageInfo(context.packageName, 0).versionName +
                ":media-session-1"
        if (preferences.getString("identity", "") != identity)
            preferences.edit().clear().putString("identity", identity).apply()
        return SystemControlCoordinator(
            create = {
                Class.forName(
                        "io.github.diegog0477.zombiebox.client.features.playback.platform.Api21SystemMediaControls"
                    )
                    .getConstructor(
                        Context::class.java,
                        Function2::class.java,
                        Function1::class.java,
                    )
                    .newInstance(context, command, publishToken) as SystemMediaControls
            },
            load = {
                StrategyHealth(
                    preferences.getInt("failures", 0),
                    preferences.getLong("retryAfter", 0),
                )
            },
            save = {
                preferences
                    .edit()
                    .putInt("failures", it.failures)
                    .putLong("retryAfter", it.retryAfter)
                    .apply()
            },
            clock = { System.currentTimeMillis() },
        )
    }
}
