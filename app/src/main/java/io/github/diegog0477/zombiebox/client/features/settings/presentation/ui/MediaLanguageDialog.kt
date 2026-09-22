package io.github.diegog0477.zombiebox.client.features.settings.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
import io.github.diegog0477.zombiebox.client.features.settings.domain.model.MediaPreferences
import io.github.diegog0477.zombiebox.client.features.settings.presentation.viewmodel.SettingsViewModel

class MediaLanguageDialog(
    private val activity: Activity,
    private val model: SettingsViewModel,
    private val failed: (Exception) -> Unit,
) {
    fun show(value: MediaPreferences): AlertDialog {
        val ui = TvWidgets(activity)
        val content = ui.column()
        content.setPadding(ui.dp(16), ui.dp(8), ui.dp(16), ui.dp(8))
        fun field(label: Int, languages: List<String>): EditText {
            content.addView(ui.text(activity.getString(label), 14f, ui.muted))
            val text =
                EditText(activity).apply {
                    setSingleLine(true)
                    setText(languages.joinToString(", "))
                }
            content.addView(text)
            return text
        }
        content.addView(ui.text(activity.getString(R.string.media_language_help), 14f, ui.muted))
        val audio = field(R.string.audio_language_order, value.audioLanguages)
        val subtitles = field(R.string.subtitle_language_order, value.subtitleLanguages)
        val modes = listOf("off", "forced", "auto", "always")
        val labels =
            listOf(
                    R.string.subtitle_mode_off,
                    R.string.subtitle_mode_forced,
                    R.string.subtitle_mode_auto,
                    R.string.subtitle_mode_always,
                )
                .map { activity.getString(it) }
        val mode = Spinner(activity)
        mode.adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, labels)
        mode.setSelection(modes.indexOf(value.subtitleMode).coerceAtLeast(0))
        content.addView(mode)
        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(R.string.media_languages)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                fun languages(text: EditText) =
                    text.text
                        .toString()
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                val selected =
                    MediaPreferences(
                        languages(audio),
                        languages(subtitles),
                        modes[mode.selectedItemPosition],
                    )
                if (!selected.valid()) {
                    audio.error = activity.getString(R.string.media_language_invalid)
                    return@setOnClickListener
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                model.saveMediaPreferences(
                    selected,
                    { dialog.dismiss() },
                    { error ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        failed(error)
                    },
                )
            }
        }
        dialog.show()
        return dialog
    }
}
