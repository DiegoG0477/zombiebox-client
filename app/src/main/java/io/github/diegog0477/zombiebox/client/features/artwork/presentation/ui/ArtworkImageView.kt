package io.github.diegog0477.zombiebox.client.features.artwork.presentation.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.ImageView
import io.github.diegog0477.zombiebox.client.features.artwork.presentation.viewmodel.ArtworkViewModel

/** Gateway-sized derivatives only; a replaced binding never displays an old response. */
class ArtworkImageView(context: Context) : ImageView(context) {
    private val budget =
        io.github.diegog0477.zombiebox.client.features.artwork.platform.ImageBudget.discover(
            context
        )
    private var request = 0
    private var bound = ""

    init {
        scaleType = ScaleType.CENTER_CROP
        isFocusable = false
    }

    fun bind(model: ArtworkViewModel, path: String, hero: Boolean = false) {
        if (bound == path && drawable != null) return
        bound = path
        val generation = ++request
        setImageDrawable(null)
        if (path.isEmpty()) return
        model.load(path, hero) { bytes ->
            if (request != generation) return@load
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth in 1..960 && bounds.outHeight in 1..540) {
                try {
                    setImageBitmap(
                        BitmapFactory.decodeByteArray(
                            bytes,
                            0,
                            bytes.size,
                            BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.RGB_565
                                val target = if (hero) budget.heroWidth else budget.cardWidth
                                while (bounds.outWidth / inSampleSize > target) inSampleSize *= 2
                            },
                        )
                    )
                } catch (_: OutOfMemoryError) {
                    setImageDrawable(null)
                }
            }
        }
    }
}
