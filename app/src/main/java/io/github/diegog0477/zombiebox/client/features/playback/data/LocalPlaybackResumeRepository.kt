package io.github.diegog0477.zombiebox.client.features.playback.data

import android.content.Context
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackBookmark
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.QueueCursor
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.PlaybackResumeRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/** One bounded checkpoint for the active pairing; credentials are hashed, never persisted here. */
class LocalPlaybackResumeRepository(context: Context, private val api: GatewayApi) :
    PlaybackResumeRepository {
    private val preferences =
        context.applicationContext.getSharedPreferences("playback-resume", Context.MODE_PRIVATE)

    private fun scope(): String =
        MessageDigest.getInstance("SHA-256")
            .digest((api.base + "\n" + api.device + "\n" + api.token).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }

    override fun load(): PlaybackBookmark? {
        if (api.token.isEmpty() || preferences.getString("scope", "") != scope()) return null
        val encoded = preferences.getString("bookmark", null) ?: return null
        if (encoded.length > 256 * 1024) return null
        return try {
            val value = JSONObject(encoded)
            val age = System.currentTimeMillis() - value.getLong("savedAt")
            if (value.getInt("version") != 1 || age !in 0..604800000L) return null
            val queue = value.getJSONArray("queue")
            if (queue.length() > 200) return null
            val cursor =
                value.optJSONObject("cursor")?.let {
                    val provider = bounded(it, "provider", 64)
                    val parent = bounded(it, "parent", 200)
                    val query = bounded(it, "query", 256)
                    val offset = it.getInt("offset")
                    require(offset in 0..20000)
                    QueueCursor(provider, parent, query, offset)
                }
            val position = value.getInt("positionMs")
            require(position in 0..604800000)
            PlaybackBookmark(
                item(value.getJSONObject("item")),
                position,
                (0 until queue.length()).map { item(queue.getJSONObject(it)) },
                cursor,
                if (value.has("subtitleId")) value.getInt("subtitleId").takeIf { it >= 0 } else null,
            )
        } catch (_: Exception) {
            null
        }
    }

    override fun save(value: PlaybackBookmark) {
        if (api.token.isEmpty()) return
        fun encode(item: MediaItem) =
            JSONObject()
                .put("id", item.id.take(200))
                .put("provider", item.provider.take(64))
                .put("title", item.title.take(256))
                .put("kind", item.kind.take(40))
        val encoded =
            JSONObject()
                .put("version", 1)
                .put("savedAt", System.currentTimeMillis())
                .put("item", encode(value.item))
                .put("positionMs", value.positionMs.coerceIn(0, 604800000))
        val queue = JSONArray()
        value.queue.take(200).forEach { queue.put(encode(it)) }
        encoded.put("queue", queue)
        value.subtitleId?.let { encoded.put("subtitleId", it) }
        value.cursor?.let {
            encoded.put(
                "cursor",
                JSONObject()
                    .put("provider", it.provider.take(64))
                    .put("parent", it.parent.take(200))
                    .put("query", it.query.take(256))
                    .put("offset", it.offset),
            )
        }
        val text = encoded.toString()
        if (text.length <= 256 * 1024)
            preferences
                .edit()
                .clear()
                .putString("scope", scope())
                .putString("bookmark", text)
                .commit()
    }

    override fun clear() {
        preferences.edit().clear().commit()
    }

    private fun bounded(value: JSONObject, name: String, max: Int): String =
        value.getString(name).also { require(it.length <= max) }

    private fun item(value: JSONObject): MediaItem =
        MediaItem(
            bounded(value, "id", 200).also { require(it.isNotEmpty()) },
            bounded(value, "provider", 64),
            bounded(value, "title", 256),
            kind = bounded(value, "kind", 40),
        )
}
