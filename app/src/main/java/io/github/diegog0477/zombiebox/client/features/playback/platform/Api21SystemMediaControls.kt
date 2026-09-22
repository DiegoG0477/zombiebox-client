package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.annotation.TargetApi
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.SystemPlayback
import io.github.diegog0477.zombiebox.client.features.playback.domain.repository.SystemMediaControls

/** Framework media buttons/transport controls; no media source or capture permission. */
@TargetApi(21)
class Api21SystemMediaControls(
    context: Context,
    private val command: (String, Long) -> Unit,
    private val publishToken: (Any?) -> Unit,
) : SystemMediaControls {
    private val session = MediaSession(context, "ZombieBoxPlayback")
    private var current = SystemPlayback()
    private var metadataKey = ""
    private var closed = false

    init {
        try {
            session.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            session.setCallback(
                object : MediaSession.Callback() {
                    override fun onPlay() = send("play")

                    override fun onPause() = send("pause")

                    override fun onStop() = send("stop")

                    override fun onSkipToNext() = send("next")

                    override fun onSeekTo(pos: Long) = send("seek", pos)
                },
                Handler(Looper.getMainLooper()),
            )
            publishToken(session.sessionToken)
        } catch (error: Throwable) {
            try {
                session.release()
            } catch (_: Exception) {}
            throw error
        }
    }

    private fun send(action: String, position: Long = 0) {
        if (!closed && current.allows(action)) command(action, position)
    }

    override fun update(state: SystemPlayback) {
        if (closed) return
        current = state
        val metadata = state.title + "\u0000" + state.subtitle + "\u0000" + state.durationMs
        if (metadata != metadataKey) {
            session.setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, state.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, state.subtitle)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, state.durationMs)
                    .build()
            )
            metadataKey = metadata
        }
        var actions = PlaybackState.ACTION_STOP
        if (state.canPause)
            actions =
                actions or
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE
        if (state.canNext) actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
        if (state.canSeek) actions = actions or PlaybackState.ACTION_SEEK_TO
        val status =
            when (state.status) {
                "PLAYING" -> PlaybackState.STATE_PLAYING
                "PAUSED" -> PlaybackState.STATE_PAUSED
                "BUFFERING" -> PlaybackState.STATE_BUFFERING
                "FAILED" -> PlaybackState.STATE_ERROR
                else -> PlaybackState.STATE_STOPPED
            }
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(actions)
                .setState(
                    status,
                    state.positionMs,
                    if (status == PlaybackState.STATE_PLAYING) 1f else 0f,
                    SystemClock.elapsedRealtime(),
                )
                .build()
        )
        session.isActive = state.active
    }

    override fun close() {
        if (closed) return
        closed = true
        publishToken(null)
        try {
            session.setCallback(null)
            session.isActive = false
        } finally {
            session.release()
        }
    }
}
