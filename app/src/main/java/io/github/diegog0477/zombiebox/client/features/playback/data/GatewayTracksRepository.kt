package io.github.diegog0477.zombiebox.client.features.playback.data

import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.TracksRepository
import io.github.diegog0477.zombiebox.shared.GatewayApi
import org.json.JSONObject

class GatewayTracksRepository(private val api: GatewayApi) : TracksRepository {
    override fun inventory(session: String): MediaTracks {
        val value = api.request("GET", "/v1/playback/$session/tracks")
        val tracks = value.getJSONArray("tracks")
        return MediaTracks(
            value.getBoolean("available"),
            (0 until tracks.length()).map {
                val track = tracks.getJSONObject(it)
                MediaTrack(
                    track.getInt("id"),
                    track.getString("kind"),
                    track.optString("language"),
                    track.optString("title"),
                    track.optBoolean("default"),
                    track.optBoolean("forced"),
                    track.getBoolean("selectable"),
                )
            },
            if (value.has("audioId")) value.getInt("audioId") else null,
        )
    }

    override fun subtitles(session: String, track: Int): List<SubtitleCue> {
        val cues = api.request("GET", "/v1/playback/$session/subtitles/$track").getJSONArray("cues")
        return (0 until cues.length()).map {
            val cue = cues.getJSONObject(it)
            SubtitleCue(cue.getInt("startMs"), cue.getInt("endMs"), cue.getString("text"))
        }
    }

    override fun audio(session: String, track: Int, positionMs: Int): PlaybackPlan =
        PlaybackPlanDecoder.decode(
            api.base,
            api.request(
                "POST",
                "/v1/playback/$session/audio",
                JSONObject().put("audioId", track).put("positionMs", positionMs),
            ),
        )

    override fun release(session: String) {
        api.request("DELETE", "/v1/playback/$session")
    }
}
