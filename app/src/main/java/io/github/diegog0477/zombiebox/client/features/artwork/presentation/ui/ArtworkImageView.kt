package io.github.diegog0477.zombiebox.client.features.artwork.presentation.ui

import android.content.Context
import android.widget.ImageView
import io.github.diegog0477.zombiebox.client.features.artwork.presentation.viewmodel.ArtworkViewModel

/** Gateway-sized derivatives only; a replaced binding never displays an old response. */
class ArtworkImageView(
    context: Context,
    private val decoder:
        io.github.diegog0477.zombiebox.client.features.artwork.platform.ArtworkDecoder,
) : ImageView(context) {
    private var request = 0
    private var bound = ""

    init {
        scaleType = ScaleType.CENTER_CROP
        isFocusable = false
    }

    fun bind(model: ArtworkViewModel, path: String, hero: Boolean = false) {
        if (bound == "$hero:$path" && drawable != null) return
        bound = "$hero:$path"
        val generation = ++request
        setImageDrawable(null)
        if (path.isEmpty()) return
        model.load(path, hero) { bytes ->
            if (request != generation) return@load
            decoder.decode(bytes, hero) { bitmap ->
                if (request == generation) setImageBitmap(bitmap)
            }
        }
    }
}
