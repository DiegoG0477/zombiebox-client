package io.github.diegog0477.zombiebox.client.features.playback.presentation.ui

import android.app.AlertDialog
import android.content.Context
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*

/** Platform dialog renders semantic tracks; it never performs transport work. */
class TracksDialog(private val context: Context) {
    fun show(value: MediaTracks, kind: String, subtitleId: Int?, select: (Int?) -> Unit) {
        val tracks = value.tracks.filter { it.kind == kind && it.selectable }
        val subtitles = kind == "subtitle"
        val labels = tracks.map { label(it) }.toMutableList()
        if (subtitles) labels.add(0, context.getString(R.string.subtitles_off))
        if (!value.available || tracks.isEmpty()) {
            AlertDialog.Builder(context)
                .setMessage(R.string.tracks_unavailable)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }
        val selected = if (subtitles) subtitleId else value.audioId
        val checked = tracks.indexOfFirst { it.id == selected } + if (subtitles) 1 else 0
        AlertDialog.Builder(context)
            .setTitle(if (subtitles) R.string.subtitles else R.string.audio_tracks)
            .setSingleChoiceItems(labels.toTypedArray(), checked) { dialog, index ->
                dialog.dismiss()
                select(
                    if (subtitles && index == 0) null
                    else tracks[index - if (subtitles) 1 else 0].id
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun label(track: MediaTrack): String {
        val title = track.title.ifEmpty { context.getString(R.string.track_number, track.id + 1) }
        val language = track.language.ifEmpty { context.getString(R.string.language_unknown) }
        val forced = if (track.forced) " · " + context.getString(R.string.track_forced) else ""
        return "$title · $language$forced"
    }
}
