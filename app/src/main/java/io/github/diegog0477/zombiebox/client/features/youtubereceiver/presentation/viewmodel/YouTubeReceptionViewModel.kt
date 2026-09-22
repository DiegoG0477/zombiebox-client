package io.github.diegog0477.zombiebox.client.features.youtubereceiver.presentation.viewmodel

import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.PlaybackSessionViewModel
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.PlaybackViewModel
import io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model.YouTubeCommand
import io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model.YouTubeReception
import io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.repository.YouTubePlaybackControl

/** Service-owned receiver intent and playback coordination; screens only observe this state. */
class YouTubeReceptionViewModel(
    private val lease: YouTubeReceiverViewModel,
    private val resolver: PlaybackViewModel,
    private val session: PlaybackSessionViewModel,
    private val playback: YouTubePlaybackControl,
    private val title: String,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    var state = YouTubeReception()
        private set

    var observer: ((YouTubeReception) -> Unit)? = null
    private var closed = false
    private var attempts = 0
    private var retryAt = 0L

    init {
        lease.command = ::receive
        lease.observer = { publish() }
        // Expiry can mean another client took ownership. Never reclaim it automatically.
        lease.expired = { disable() }
    }

    private fun publish() {
        state = state.copy(receiver = lease.receiver, failed = lease.failed)
        observer?.invoke(state)
    }

    fun enable() {
        if (closed || (state.enabled && !state.failed)) return
        attempts = 0
        retryAt = 0
        state = state.copy(enabled = true)
        publish()
        tick()
    }

    fun tick() {
        if (closed || !state.enabled) return
        if (lease.receiver != null) lease.tick()
        else if (attempts < 3 && clock() >= retryAt) {
            attempts++
            retryAt = clock() + (5000L shl attempts)
            lease.open()
        }
    }

    /**
     * A different source is taking over; invalidate pending resolution without closing the lease.
     */
    fun standby() {
        resolver.stop("", null)
        lease.standby()
    }

    fun disable() {
        state = state.copy(enabled = false)
        resolver.stop("", null)
        lease.disable()
        endIncoming()
    }

    private fun ownsPlayback(): Boolean =
        session.state.incoming && session.state.item?.provider == "youtube"

    private fun endIncoming() {
        if (ownsPlayback() && !session.restoreInterrupted()) session.stop()
    }

    fun playbackState(status: String, position: Int, duration: Int) {
        if (state.enabled && ownsPlayback()) lease.playerState(status, position, duration)
    }

    private fun receive(command: YouTubeCommand) {
        if (!state.enabled || (!ownsPlayback() && command.action !in listOf("play", "stop"))) {
            lease.complete(false, command.id)
            return
        }
        when (command.action) {
            "play" -> start(command)
            "pause" -> playback.pause()
            "resume" -> if (!playback.resume()) lease.complete(false, command.id)
            "stop" -> {
                resolver.stop("", null)
                lease.playerState("STOPPED", 0, 0)
                endIncoming()
            }
            "seek" -> {
                val plan = session.state.plan
                if (
                    plan != null &&
                        plan.seekable &&
                        !plan.live &&
                        command.positionMs >= plan.timelineOffsetMs
                )
                    playback.seek(command.positionMs - plan.timelineOffsetMs)
                else lease.complete(false, command.id)
            }
            "volume" ->
                playback.volume(command.volume, command.muted) { ok ->
                    lease.volumeApplied(command.id, command.volume, command.muted, ok)
                }
            else -> lease.complete(false, command.id)
        }
    }

    private fun start(command: YouTubeCommand) {
        val receiver = lease.receiver ?: return
        val item = MediaItem(command.itemId, "youtube", title)
        resolver.start(
            item.id,
            "AUTO",
            done = { plan ->
                if (
                    !state.enabled || !lease.accepts(command.id) || plan.mode == "EXTERNAL_PLAYER"
                ) {
                    resolver.stop(plan.sessionId, null)
                    lease.complete(false, command.id)
                } else {
                    session.rememberInterruption()
                    session.stop(preserveInterrupted = true)
                    session.adopt(plan, item, emptyList(), incoming = true)
                    playback.play(plan, item)
                }
            },
            failed = { lease.complete(false, command.id) },
            positionMs = command.positionMs,
            receiverId = receiver.id,
        )
    }

    fun close() {
        closed = true
        state = state.copy(enabled = false)
        observer = null
        resolver.close()
        lease.close()
    }
}
