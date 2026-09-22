package io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.HardwareReport

class HardwareInventoryDialog(private val activity: Activity) {
    fun show(report: HardwareReport) {
        fun dp(value: Int) = (value * activity.resources.displayMetrics.density).toInt()
        val content =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(16), dp(24), dp(16))
            }
        fun label(text: String, heading: Boolean = false) {
            content.addView(
                TextView(activity).apply {
                    this.text = text
                    textSize = if (heading) 19f else 15f
                    setPadding(0, dp(12), 0, dp(12))
                    if (heading) setTextColor(activity.resources.getColor(R.color.accent_zombie))
                }
            )
        }
        label(activity.getString(R.string.inventory_disclaimer))
        if (report.inventoryLimited) label(activity.getString(R.string.inventory_limited))
        label(activity.getString(R.string.inventory_displays), true)
        if (report.displays.isEmpty()) label(activity.getString(R.string.inventory_unknown))
        for (display in report.displays) {
            label(
                activity.getString(
                    R.string.inventory_display,
                    display.id,
                    display.width,
                    display.height,
                    display.refreshMilliHz / 1000.0,
                    activity.getString(
                        if (display.activeModeId > 0) R.string.inventory_native_mode
                        else R.string.inventory_logical_mode
                    ),
                )
            )
            for (mode in display.modes) label(
                activity.getString(
                    R.string.inventory_mode,
                    mode.id,
                    mode.width,
                    mode.height,
                    mode.refreshMilliHz / 1000.0,
                )
            )
        }
        for ((title, codecs) in
            listOf(
                R.string.inventory_decoders to report.decoders,
                R.string.inventory_encoders to report.encoders,
            )) {
            label(activity.getString(title), true)
            if (codecs.isEmpty()) label(activity.getString(R.string.inventory_unknown))
            if (codecs.size > 16) label(activity.getString(R.string.inventory_preview))
            for (codec in codecs.take(16)) {
                val acceleration =
                    activity.getString(
                        when (codec.acceleration) {
                            "HARDWARE" -> R.string.inventory_hardware
                            "SOFTWARE" -> R.string.inventory_software
                            else -> R.string.inventory_unknown
                        }
                    )
                label(codec.name + "\n" + codec.types.joinToString(", ") + "\n" + acceleration)
                for (profile in codec.profiles) label(
                    activity.getString(
                        R.string.inventory_profile,
                        profile.mime,
                        profile.profile,
                        profile.level,
                    )
                )
            }
        }
        AlertDialog.Builder(activity)
            .setTitle(R.string.inventory_title)
            .setView(
                ScrollView(activity).apply {
                    addView(content)
                    isFocusable = true
                }
            )
            .setPositiveButton(R.string.close, null)
            .show()
    }
}
