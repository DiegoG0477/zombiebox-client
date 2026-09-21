package io.github.diegog0477.zombiebox.client.features.playback.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import io.github.diegog0477.zombiebox.client.R

/** The caller owns session identity; dismiss this prompt when selection changes. */
class PlaybackFailureDialog(private val activity: Activity) {
    private var dialog: AlertDialog? = null

    fun show(canRetry: Boolean, retry: () -> Unit, external: () -> Unit) {
        dismiss()
        val builder =
            AlertDialog.Builder(activity)
                .setTitle(R.string.playback_failed)
                .setMessage(R.string.playback_recovery_help)
                .setNegativeButton(R.string.close, null)
                .setNeutralButton(R.string.external_player) { _, _ -> external() }
        if (canRetry) builder.setPositiveButton(R.string.retry_compatible) { _, _ -> retry() }
        dialog = builder.create().also { it.show() }
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}
