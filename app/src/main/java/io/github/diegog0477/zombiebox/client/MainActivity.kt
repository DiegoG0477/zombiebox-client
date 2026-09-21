package io.github.diegog0477.zombiebox.client

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import io.github.diegog0477.zombiebox.client.core.data.GatewayEvents
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.presentation.ScreenTasks
import io.github.diegog0477.zombiebox.client.core.ui.RemoteFocus
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
import io.github.diegog0477.zombiebox.client.features.artwork.data.GatewayArtworkRepository
import io.github.diegog0477.zombiebox.client.features.artwork.presentation.viewmodel.ArtworkViewModel
import io.github.diegog0477.zombiebox.client.features.browser.presentation.ui.BrowserActivity
import io.github.diegog0477.zombiebox.client.features.catalog.data.GatewayCatalogRepository
import io.github.diegog0477.zombiebox.client.features.catalog.presentation.ui.CatalogDialogs
import io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel.CatalogViewModel
import io.github.diegog0477.zombiebox.client.features.diagnostics.data.GatewayDiagnosticsRepository
import io.github.diegog0477.zombiebox.client.features.diagnostics.platform.HardwareScanner
import io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.ui.DiagnosticsDialog
import io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.viewmodel.DiagnosticsViewModel
import io.github.diegog0477.zombiebox.client.features.home.data.GatewayHomeRepository
import io.github.diegog0477.zombiebox.client.features.home.domain.model.HomeScope
import io.github.diegog0477.zombiebox.client.features.home.presentation.ui.HomeActions
import io.github.diegog0477.zombiebox.client.features.home.presentation.ui.HomeView
import io.github.diegog0477.zombiebox.client.features.home.presentation.viewmodel.HomeViewModel
import io.github.diegog0477.zombiebox.client.features.mirroring.data.GatewayReceiverRepository
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.PlaybackContext
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.ReceiverChange
import io.github.diegog0477.zombiebox.client.features.mirroring.domain.model.ReceiverPlan
import io.github.diegog0477.zombiebox.client.features.mirroring.presentation.ui.MediaReceiverDialog
import io.github.diegog0477.zombiebox.client.features.mirroring.presentation.viewmodel.ReceiverViewModel
import io.github.diegog0477.zombiebox.client.features.playback.data.GatewayPlaybackRepository
import io.github.diegog0477.zombiebox.client.features.playback.data.GatewayTracksRepository
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackPlan
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackProgress
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.PlaybackSession
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.QueueCursor
import io.github.diegog0477.zombiebox.client.features.playback.platform.AudioFocusController
import io.github.diegog0477.zombiebox.client.features.playback.platform.PlaybackConnection
import io.github.diegog0477.zombiebox.client.features.playback.platform.PlaybackNotifications
import io.github.diegog0477.zombiebox.client.features.playback.presentation.ui.PlaybackFailureDialog
import io.github.diegog0477.zombiebox.client.features.playback.presentation.ui.TracksDialog
import io.github.diegog0477.zombiebox.client.features.playback.presentation.ui.VideoSurface
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.PlaybackViewModel
import io.github.diegog0477.zombiebox.client.features.playback.presentation.viewmodel.TracksViewModel
import io.github.diegog0477.zombiebox.client.features.settings.data.GatewaySettingsRepository
import io.github.diegog0477.zombiebox.client.features.settings.presentation.ui.SettingsActions
import io.github.diegog0477.zombiebox.client.features.settings.presentation.ui.SettingsDialogs
import io.github.diegog0477.zombiebox.client.features.settings.presentation.viewmodel.SettingsViewModel
import io.github.diegog0477.zombiebox.client.features.youtubereceiver.data.GatewayYouTubeReceiverRepository
import io.github.diegog0477.zombiebox.client.features.youtubereceiver.domain.model.YouTubeCommand
import io.github.diegog0477.zombiebox.client.features.youtubereceiver.presentation.viewmodel.YouTubeReceiverViewModel
import io.github.diegog0477.zombiebox.shared.GatewayApi
import io.github.diegog0477.zombiebox.shared.GatewayFailure
import java.util.Locale
import java.util.concurrent.Executors

/** Native semantic Home. The gateway supplies content; all layout stays on-device. */
@Suppress("DEPRECATION")
class MainActivity : Activity() {
    private val ui by lazy { TvWidgets(this) { contextAccent() } }

    private fun render() {
        artwork.reset()
        imageWorker.queue.clear()
        content.render(snapshot, homeViewModel.state.scope, isTV(), bottom, full)
    }

