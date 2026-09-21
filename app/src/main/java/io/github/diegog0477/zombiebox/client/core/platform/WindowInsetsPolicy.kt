package io.github.diegog0477.zombiebox.client.core.platform

import android.os.Build
import android.view.View

interface WindowInsetsPolicy {
    fun install(view: View)

    companion object {
        fun apply(view: View) {
            if (Build.VERSION.SDK_INT < 21) return
            try {
                val policy =
                    Class.forName(
                            "io.github.diegog0477.zombiebox.client.core.platform.ModernWindowInsetsPolicy"
                        )
                        .getDeclaredConstructor()
                        .newInstance() as WindowInsetsPolicy
                policy.install(view)
            } catch (_: Exception) {
                // Existing platform window fitting remains the legacy fallback.
            }
        }
    }
}
