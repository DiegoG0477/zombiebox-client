package io.github.diegog0477.zombiebox.client.features.artwork.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.concurrent.RejectedExecutionException

/** Activity-owned bounded bitmap reuse. Decoding never runs on the UI thread. */
class ArtworkDecoder(
    private val budget: ImageBudget,
    private val execute: (() -> Unit) -> Unit,
    private val deliver: (() -> Unit) -> Unit,
) {
    private data class Entry(val bitmap: Bitmap, val expires: Long, val hero: Boolean) {
        val bytes: Long
            get() = bitmap.rowBytes.toLong() * bitmap.height
    }

    private val cache = LinkedHashMap<String, Entry>(16, 0.75f, true)
    private var bytes = 0L
    private var generation = 0
    private var closed = false

    @Synchronized
    fun clear() {
        generation++
        cache.clear()
        bytes = 0
        // Views may still reference an evicted bitmap: never recycle it here.
    }

    @Synchronized
    fun close() {
        closed = true
        clear()
    }

    fun decode(encoded: ByteArray, hero: Boolean, done: (Bitmap?) -> Unit) {
        val ticket = synchronized(this) { if (closed) return else generation }
        try {
            execute {
                val target = if (hero) budget.heroWidth else budget.cardWidth
                val key =
                    target.toString() +
                        ":" +
                        hero +
                        ":" +
                        MessageDigest.getInstance("SHA-256").digest(encoded).joinToString("") {
                            "%02x".format(it.toInt() and 255)
                        }
                val cached =
                    synchronized(this) {
                        if (closed || ticket != generation) return@execute
                        val entry = cache[key]
                        if (entry != null && entry.expires > System.currentTimeMillis())
                            entry.bitmap
                        else {
                            if (entry != null) bytes -= cache.remove(key)!!.bytes
                            null
                        }
                    }
                val bitmap = cached ?: decodeBytes(encoded, target)
                synchronized(this) {
                    if (closed || ticket != generation) return@execute
                    if (bitmap != null && cached == null) {
                        val entries = cache.entries.iterator()
                        while (entries.hasNext()) {
                            val entry = entries.next().value
                            if (
                                (hero && entry.hero) || entry.expires <= System.currentTimeMillis()
                            ) {
                                bytes -= entry.bytes
                                entries.remove()
                            }
                        }
                        cache.remove(key)?.let { bytes -= it.bytes }
                        val entry = Entry(bitmap, System.currentTimeMillis() + 300000, hero)
                        cache[key] = entry
                        bytes += entry.bytes
                        val oldest = cache.entries.iterator()
                        while (bytes > budget.decodedBytes && oldest.hasNext()) {
                            bytes -= oldest.next().value.bytes
                            oldest.remove()
                        }
                    }
                }
                deliver {
                    val valid = synchronized(this) { !closed && ticket == generation }
                    if (valid) done(bitmap)
                }
            }
        } catch (_: RejectedExecutionException) {
            done(null)
        }
    }

    private fun decodeBytes(encoded: ByteArray, target: Int): Bitmap? {
        if (encoded.size > 256 * 1024) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(encoded, 0, encoded.size, bounds)
            if (bounds.outWidth !in 1..960 || bounds.outHeight !in 1..540) return null
            val options =
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > target) inSampleSize *= 2
                }
            BitmapFactory.decodeByteArray(encoded, 0, encoded.size, options)
        } catch (_: OutOfMemoryError) {
            synchronized(this) {
                cache.clear()
                bytes = 0
            }
            null
        }
    }
}
