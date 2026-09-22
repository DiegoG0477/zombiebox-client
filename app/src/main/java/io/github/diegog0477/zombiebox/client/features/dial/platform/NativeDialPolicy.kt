package io.github.diegog0477.zombiebox.client.features.dial.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import io.github.diegog0477.zombiebox.client.features.dial.presentation.ui.NativeDialActivity

/** A local opt-in gates the exported entry point on every device, without OEM guesses. */
object NativeDialPolicy {
    fun enabled(context: Context): Boolean =
        context.packageManager.getComponentEnabledSetting(
            ComponentName(context, NativeDialActivity::class.java)
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun enable(context: Context, value: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, NativeDialActivity::class.java),
            if (value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
