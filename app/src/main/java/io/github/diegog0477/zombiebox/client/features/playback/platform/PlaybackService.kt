package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.PowerManager
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.features.catalog.data.GatewayCatalogRepository
import io.github.diegog0477.zombiebox.client.features.mirroring.data.GatewayReceiverRepository
import io.github.diegog0477.zombiebox.client.features.playback.data.GatewayPlaybackRepository
import io.github.diegog0477.zombiebox.client.features.playback.data.LocalPlaybackResumeRepository
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.*
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.PlaybackSessionViewModel
import io.github.diegog0477.zombiebox.shared.GatewayApi
import java.util.concurrent.Executors

/** Started while visible, then foreground for the lifetime of a playback session. */
class PlaybackService : Service() {
    inner class Access : Binder() {
        val service
            get() = this@PlaybackService
    }

    private val handler = Handler()
    private val worker = Executors.newSingleThreadExecutor()
    private val api = GatewayApi()
    lateinit var player: EmbeddedPlayer
        private set

    lateinit var model: PlaybackSessionViewModel
        private set

    lateinit var focus: AudioFocusController
        private set

    private lateinit var wake: PowerManager.WakeLock
    private lateinit var notifications: PlaybackNotification
    private var notificationKey = ""
    private var closed = false
    private var refreshing = false
    private var receiverAttempts = 0
    private var receiverRetryAt = 0L
    private var receiverSession = ""
    private var receiverPlaybackState = ""
    private var receiverHealthySince = 0L
    var visible = false
        private set

