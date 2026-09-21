package io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.widget.*
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.viewmodel.DiagnosticsViewModel

class DiagnosticsDialog(private val activity: Activity, private val model: DiagnosticsViewModel) {
    fun show() {
        val report =
            activity.getString(
                R.string.diagnostics_report,
                Build.VERSION.SDK_INT,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.CPU_ABI,
            )
        val export =
            Button(activity).apply {
                setText(R.string.export_diagnostics)
                setOnClickListener {
                    model.export(
                        { text ->
                            AlertDialog.Builder(activity)
                                .setTitle(R.string.export_diagnostics)
                                .setMessage(text)
                                .setNegativeButton(R.string.close, null)
                                .setPositiveButton(R.string.share_report) { _, _ ->
                                    val intent =
                                        Intent(Intent.ACTION_SEND)
                                            .setType("text/plain")
                                            .putExtra(Intent.EXTRA_TEXT, text)
                                            .putExtra(
                                                Intent.EXTRA_SUBJECT,
                                                activity.getString(R.string.diagnostics),
                                            )
                                    if (intent.resolveActivity(activity.packageManager) != null) {
                                        activity.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                activity.getString(R.string.share_report),
                                            )
                                        )
                                    } else
                                        Toast.makeText(
                                                activity,
                                                R.string.share_unavailable,
                                                Toast.LENGTH_LONG,
                                            )
                                            .show()
                                }
                                .show()
                        },
                        {
                            Toast.makeText(activity, R.string.error_request, Toast.LENGTH_LONG)
                                .show()
                        },
                    )
                }
            }
        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(R.string.diagnostics)
                .setMessage(report)
                .setView(export)
                .setNeutralButton(R.string.run_probes) { _, _ ->
                    activity.startActivity(Intent(activity, ProbesActivity::class.java))
                }
                .setNegativeButton(R.string.rescan_hardware, null)
                .setPositiveButton(R.string.close, null)
                .create()
        model.observer = { hardware, failed ->
            dialog.setMessage(
                if (failed) activity.getString(R.string.error_request)
                else if (hardware == null) report
                else
                    activity.getString(
                        R.string.hardware_report,
                        hardware.abis.joinToString(", "),
                        hardware.cores,
                        hardware.memoryMb,
                        hardware.storageFreeMb,
                        hardware.network,
                        hardware.latencyMs,
                        hardware.decoders.size,
                        hardware.externalPlayers.size,
                    )
            )
        }
        dialog.setOnDismissListener { model.close() }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.setMessage(activity.getString(R.string.loading))
                model.scan()
            }
        }
        dialog.show()
    }
}
