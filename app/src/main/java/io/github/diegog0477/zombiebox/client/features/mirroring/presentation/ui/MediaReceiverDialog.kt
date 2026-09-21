package io.github.diegog0477.zombiebox.client.features.mirroring.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.features.mirroring.presentation.viewmodel.ReceiverViewModel

class MediaReceiverDialog(
    private val activity: Activity,
    private val model: ReceiverViewModel,
    private val error: (Exception) -> Unit,
) {
    fun show() {
        model.readMediaProvider(
            { provider ->
                val providers = listOf("", "spotify", "airplay", "auto")
                val labels =
                    arrayOf(
                        activity.getString(R.string.disabled),
                        activity.getString(R.string.spotify),
                        activity.getString(R.string.airplay),
                        activity.getString(R.string.media_receiver_auto),
                    )
                AlertDialog.Builder(activity)
                    .setTitle(R.string.media_receiver)
                    .setSingleChoiceItems(labels, providers.indexOf(provider).coerceAtLeast(0)) {
                        dialog,
                        index ->
                        model.selectMediaProvider(providers[index], error)
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.close, null)
                    .show()
            },
            error,
        )
    }
}