    private val settingsModel by lazy {
        SettingsViewModel(GatewaySettingsRepository(applicationContext, api), screenTasks())
    }
    private val catalogModel by lazy {
        CatalogViewModel(GatewayCatalogRepository(api), screenTasks())
    }
    private val tracksModel by lazy {
        TracksViewModel(
            GatewayTracksRepository(api),
            { work -> worker.execute { work() } },
            { work -> handler.post { work() } },
        )
    }
    private lateinit var subtitleText: TextView
    private var timelineOffset = 0
    private var playbackSeekable = true
    private val seekButtons = ArrayList<Button>()
    private lateinit var playerControls: LinearLayout

    private val playbackModel by lazy {
        PlaybackViewModel(
            GatewayPlaybackRepository(api),
            { work -> worker.execute { work() } },
            { work -> handler.post { work() } },
        )
    }
    private val events by lazy { GatewayEvents(api, poller) { work -> handler.post { work() } } }

    private fun screenTasks() =
        ScreenTasks({ work -> worker.execute { work() } }, { work -> handler.post { work() } })

    private val settingsDialogs by lazy {
        SettingsDialogs(
            this,
            settingsModel,
            { api.token.isNotEmpty() },
            { api.base },
            ::error,
            SettingsActions(
                { profile ->
                    stopPlayback()
                    settingsModel.activate(profile)
                    receiverViewModel.reset()
                    homeViewModel.reset()
                    refresh()
                    receiverViewModel.refresh()
                },
                { refresh() },
                ::render,
                ::diagnostics,
                ::audioSettings,
                ::receiverSettings,
                ::youtubeReceiverSettings,
                ::mediaReceiverSettings,
            ),
        )
    }

    private fun settings() = settingsDialogs.show()

    private fun pairing() = settingsDialogs.pairing()

    private fun providerList() = settingsDialogs.providers()

    private val catalogDialogs by lazy {
        CatalogDialogs(
            this,
            catalogModel,
            { homeViewModel.state.scope.query },
            { refresh(query = it) },
            { startPlayback(it) },
            ::error,
        )
    }

    private fun search() = catalogDialogs.search()

    private fun catalogPage(provider: String) = catalogDialogs.page(provider)

    private fun details(item: MediaItem) = catalogDialogs.details(item)

    private val worker = Executors.newSingleThreadExecutor()
    private val receiverWorker = Executors.newSingleThreadExecutor()
    private lateinit var youtubeReceiver: YouTubeReceiverViewModel
    private var diagnosticsModel: DiagnosticsViewModel? = null
    private var youtubeDialog: AlertDialog? = null
    private val youtubeTick =
        object : Runnable {
            override fun run() {
                if (!closed) {
                    if (foreground && ::youtubeReceiver.isInitialized) youtubeReceiver.tick()
                    handler.postDelayed(this, 1000)
                }
            }
        }
    private val receiverTick =
        object : Runnable {
            override fun run() {
                if (!closed) {
                    if (foreground && api.token.isNotEmpty() && ::receiverViewModel.isInitialized)
                        receiverViewModel.refresh()
                    handler.postDelayed(this, 3000)
                }
            }
        }
    private val poller = Executors.newSingleThreadExecutor()
    private val api = GatewayApi()
    private val handler = Handler()
    private val imageWorker =
        java.util.concurrent.ThreadPoolExecutor(
            2,
            2,
            0L,
            java.util.concurrent.TimeUnit.MILLISECONDS,
            java.util.concurrent.ArrayBlockingQueue<Runnable>(24),
        )
    private val artwork =
        ArtworkViewModel(
            GatewayArtworkRepository(api),
            { work -> imageWorker.execute { work() } },
            { work -> handler.post { work() } },
        )
    private val prefs by lazy { getSharedPreferences("zombie", MODE_PRIVATE) }
    private lateinit var root: FrameLayout
    private lateinit var content: HomeView
    private lateinit var now: TextView
    private lateinit var playerLayer: LinearLayout
    private lateinit var playerStatus: TextView
    private lateinit var videoSurface: VideoSurface
    private lateinit var player: PlaybackConnection
    private lateinit var nextButton: Button
    private var restoreFullscreen = true
    private var queueFailed = false
    private lateinit var homeViewModel: HomeViewModel
    private val snapshot
        get() = homeViewModel.state.snapshot

    private lateinit var receiverViewModel: ReceiverViewModel
    private var currentItem: MediaItem? = null
    private var session = ""
    private var stream = ""
    private var mime = "video/mp4"
    private var itemTitle = ""
    private val playbackFailure by lazy { PlaybackFailureDialog(this) }
    private var playbackPending = false
    private var playbackLive = false
    private var receiverState = ""
    private lateinit var receiverInfo: TextView
    private var full = false
    private val homeFocus
        get() = content.focus