    var listener: ((PlaybackSession) -> Unit)? = null
    var sizeListener: ((Int, Int) -> Unit)? = null
    private var width = 0
    private var height = 0
    private val receiverPoll =
        object : Runnable {
            override fun run() {
                if (closed) return
                model.recoveryTick()
                val current = model.state
                val plan = current.plan
                if (
                    !visible &&
                        !refreshing &&
                        current.incoming &&
                        plan != null &&
                        current.item?.provider in listOf("spotify", "airplay")
                ) {
                    refreshing = true
                    worker.execute {
                        try {
                            val received = GatewayReceiverRepository(api).active()
                            handler.post {
                                if (
                                    !closed &&
                                        !visible &&
                                        model.state.plan?.sessionId == plan.sessionId
                                ) {
                                    if (received == null) {
                                        if (!model.restoreInterrupted()) model.stop()
                                    } else if (received.sessionId != plan.sessionId) {
                                        received.item?.let { item ->
                                            val replacement =
                                                PlaybackPlan(
                                                    received.sessionId,
                                                    api.base + received.path,
                                                    received.mime,
                                                    "DIRECT_PLAY",
                                                    0,
                                                    live = true,
                                                    seekable = false,
                                                )
                                            model.adopt(
                                                replacement,
                                                item,
                                                emptyList(),
                                                incoming = true,
                                            )
                                            playPlan(replacement, item)
                                        }
                                    } else {
                                        receiverStatus(received.item, received.state)
                                        if (receiverSession != plan.sessionId) {
                                            receiverSession = plan.sessionId
                                            receiverAttempts = 0
                                            receiverRetryAt = 0
                                            receiverHealthySince = 0
                                        }
                                        val now = android.os.SystemClock.elapsedRealtime()
                                        if (model.state.progress.state == "PLAYING") {
                                            if (receiverHealthySince == 0L)
                                                receiverHealthySince = now
                                            if (now - receiverHealthySince >= 60000)
                                                receiverAttempts = 0
                                        } else receiverHealthySince = 0
                                        if (
                                            model.state.progress.state in
                                                listOf("FAILED", "ENDED") &&
                                                received.state == "PLAYING" &&
                                                receiverAttempts < 3 &&
                                                now >= receiverRetryAt
                                        ) {
                                            receiverAttempts++
                                            receiverRetryAt = now + (2000L shl receiverAttempts)
                                            received.item?.let { item ->
                                                model.mediaState("BUFFERING", 0, 0)
                                                playPlan(plan, item)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            /* Unknown worker state preserves the confirmed session. */
                        } finally {
                            handler.post { refreshing = false }
                        }
                    }
                }
                handler.postDelayed(this, 3000)
            }
        }

    override fun onCreate() {
        super.onCreate()
        notifications = PlaybackNotifications.create()
        wake =
            (getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "zombiebox:playback")
                .apply { setReferenceCounted(false) }
        player =
            EmbeddedPlayer({ w, h ->
                width = w
                height = h
                sizeListener?.invoke(w, h)
            }) { status, position, duration ->
                model.mediaState(status, position, duration)
            }
        refreshFocus()
        model =
            PlaybackSessionViewModel(
                GatewayPlaybackRepository(api),
                GatewayCatalogRepository(api),
                { id -> GatewayReceiverRepository(api).stop(id) },
                { work -> worker.execute { work() } },
                { work -> handler.post { work() } },
                resumeRepository = LocalPlaybackResumeRepository(applicationContext, api),
            )
        model.play = ::playPlan
        model.stopPlayer = { player.stop() }
        model.observer = { state ->
            updateNotification(state)
            listener?.invoke(state)
        }
        player.background(true)
        handler.post(receiverPoll)
    }

    fun configure(base: String, device: String, token: String) {
        val preferences = getSharedPreferences("zombie", MODE_PRIVATE)
        model.automaticRecovery =
            preferences.getBoolean("automaticRecovery", true) &&
                preferences.getString("playbackMode", "AUTO") == "AUTO"
        // Serialize profile changes after queued writes/revocation for the old gateway.
        worker.execute { api.configure(base, device, token) }
    }

    fun refreshFocus() {
        if (::focus.isInitialized) focus.release()
        val preferences = getSharedPreferences("strategy-health", MODE_PRIVATE)
        val identity =
            Build.FINGERPRINT +
                ":" +
                packageManager.getPackageInfo(packageName, 0).versionName +
                ":focus-1"
        if (preferences.getString("identity", "") != identity)
            preferences.edit().clear().putString("identity", identity).apply()
        focus =
            AudioFocusFactory.create(
                Build.VERSION.SDK_INT,
                getSystemService(AUDIO_SERVICE) as AudioManager,
                AudioManager.OnAudioFocusChangeListener {
                    if (it <= 0 && !model.setRecoveryPaused(true)) player.pause()
                },
                getSharedPreferences("zombie", MODE_PRIVATE)
                    .getBoolean("audioFocusCompatibility", false),
                {
                    StrategyHealth(
                        preferences.getInt("focusFailures", 0),
                        preferences.getLong("focusRetryAfter", 0),
                    )
                },
                { health ->
                    preferences
                        .edit()
                        .putInt("focusFailures", health.failures)
                        .putLong("focusRetryAfter", health.retryAfter)
                        .apply()
                },
            )
    }

    fun foreground(value: Boolean) {
        visible = value
        player.foreground(value)
    }

    fun attach(observer: (PlaybackSession) -> Unit, size: (Int, Int) -> Unit) {
        listener = observer
        sizeListener = size
        size(width, height)
        observer(model.state)
    }

    fun detach(observer: (PlaybackSession) -> Unit) {
        if (listener === observer) {
            listener = null
            sizeListener = null
            player.surface(null)
            foreground(false)
        }
    }

    fun adopt(
        plan: PlaybackPlan,
        item: MediaItem,
        queue: List<MediaItem>?,
        cursor: QueueCursor?,
        incoming: Boolean,
    ) {
        model.adopt(plan, item, queue, cursor, incoming)
    }

    private fun playPlan(plan: PlaybackPlan, item: MediaItem) {
        if (plan.mode == "EXTERNAL_PLAYER") {
            model.mediaState("FAILED", plan.resumePositionMs, 0)
            return
        }
        if (focus.acquire())
            player.play(
                plan.url,
                plan.resumePositionMs,
                autoplay = model.state.progress.state != "PAUSED",
                video = item.kind != "audio",
                seekable = plan.seekable && !plan.live,
            )
        else model.mediaState("FAILED", plan.resumePositionMs, 0)
    }

    fun discard(plan: PlaybackPlan, incoming: Boolean) {
        worker.execute {
            try {
                if (incoming) GatewayReceiverRepository(api).stop(plan.sessionId)
                else GatewayPlaybackRepository(api).stop(plan.sessionId)
            } catch (_: Exception) {}
        }
    }

    fun receiverStatus(item: MediaItem?, status: String) {
        receiverPlaybackState = status
        item?.let { model.metadata(it) }
        updateNotification(model.state)
    }

    fun toggle() {
        if (model.setRecoveryPaused(model.state.progress.state != "PAUSED")) return
        if (model.state.incoming && model.state.item?.provider == "spotify") {
            val action = if (receiverPlaybackState == "PAUSED") "resume" else "pause"
            worker.execute {
                try {
                    GatewayReceiverRepository(api).command(action)
                } catch (_: Exception) {}
            }
        } else if (focus.acquire()) player.toggle()
    }

    private fun updateNotification(state: PlaybackSession) {
        if (state.plan == null) {
            if (state.loading) return
            notificationKey = ""
            if (wake.isHeld) wake.release()
            focus.release()
            stopForeground(true)
            stopSelf()
            return
        }
        val remotePaused =
            state.incoming && state.item?.provider == "spotify" && receiverPlaybackState == "PAUSED"
        val active =
            !remotePaused &&
                (state.progress.state == "PLAYING" || state.progress.state == "BUFFERING")
        if (active && !wake.isHeld) wake.acquire(6 * 60 * 60 * 1000L)
        if (!active && wake.isHeld) wake.release()
        val key = "${state.item?.title}:$active"
        if (key != notificationKey) {
            notificationKey = key
            startForeground(
                1001,
                notifications.build(this, state.item?.title ?: "Zombie Box", active),
            )
        }
    }

    override fun onBind(intent: Intent): IBinder = Access()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "stop" -> model.stop()
            "next" -> model.next()
            "toggle" -> toggle()
        }
        if (model.state.plan == null && !model.state.loading) stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        closed = true
        handler.removeCallbacks(receiverPoll)
        listener = null
        sizeListener = null
        model.close()
        player.close()
        focus.release()
        if (wake.isHeld) wake.release()
        // Drain bounded progress/revocation work before closing the transport.
        worker.execute {
            handler.post {
                worker.execute { api.close() }
                worker.shutdown()
            }
        }
        super.onDestroy()
    }
}
