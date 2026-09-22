package io.github.diegog0477.zombiebox.client.features.hdmi.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Handler
import android.widget.CheckBox
import android.widget.ScrollView
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
import io.github.diegog0477.zombiebox.client.features.hdmi.platform.FrameworkHdmiControl
import io.github.diegog0477.zombiebox.client.features.hdmi.presentation.viewmodel.HdmiViewModel
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth
import java.util.concurrent.Executors

/** Local-only control: one-touch play can change the television input and requires a click. */
object HdmiDialog {
    fun show(activity: Activity): AlertDialog {
        val handler = Handler()
        val worker = Executors.newSingleThreadExecutor()
        val prefs = activity.getSharedPreferences("hdmi-control", Activity.MODE_PRIVATE)
        val revision =
            Build.FINGERPRINT +
                ":" +
                activity.packageManager.getPackageInfo(activity.packageName, 0).versionName +
                ":cec-1"
        if (prefs.getString("revision", "") != revision)
            prefs
                .edit()
                .putString("revision", revision)
                .remove("failures")
                .remove("retryAfter")
                .apply()
        val model =
            HdmiViewModel(
                FrameworkHdmiControl(activity),
                { task -> worker.execute { task() } },
                { task -> handler.post { task() } },
                { delay, task -> handler.postDelayed({ task() }, delay) },
                { System.currentTimeMillis() },
                { StrategyHealth(prefs.getInt("failures", 0), prefs.getLong("retryAfter", 0)) },
                { health ->
                    prefs
                        .edit()
                        .putInt("failures", health.failures)
                        .putLong("retryAfter", health.retryAfter)
                        .apply()
                },
            )
        val ui = TvWidgets(activity)
        val form = ui.column().apply { setPadding(ui.dp(18), ui.dp(12), ui.dp(18), ui.dp(12)) }
        val enabled =
            CheckBox(activity).apply {
                setText(R.string.hdmi_native_enabled)
                isChecked = prefs.getBoolean("enabled", true)
            }
        form.addView(enabled)
        form.addView(ui.text(activity.getString(R.string.hdmi_explanation), 15f, ui.muted))
        val status = ui.text("", 16f)
        form.addView(status)
        val query = ui.button(R.string.hdmi_query) { model.query() }
        val activate = ui.primary(R.string.hdmi_activate) { model.activate() }
        form.addView(query)
        form.addView(activate)
        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(R.string.hdmi_control)
                .setView(ScrollView(activity).apply { addView(form) })
                .setNegativeButton(R.string.close, null)
                .create()
        model.observer = { state ->
            status.setText(
                when (state) {
                    "NEEDS_PRIVILEGE" -> R.string.hdmi_privilege
                    "MISSING_API",
                    "UNAVAILABLE" -> R.string.hdmi_unavailable
                    "QUERYING",
                    "ACTIVATING" -> R.string.hdmi_waiting
                    "ON" -> R.string.hdmi_on
                    "STANDBY" -> R.string.hdmi_standby
                    "TRANSITIONING" -> R.string.hdmi_transition
                    "SUCCESS" -> R.string.hdmi_success
                    "FAILED" -> R.string.hdmi_failed
                    "TIMEOUT" -> R.string.hdmi_timeout
                    "COOLDOWN" -> R.string.hdmi_cooldown
                    "DISABLED" -> R.string.disabled
                    else -> R.string.hdmi_unknown
                }
            )
            val busy = state == "QUERYING" || state == "ACTIVATING"
            query.isEnabled = enabled.isChecked && !busy
            activate.isEnabled =
                enabled.isChecked &&
                    state in
                        listOf("ON", "STANDBY", "TRANSITIONING", "SUCCESS", "FAILED", "TIMEOUT")
        }
        enabled.setOnCheckedChangeListener { _, value ->
            prefs.edit().putBoolean("enabled", value).apply()
            model.enabled(value)
            if (value) model.query()
        }
        dialog.setOnDismissListener {
            model.close()
            handler.removeCallbacksAndMessages(null)
            worker.shutdownNow()
        }
        dialog.show()
        model.enabled(enabled.isChecked)
        if (enabled.isChecked) model.query()
        return dialog
    }
}