    private val playerFocus = RemoteFocus()
    private lateinit var bottom: LinearLayout
    private lateinit var audioController: AudioFocusController
    private var gamepadClick: View? = null
    private var lastReport = 0L
    private var lastState = ""
    private var lastPosition = 0
    private var lastDuration = 0
    @Volatile private var closed = false
    @Volatile private var foreground = false
    private val green
        get() = resources.getColor(R.color.accent_zombie)

    private val background = Color.rgb(10, 15, 16)
    private val panel = Color.rgb(24, 31, 33)
    private val muted = Color.rgb(167, 180, 186)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        restoreFullscreen =
            if (state?.containsKey("playbackFullscreen") == true)
                state.getBoolean("playbackFullscreen")
            else true
        val language = prefs.getString("language", "en") ?: "en"
        val config = Configuration(resources.configuration)
        config.locale = Locale(language)
        resources.updateConfiguration(config, resources.displayMetrics)
        api.base = prefs.getString("gateway", "") ?: ""
        api.device = prefs.getString("device", "") ?: ""
        api.token = prefs.getString("token", "") ?: ""
        root = FrameLayout(this)
        root.setBackgroundColor(background)
        val shell = ui.column()
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
        val scroll = ScrollView(this)
        content =
            HomeView(
                this,
                artwork,
                HomeActions(
                    navigate = { provider, query -> refresh(provider, query) },
                    play = { item -> startPlayback(item) },
                    details = { item -> details(item) },
                    settings = { settings() },
                    search = { search() },
                    youtubeReceiver = { youtubeReceiverSettings() },
                    catalog = { provider -> catalogPage(provider) },
                    mirrorReceiver = { receiverSettings() },
                    providers = { providerList() },
                    pair = { pairing() },
                ),
            )
        content.setPadding(ui.dp(22), ui.dp(16), ui.dp(22), ui.dp(18))
        scroll.addView(content)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        bottom = ui.row()
        bottom.setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(4))
        bottom.setBackgroundColor(panel)
        now = ui.text(getString(R.string.nothing_playing), 16f)
        bottom.addView(now, LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(ui.button(R.string.play_pause) { togglePlayback() })
        bottom.addView(
            ui.button(R.string.expand) { if (session.isNotEmpty()) setFullscreen(!full) }
        )
        shell.addView(bottom)
        createPlayer()
        player =
            PlaybackConnection(
                this,
                ::restorePlayback,
                { width, height -> videoSurface.setVideoSize(width, height) },
            ) { status, position, duration ->
                if (playbackPending) return@PlaybackConnection
                if (::receiverViewModel.isInitialized && receiverViewModel.activeSession == session)
                    receiverViewModel.playbackState(status)
                lastPosition = position + timelineOffset
                lastDuration = if (duration > 0) duration + timelineOffset else 0
                subtitleText.text =
                    if (status == "PLAYING" || status == "PAUSED") tracksModel.textAt(lastPosition)
                    else ""
                subtitleText.visibility =
                    if (subtitleText.text.isEmpty()) View.GONE else View.VISIBLE
                if (
                    ::youtubeReceiver.isInitialized &&
                        (currentItem?.provider == "youtube" || status == "STOPPED")
                )
                    youtubeReceiver.playerState(status, position, duration)
                playerStatus.text =
                    getString(
                        R.string.player_status,
                        ui.localizedState(status),
                        ui.formatTime(lastPosition),
                        ui.formatTime(lastDuration),
                    )
                if (
                    session.isNotEmpty() &&
                        (!::receiverViewModel.isInitialized ||
                            receiverViewModel.activeSession.isEmpty())
                )
                    now.text = itemTitle
                if (status == "PLAYING")
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (
                    status == "FAILED" &&
                        lastState != "FAILED" &&
                        foreground &&
                        receiverViewModel.activeSession.isEmpty() &&
                        session.isNotEmpty()
                ) {
                    showPlaybackFailure()
                }
                lastState = status
            }
        audioController = player
        youtubeReceiver =
            YouTubeReceiverViewModel(
                GatewayYouTubeReceiverRepository(api),
                { work -> receiverWorker.execute { work() } },
                { work -> handler.post { work() } },
            )
        youtubeReceiver.command = { command -> receiveYouTube(command) }
        youtubeReceiver.observer = { youtubeDialog?.setMessage(youtubeReceiverMessage()) }
        handler.post(youtubeTick)
        val backgroundExecutor = worker
        val uiHandler = handler
        homeViewModel =
            HomeViewModel(
                GatewayHomeRepository(api),
                { work -> backgroundExecutor.execute { work() } },
                { done -> uiHandler.post { done() } },
            )
        receiverViewModel =
            ReceiverViewModel(
                GatewayReceiverRepository(api),
                { work -> backgroundExecutor.execute { work() } },
                { done -> uiHandler.post { done() } },
            )
        receiverViewModel.observer = { plan -> receiveCast(plan) }
        handler.post(receiverTick)
        homeViewModel.observer = { state ->
            if (!closed) content.selectScope(state.scope)
            if (!closed && !state.loading) {
                render()
                state.failure?.let { error(it) }
            }
        }
        setContentView(root)
        render()
        if (api.token.isNotEmpty()) refresh() else handler.post { pairing() }
        events.start { changed ->
            if (!closed && foreground) {
                receiverViewModel.refresh()
                if (changed) refresh()
            }
        }
    }

    private fun refresh(
        provider: String = homeViewModel.state.scope.provider,
        query: String = homeViewModel.state.scope.query,
    ) {
        if (provider == "rebrowser") {
            startActivity(Intent(this, BrowserActivity::class.java))
            return
        }
        if (api.token.isNotEmpty()) homeViewModel.refresh(HomeScope(provider, query))
    }

    private fun error(e: Exception) {
        val message =
            if (e is GatewayFailure)
                getString(
                    when (e.status) {
                        401 -> R.string.error_pairing
                        403 -> R.string.error_admin
                        409 -> R.string.error_conflict
                        429 -> R.string.error_busy
                        else -> R.string.error_request
                    }
                )
            else getString(R.string.error_network)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun audioSettings() {
        AlertDialog.Builder(this)
            .setTitle(R.string.audio_focus_backend)
            .setSingleChoiceItems(
                arrayOf(
                    getString(R.string.backend_auto),
                    getString(R.string.backend_compatibility),
                ),
                if (prefs.getBoolean("audioFocusCompatibility", false)) 1 else 0,
            ) { dialog, index ->
                player.pause()
                audioController.release()
                prefs.edit().putBoolean("audioFocusCompatibility", index == 1).commit()
                player.refreshFocus()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun diagnostics() {
        val report =
            getString(
                R.string.diagnostics_report,
                Build.VERSION.SDK_INT,
                Build.MANUFACTURER,
                Build.MODEL,
                Build.CPU_ABI,
            )
        val model =
            io.github.diegog0477.zombiebox.client.features.diagnostics.presentation.viewmodel
                .DiagnosticsViewModel(
                    GatewayDiagnosticsRepository(api, HardwareScanner(applicationContext)),
                    { work -> worker.execute { work() } },
                    { work -> handler.post { work() } },
                )
        diagnosticsModel?.close()
        diagnosticsModel = model
        DiagnosticsDialog(this, model).show()
    }

    private fun youtubeReceiverMessage(): String {
        val value = youtubeReceiver.receiver
        return when {
            youtubeReceiver.failed -> getString(R.string.unavailable)
            value == null -> getString(R.string.youtube_receiver_detail)
            value.code.isEmpty() -> getString(R.string.loading)
            else -> getString(R.string.youtube_tv_code, value.code)
        }
    }

    private fun youtubeReceiverSettings() {
        if (api.token.isEmpty()) {
            pairing()
            return
        }
        val dialog =
            AlertDialog.Builder(this)
                .setTitle(R.string.youtube_receiver)
                .setMessage(youtubeReceiverMessage())
                .setPositiveButton(R.string.enable) { _, _ ->
                    youtubeReceiver.open()
                    youtubeReceiverSettings()
                }
                .setNeutralButton(R.string.disable) { _, _ -> youtubeReceiver.disable() }
                .setNegativeButton(R.string.close, null)
                .create()
        youtubeDialog = dialog
        dialog.setOnDismissListener { if (youtubeDialog === dialog) youtubeDialog = null }
        dialog.show()
    }

    private fun receiveYouTube(command: YouTubeCommand) {
        if (!foreground || receiverViewModel.activeSession.isNotEmpty()) {
            youtubeReceiver.complete(false, command.id)
            return
        }
        if (session.isEmpty() && command.action !in listOf("play", "stop")) {
            youtubeReceiver.complete(false, command.id)
            return
        }
        when (command.action) {
            "play" ->
                startPlayback(
                    MediaItem(command.itemId, "youtube", getString(R.string.youtube)),
                    remote = command,
                )
            "pause" -> player.pause()
            "resume" ->
                if (audioController.acquire()) player.resume()
                else youtubeReceiver.complete(false, command.id)
            "stop" -> stopPlayback()
            "seek" ->
                if (playbackSeekable) player.seekTo(command.positionMs)
                else youtubeReceiver.complete(false, command.id)
            "volume" ->
                player.volume(command.volume, command.muted) { ok ->
                    youtubeReceiver.volumeApplied(command.id, command.volume, command.muted, ok)
                }
            else -> youtubeReceiver.complete(false, command.id)
        }
    }

    private fun receiverSettings() {
        if (api.token.isEmpty()) {
            pairing()
            return
        }
        receiverViewModel.readEnabled(
            { enabled ->
                AlertDialog.Builder(this)
                    .setTitle(R.string.receive_cast)
                    .setMessage(R.string.receive_cast_detail)
                    .setPositiveButton(if (enabled) R.string.disable else R.string.enable) { _, _ ->
                        receiverViewModel.setEnabled(!enabled, ::error)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            },
            ::error,
        )
    }

    private fun mediaReceiverSettings() {
        if (api.token.isEmpty()) {
            pairing()
            return
        }
        MediaReceiverDialog(this, receiverViewModel, ::error).show()
    }

    private fun updateReceiver(plan: ReceiverPlan) {
        currentItem = plan.item
        receiverState = plan.state
        itemTitle = plan.item?.title ?: getString(R.string.screen_mirroring)
        now.text =
            listOf(itemTitle, plan.item?.subtitle ?: "")
                .filter { it.isNotEmpty() }
                .joinToString(" — ")
        receiverInfo.text =
            listOf(itemTitle, plan.item?.subtitle ?: "", ui.localizedState(plan.state))
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        receiverInfo.visibility = if (plan.fullscreen) View.GONE else View.VISIBLE
    }

    private fun receiveCast(plan: ReceiverPlan?) {
        if (!foreground || !player.ready) return
        when (
            val change =
                receiverViewModel.transition(
                    plan,
                    PlaybackContext(currentItem, full, lastState == "PLAYING"),
                )
        ) {
            is ReceiverChange.Restore -> {
                stopPlayback(keepReceiver = true)
                change.previous?.let { previous ->
                    previous.item?.let { startPlayback(it, previous.fullscreen, previous.playing) }
                }
            }
            is ReceiverChange.Update -> {
                updateReceiver(change.plan)
            }
            is ReceiverChange.Reconnect -> {
                updateReceiver(change.plan)
                if (audioController.acquire())
                    player.play(stream, 0, video = change.plan.fullscreen, seekable = false)
            }
            is ReceiverChange.Begin -> {
                youtubeReceiver.disable()
                stopPlayback(keepReceiver = true)
                session = change.plan.sessionId
                stream = api.base + change.plan.path
                mime = change.plan.mime
                updateReceiver(change.plan)
                player.configure(api.base, api.device, api.token)
                player.adopt(
                    PlaybackPlan(
                        session,
                        stream,
                        mime,
                        "DIRECT_PLAY",
                        0,
                        live = true,
                        seekable = false,
                    ),
                    currentItem ?: MediaItem(session, "cast", getString(R.string.screen_mirroring)),
                    emptyList(),
                    incoming = true,
                )
                lastReport = 0
                lastState = ""
                setSeekable(false)
                setFullscreen(change.plan.fullscreen)
                if (audioController.acquire())
                    player.play(stream, 0, video = change.plan.fullscreen, seekable = false)
            }
            null -> Unit
        }
    }

    private fun isTV(): Boolean =
        when (prefs.getString("mode", "AUTO")) {
            "TV",
            "DOCKED" -> true
            "HANDHELD" -> false
            else -> !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        }

    private fun createPlayer() {
        playerLayer = ui.column()
        playerLayer.setBackgroundColor(Color.BLACK)
        playerLayer.visibility = View.GONE
        val surface = VideoSurface(this)
        videoSurface = surface
        surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    player.surface(holder)
                }

                override fun surfaceChanged(
                    holder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    player.surface(null)
                }
            }
        )
        val viewport = FrameLayout(this)
        viewport.addView(surface, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        receiverInfo =
            ui.text("", 22f).apply {
                gravity = Gravity.CENTER
                visibility = View.GONE
                setBackgroundColor(panel)
                setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16))
            }
        viewport.addView(receiverInfo, FrameLayout.LayoutParams(-1, -1))
        subtitleText =
            ui.text("", 22f).apply {
                gravity = Gravity.CENTER
                setShadowLayer(3f, 1f, 1f, Color.BLACK)
                setBackgroundColor(Color.argb(170, 0, 0, 0))
                visibility = View.GONE
            }
        viewport.addView(
            subtitleText,
            FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
                setMargins(ui.dp(24), 0, ui.dp(24), ui.dp(18))
            },
        )
        playerLayer.addView(viewport, LinearLayout.LayoutParams(-1, 0, 1f))
        playerStatus = ui.text("", 12f, muted)
        playerLayer.addView(playerStatus)
        val controls = ui.row()
        playerControls = controls
        playerLayer.addView(HorizontalScrollView(this).apply { addView(controls) })
        seekButtons.clear()
        val back = ui.button(R.string.seek_back) { seek(-10000) }
        val forward = ui.button(R.string.seek_forward) { seek(10000) }
        seekButtons.add(back)
        seekButtons.add(forward)
        controls.addView(back)
        controls.addView(ui.button(R.string.play_pause) { togglePlayback() })
        controls.addView(forward)
        controls.addView(ui.button(R.string.audio_tracks) { showTracks("audio") })
        controls.addView(ui.button(R.string.subtitles) { showTracks("subtitle") })
        controls.addView(ui.button(R.string.minimize) { setFullscreen(!full) })
        nextButton = ui.button(R.string.next_item) { player.next() }
        nextButton.isEnabled = false
        controls.addView(nextButton)
        controls.addView(ui.button(R.string.external_player) { external() })
        controls.addView(ui.button(R.string.stop) { stopPlayback() })
        playerFocus.rebuild(listOf(Pair("player", controls)), false)
        root.addView(playerLayer, FrameLayout.LayoutParams(-1, -1))
    }

    private fun setSeekable(value: Boolean) {
        playbackSeekable = value
        seekButtons.forEach { it.isEnabled = value }
        if (::playerControls.isInitialized)
            playerFocus.rebuild(listOf(Pair("player", playerControls)), false)
    }

    private fun seek(delta: Int) {
        if (playbackSeekable) player.seek(delta)
    }

    private fun adoptPlan(plan: PlaybackPlan) {
        playbackModel.adopt(plan)
        playbackLive = plan.live
        session = plan.sessionId
        stream = plan.url
        mime = plan.mime
        timelineOffset = plan.timelineOffsetMs
        setSeekable(plan.seekable && !plan.live)
    }

    private fun retainPlan(plan: PlaybackPlan) {
        currentItem?.let { player.adopt(plan, it) }
    }

    private fun restorePlayback(state: PlaybackSession) {
        if (playbackPending) return
        nextButton.isEnabled = state.canNext
        if (state.error && !queueFailed)
            Toast.makeText(this, R.string.next_failed, Toast.LENGTH_LONG).show()
        queueFailed = state.error
        val plan = state.plan
        if (plan == null) {
            if (session.isNotEmpty()) {
                session = ""
                stopPlayback(endSession = false)
            }
            return
        }
        if (session != plan.sessionId) {
            val wasPlaying = session.isNotEmpty()
            adoptPlan(plan)
            currentItem = state.item
            itemTitle = state.item?.title ?: getString(R.string.screen_mirroring)
            now.text = itemTitle
            tracksModel.attach(session)
            state.subtitleId?.let { tracksModel.subtitles(it, {}, ::error) }
            if (state.incoming) {
                val receiver =
                    ReceiverPlan(
                        session,
                        stream.removePrefix(api.base),
                        mime,
                        state.item,
                        state.item?.kind != "audio",
                    )
                receiverViewModel.restore(receiver)
                updateReceiver(receiver)
            }
            lastState = ""
            lastPosition = state.progress.positionMs
            lastDuration = state.progress.durationMs
            setFullscreen(if (wasPlaying) full else restoreFullscreen)
        }
    }

    private fun showTracks(kind: String) {
        val requestedSession = session
        tracksModel.inventory(
            { inventory ->
                TracksDialog(this).show(inventory, kind, tracksModel.subtitleId) { id ->
                    if (requestedSession == session) {
                        if (kind == "subtitle")
                            tracksModel.subtitles(
                                id,
                                {
                                    player.subtitle(tracksModel.subtitleId)
                                    subtitleText.text = tracksModel.textAt(lastPosition)
                                    subtitleText.visibility =
                                        if (subtitleText.text.isEmpty()) View.GONE else View.VISIBLE
                                },
                                ::error,
                            )
                        else if (id != null) {
                            val paused = lastState != "PLAYING"
                            tracksModel.audio(
                                id,
                                lastPosition,
                                { plan ->
                                    adoptPlan(plan)
                                    retainPlan(plan)
                                    lastReport = 0
                                    player.play(
                                        stream,
                                        plan.resumePositionMs,
                                        !paused,
                                        currentItem?.kind != "audio",
                                        playbackSeekable,
                                    )
                                },
                                ::error,
                            )
                        }
                    }
                }
            },
            ::error,
        )
    }

    private fun setPlaying(playing: Boolean) {
        if (currentItem?.provider == "spotify" && receiverViewModel.activeSession == session) {
            receiverViewModel.command(if (playing) "resume" else "pause", ::error)
        } else if (playing) {
            if (audioController.acquire()) player.resume()
        } else player.pause()
    }

    private fun togglePlayback() {
        if (currentItem?.provider == "spotify" && receiverViewModel.activeSession == session) {
            receiverViewModel.command(if (receiverState == "PAUSED") "resume" else "pause", ::error)
            return
        }
        if (session.isNotEmpty() && foreground && audioController.acquire()) player.toggle()
    }

    private fun startPlayback(
        item: MediaItem,
        fullscreen: Boolean = true,
        autoplay: Boolean = true,
        remote: YouTubeCommand? = null,
    ) {
        if (remote == null) youtubeReceiver.disable()
        val catalog =
            catalogModel.screen?.takeIf { page -> page.page.items.any { it.id == item.id } }
        val queue =
            if (remote != null) listOf(item)
            else
                catalog?.page?.items
                    ?: snapshot.sections
                        .firstOrNull { section -> section.items.any { it.id == item.id } }
                        ?.items
                    ?: listOf(item)
        val cursor =
            catalog
                ?.takeIf { it.page.nextOffset > it.location.offset }
                ?.let {
                    QueueCursor(
                        it.location.provider,
                        it.location.parent,
                        it.location.query,
                        it.page.nextOffset,
                    )
                }
        stopPlayback()
        playbackPending = true
        playbackModel.start(
            item.id,
            if (remote != null) "AUTO" else prefs.getString("playbackMode", "AUTO") ?: "AUTO",
            { plan ->
                playbackPending = false
                if (!foreground || !player.ready) {
                    playbackModel.stop(plan.sessionId, null)
                    return@start
                }
                adoptPlan(plan)
                tracksModel.attach(session)
                currentItem = item
                player.configure(api.base, api.device, api.token)
                player.adopt(plan, item, queue, if (remote == null) cursor else null)
                itemTitle = item.title
                now.text = itemTitle
                lastState = ""
                lastReport = 0
                lastPosition = plan.resumePositionMs + plan.timelineOffsetMs
                lastDuration = 0
                setFullscreen(fullscreen)
                if (plan.mode == "EXTERNAL_PLAYER") {
                    if (remote != null) {
                        youtubeReceiver.complete(false, remote.id)
                        stopPlayback()
                    } else external()
                    return@start
                }
                if (audioController.acquire()) {
                    player.play(
                        stream,
                        remote?.positionMs ?: plan.resumePositionMs,
                        autoplay,
                        item.kind != "audio",
                        playbackSeekable,
                    )
                } else if (remote != null) youtubeReceiver.complete(false, remote.id)
            },
            failed = { failure ->
                playbackPending = false
                if (remote != null) youtubeReceiver.complete(false, remote.id)
                error(failure)
            },
        )
    }

    private fun showPlaybackFailure() {
        val expected = session
        playbackFailure.show(
            playbackModel.canRetry(if (playbackLive) 0 else lastPosition),
            { if (session == expected) retryPlayback() },
            { if (session == expected) replaceWithExternal() },
        )
    }

    private fun retryPlayback() {
        val item = currentItem ?: return
        val progress =
            PlaybackProgress("FAILED", if (playbackLive) 0 else lastPosition, lastDuration)
        playbackPending = true
        player.stop()
        playbackModel.retry(
            item.id,
            session,
            progress,
            { plan ->
                playbackPending = false
                adoptPlan(plan)
                retainPlan(plan)
                tracksModel.attach(session)
                lastPosition = plan.resumePositionMs + plan.timelineOffsetMs
                lastReport = 0
                lastState = ""
                if (audioController.acquire())
                    player.play(
                        stream,
                        plan.resumePositionMs,
                        video = item.kind != "audio",
                        seekable = playbackSeekable,
                    )
            },
            { failure ->
                playbackPending = false
                player.failed()
                error(failure)
                if (foreground) showPlaybackFailure()
            },
        )
    }

    private fun replaceWithExternal() {
        val item = currentItem ?: return
        val position = lastPosition
        playbackPending = true
        player.stop()
        playbackModel.stop(session, PlaybackProgress("FAILED", position, lastDuration))
        playbackModel.start(
            item.id,
            "EXTERNAL_PLAYER",
            { plan ->
                playbackPending = false
                adoptPlan(plan)
                retainPlan(plan)
                tracksModel.attach(session)
                if (foreground) external()
            },
            { failure ->
                playbackPending = false
                player.failed()
                error(failure)
            },
        )
    }

    private fun setFullscreen(value: Boolean) {
        full = value
        playerLayer.visibility = View.VISIBLE
        playerLayer.layoutParams =
            if (full) FrameLayout.LayoutParams(-1, -1)
            else
                FrameLayout.LayoutParams(ui.dp(320), ui.dp(230), Gravity.BOTTOM or Gravity.RIGHT)
                    .apply {
                        bottomMargin = ui.dp(58)
                        rightMargin = ui.dp(12)
                    }
        content.descendantFocusability =
            if (full) ViewGroup.FOCUS_BLOCK_DESCENDANTS else ViewGroup.FOCUS_AFTER_DESCENDANTS
        bottom.descendantFocusability = content.descendantFocusability
        playerLayer.bringToFront()
        if (full) playerFocus.restore() else homeFocus.restore()
    }

    private fun stopPlayback(keepReceiver: Boolean = false, endSession: Boolean = true) {
        playbackFailure.dismiss()
        playbackPending = false
        if (!keepReceiver && receiverViewModel.activeSession.isNotEmpty())
            receiverViewModel.dismiss(receiverViewModel.activeSession)
        currentItem = null
        receiverState = ""
        receiverInfo.visibility = View.GONE
        videoSurface.visibility = View.VISIBLE
        audioController.release()
        // Cancel pending Activity requests; the service owns session progress and revocation.
        playbackModel.stop("", PlaybackProgress("STOPPED", 0, 0))
        session = ""
        stream = ""
        tracksModel.attach("")
        subtitleText.text = ""
        subtitleText.visibility = View.GONE
        timelineOffset = 0
        full = false
        if (endSession) player.end()
        playerLayer.visibility = View.GONE
        now.setText(R.string.nothing_playing)
        content.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        bottom.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        homeFocus.restore()
    }

    private fun external() {
        if (stream.isEmpty()) return
        player.pause()
        audioController.release()
        try {
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(stream), mime))
        } catch (_: Exception) {
            Toast.makeText(this, R.string.no_external_player, Toast.LENGTH_LONG).show()
        }
    }

    private fun contextAccent(): Int =
        ui.providerAccent(
            if (::homeViewModel.isInitialized) homeViewModel.state.scope.provider else ""
        )

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = event.keyCode
        if (currentFocus !is EditText) {
            if (
                event.action == KeyEvent.ACTION_DOWN &&
                    (if (full) playerFocus else homeFocus).move(key, currentFocus)
            )
                return true
            // Preserve native CENTER/ENTER activation; gamepad A follows the same key-up contract.
            if (key == KeyEvent.KEYCODE_BUTTON_A) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0)
                    gamepadClick = currentFocus
                if (event.action == KeyEvent.ACTION_UP) {
                    if (!event.isCanceled && gamepadClick === currentFocus)
                        gamepadClick?.performClick()
                    gamepadClick = null
                }
                return true
            }
            if (key == KeyEvent.KEYCODE_BUTTON_B) {
                if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onBackPressed()
                return true
            }
            if (
                session.isNotEmpty() &&
                    key in
                        intArrayOf(
                            KeyEvent.KEYCODE_MEDIA_PLAY,
                            KeyEvent.KEYCODE_MEDIA_PAUSE,
                            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                            KeyEvent.KEYCODE_HEADSETHOOK,
                            KeyEvent.KEYCODE_MEDIA_STOP,
                            KeyEvent.KEYCODE_MEDIA_NEXT,
                            KeyEvent.KEYCODE_MEDIA_REWIND,
                            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                        )
            ) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0)
                    when (key) {
                        KeyEvent.KEYCODE_MEDIA_PLAY -> setPlaying(true)
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> setPlaying(false)
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_HEADSETHOOK -> togglePlayback()
                        KeyEvent.KEYCODE_MEDIA_STOP -> stopPlayback()
                        KeyEvent.KEYCODE_MEDIA_NEXT -> player.next()
                        KeyEvent.KEYCODE_MEDIA_REWIND -> seek(-10000)
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> seek(10000)
                    }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (full) setFullscreen(false)
        else if (session.isNotEmpty()) stopPlayback() else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        PlaybackNotifications.requestPermission(this)
        foreground = true
        if (::player.isInitialized) {
            if (session.isNotEmpty() && !audioController.acquire()) player.pause()
            player.foreground(true)
        }
        if (::receiverViewModel.isInitialized && api.token.isNotEmpty())
            receiverViewModel.resumeForeground()
        if (
            !playbackPending &&
                lastState == "FAILED" &&
                session.isNotEmpty() &&
                receiverViewModel.activeSession.isEmpty()
        )
            showPlaybackFailure()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("playbackFullscreen", full)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        foreground = false
        youtubeReceiver.disable()
        playbackFailure.dismiss()
        player.foreground(false)
        super.onPause()
    }

    override fun onDestroy() {
        playbackFailure.dismiss()
        closed = true
        events.close()
        settingsModel.close()
        catalogModel.close()
        playbackModel.close()
        tracksModel.close()
        diagnosticsModel?.close()
        youtubeReceiver.close()
        receiverWorker.shutdown()
        artwork.close()
        imageWorker.shutdownNow()
        receiverViewModel.close()
        homeViewModel.close()
        foreground = false
        handler.removeCallbacks(youtubeTick)
        handler.removeCallbacks(receiverTick)
        player.close()
        // Let closed ViewModels revoke plans already queued for UI delivery.
        worker.execute {
            handler.post {
                worker.execute { api.close() }
                worker.shutdown()
            }
        }
        poller.shutdownNow()
        super.onDestroy()
    }
}
