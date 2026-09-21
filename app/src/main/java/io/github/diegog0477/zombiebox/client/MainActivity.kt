package io.github.diegog0477.zombiebox.client

import io.github.diegog0477.zombiebox.shared.GatewayApi
import io.github.diegog0477.zombiebox.shared.GatewayFailure
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.net.Uri
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import io.github.diegog0477.zombiebox.client.data.GatewayHomeRepository
import io.github.diegog0477.zombiebox.client.model.*
import io.github.diegog0477.zombiebox.client.presentation.HomeViewModel
import io.github.diegog0477.zombiebox.client.presentation.ReceiverViewModel
import io.github.diegog0477.zombiebox.client.data.GatewayReceiverRepository
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

/** Native semantic Home. The gateway supplies content; all layout stays on-device. */
@Suppress("DEPRECATION")
class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val poller = Executors.newSingleThreadExecutor()
    private val api = GatewayApi()
    private val handler = Handler()
    private val prefs by lazy { getSharedPreferences("zombie", MODE_PRIVATE) }
    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var now: TextView
    private lateinit var playerLayer: LinearLayout
    private lateinit var playerStatus: TextView
    private lateinit var videoSurface: VideoSurface
    private lateinit var player: EmbeddedPlayer
    private val audioFocus = AudioManager.OnAudioFocusChangeListener { if (it <= 0) player.pause() }
    private lateinit var homeViewModel: HomeViewModel
    private val snapshot get() = homeViewModel.state.snapshot
    private lateinit var receiverViewModel:ReceiverViewModel
    private var currentItem:MediaItem?=null
    private var session = ""
    private var stream = ""
    private var mime = "video/mp4"
    private var itemTitle = ""
    private var full = false
    private val homeFocus = RemoteFocus()
    private val playerFocus = RemoteFocus()
    private val focusRows = ArrayList<Pair<String, ViewGroup>>()
    private lateinit var bottom: LinearLayout
    private lateinit var audioController: AudioFocusController
    private var gamepadClick: View? = null
    private var lastReport = 0L
    private var lastState = ""
    private var lastPosition = 0
    private var lastDuration = 0
    private var playbackGeneration = 0
    @Volatile private var closed = false
    @Volatile private var foreground = false
    private val green = Color.rgb(76, 239, 105)
    private val background = Color.rgb(10, 15, 16)
    private val panel = Color.rgb(24, 31, 33)
    private val muted = Color.rgb(167, 180, 186)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val language = prefs.getString("language", "en") ?: "en"
        val config = Configuration(resources.configuration)
        config.locale = Locale(language)
        resources.updateConfiguration(config, resources.displayMetrics)
        api.base = prefs.getString("gateway", "") ?: ""
        api.device = prefs.getString("device", "") ?: ""
        api.token = prefs.getString("token", "") ?: ""
        root = FrameLayout(this)
        root.setBackgroundColor(background)
        val shell = column()
        root.addView(shell, FrameLayout.LayoutParams(-1, -1))
        val scroll = ScrollView(this)
        content = column()
        content.setPadding(dp(22), dp(16), dp(22), dp(18))
        scroll.addView(content)
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        bottom = row()
        bottom.setPadding(dp(16), dp(4), dp(16), dp(4))
        bottom.setBackgroundColor(panel)
        now = text(getString(R.string.nothing_playing), 16f)
        bottom.addView(now, LinearLayout.LayoutParams(0, -2, 1f))
        bottom.addView(button(R.string.play_pause) { togglePlayback() })
        bottom.addView(button(R.string.expand) { if (session.isNotEmpty()) setFullscreen(!full) })
        shell.addView(bottom)
        createPlayer()
        player = EmbeddedPlayer({ width, height -> videoSurface.setVideoSize(width, height) }) { status, position, duration ->
            lastPosition = position; lastDuration = duration
            playerStatus.text = getString(R.string.player_status, localizedState(status), formatTime(position), formatTime(duration))
            if (session.isNotEmpty()) now.text = itemTitle
            if (status == "PLAYING") window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val time = System.currentTimeMillis()
            if (session.isNotEmpty() && (status != lastState || time - lastReport > 10000)) {
                lastState = status; lastReport = time
                val current = session
                async({ api.request("PUT", "/v1/playback/$current/progress", JSONObject().put("state", status).put("positionMs", position).put("durationMs", duration)) }, {}, false)
            }
        }
        audioController = AudioFocusFactory.create(Build.VERSION.SDK_INT, getSystemService(AUDIO_SERVICE) as AudioManager, audioFocus, prefs.getBoolean("audioFocusCompatibility", false))
        val backgroundExecutor = worker
        val uiHandler = handler
        homeViewModel = HomeViewModel(GatewayHomeRepository(api), { work -> backgroundExecutor.execute { work() } }, { done -> uiHandler.post { done() } })
        receiverViewModel=ReceiverViewModel(GatewayReceiverRepository(api),{ work -> backgroundExecutor.execute { work() } },{ done -> uiHandler.post { done() } })
        receiverViewModel.observer={plan -> receiveCast(plan)}
        homeViewModel.observer = { state ->
            if (!closed && !state.loading) { render(); state.failure?.let { error(it) } }
        }
        setContentView(root)
        render()
        if (api.token.isNotEmpty()) refresh() else handler.post { pairing() }
        poller.execute {
            var cursor = ""
            while (!closed) {
                if (!foreground || api.token.isEmpty()) { try { Thread.sleep(1000) } catch (_: InterruptedException) { break }; continue }
                try {
                    val result = api.request("GET", "/v1/events?cursor=" + URLEncoder.encode(cursor, "UTF-8"))
                    cursor = result.optString("cursor")
                    runOnUiThread { if (!closed && foreground) { receiverViewModel.refresh(); if ((result.optJSONArray("events")?.length() ?: 0) > 0) refresh() } }
                } catch (e: Exception) {
                    if (e is GatewayFailure && e.status == 409) cursor = ""
                    try { Thread.sleep(3000) } catch (_: InterruptedException) { break }
                }
            }
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun text(value: String, size: Float, color: Int = Color.WHITE) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color); setPadding(dp(4), dp(4), dp(4), dp(4))
    }
    private fun box(color: Int, stroke: Int = Color.rgb(48, 60, 63)) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(8).toFloat(); setStroke(dp(1), stroke)
    }
    private fun focusBackground(): StateListDrawable = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), box(Color.rgb(25, 66, 40), green))
        addState(intArrayOf(android.R.attr.state_pressed), box(Color.rgb(25, 66, 40), green))
        addState(intArrayOf(), box(panel))
    }
    private fun action(label: String, click: () -> Unit) = Button(this).apply {
        text = label; textSize = 14f; setTextColor(Color.WHITE); isFocusable = true
        setPadding(dp(12), dp(7), dp(12), dp(7)); setBackgroundDrawable(focusBackground()); tag = "action:$label"
        setOnClickListener { click() }
        layoutParams = LinearLayout.LayoutParams(-2, dp(44)).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
    }
    private fun button(label: Int, click: () -> Unit) = action(getString(label), click).apply { tag = "button:$label" }
    private fun horizontal(parent: LinearLayout, id: String = ""): LinearLayout {
        val scroll = HorizontalScrollView(this); scroll.isHorizontalScrollBarEnabled = false
        val line = row(); scroll.addView(line); parent.addView(scroll)
        if (id.isNotEmpty()) focusRows.add(Pair(id, line))
        return line
    }
    private fun title(label: String) { content.addView(text(label, 18f).apply { setPadding(dp(4), dp(14), 0, dp(8)) }) }
    private fun render() {
        focusRows.clear()
        content.removeAllViews()
        val nav = horizontal(content, "navigation")
        nav.addView(text(getString(R.string.brand), 25f, green).apply { typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, dp(24), 0) })
        nav.addView(button(R.string.home) { refresh("", "") })
        for (id in arrayOf("youtube", "plex", "stremio", "spotify", "iptv", "airplay")) {
            nav.addView(action(serviceTitle(id)) { refresh(id, "") }.apply { tag = "nav:$id" })
        }
        nav.addView(button(R.string.search) { search() })
        nav.addView(button(R.string.settings) { settings() })
        val hero = column()
        hero.setPadding(dp(24), dp(18), dp(24), dp(18))
        hero.setBackgroundDrawable(GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.rgb(20, 39, 31), Color.rgb(25, 41, 44), background)).apply { cornerRadius = dp(10).toFloat() })
        val featured = snapshot.hero
        hero.addView(text(getString(R.string.tagline), 12f, green))
        hero.addView(text(featured?.title ?: getString(R.string.welcome), 32f).apply { typeface = Typeface.DEFAULT_BOLD; maxLines = 2 })
        hero.addView(text(featured?.description?.takeIf { it.isNotEmpty() } ?: getString(R.string.welcome_detail), 16f, muted).apply { maxLines = 2 })
        val heroActions = row()
        if (featured != null) {
            heroActions.addView(button(R.string.play) { startPlayback(featured) })
            heroActions.addView(button(R.string.more_info) { details(featured) })
        } else heroActions.addView(button(R.string.configure_services) { settings() })
        focusRows.add(Pair("hero", heroActions))
        hero.addView(heroActions)
        content.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(14), 0, dp(4)) })
        for (section in snapshot.sections) {
            title(if (section.id == "continue") getString(R.string.continue_watching) else serviceTitle(section.id))
            val line = horizontal(content, "section:" + section.id)
            for (item in section.items) {
                val card = column().apply { tag = "item:" + section.id + ":" + item.id }
                card.setPadding(dp(12), dp(12), dp(12), dp(12)); card.setBackgroundDrawable(focusBackground()); card.isFocusable = true; card.isClickable = true
                card.addView(text(serviceTitle(item.provider), 12f, green))
                card.addView(text(item.title, 19f).apply { maxLines = 2; typeface = Typeface.DEFAULT_BOLD })
                if (item.subtitle.isNotEmpty()) card.addView(text(item.subtitle, 12f, muted).apply { maxLines = 1 })
                if (item.positionMs > 0) card.addView(text(getString(R.string.resume_at, formatTime(item.positionMs)), 12f, muted))
                card.setOnClickListener { details(item) }
                line.addView(card, LinearLayout.LayoutParams(dp(230), dp(130)).apply { setMargins(dp(3), dp(3), dp(8), dp(3)) })
            }
            if (section.id != "continue") line.addView(button(R.string.view_all) { catalogPage(section.id) }.apply { tag = "all:" + section.id })
        }
        title(getString(R.string.apps_content))
        val services = horizontal(content, "services")
        for (module in snapshot.modules) {
            val id = module.id
            val label = serviceTitle(id) + "\n" + localizedState(module.state)
            services.addView(action(label) { if(id=="android_mirror") receiverSettings() else if (module.state == "DISABLED") providerList() else refresh(id, "") }.apply {
                tag = "service:$id"
                layoutParams = LinearLayout.LayoutParams(dp(165), dp(66)).apply { setMargins(dp(3), dp(3), dp(6), dp(3)) }
            })
        }
        if (snapshot.modules.isEmpty()) services.addView(button(R.string.connect_gateway) { pairing() })
        content.addView(text(getString(if (isTV()) R.string.docked_description else R.string.handheld_description), 14f, muted))
        homeFocus.rebuild(focusRows + Pair("transport", bottom), !full)
    }
    private fun refresh(provider: String = homeViewModel.state.scope.provider, query: String = homeViewModel.state.scope.query) {
        if (provider == "rebrowser") { startActivity(Intent(this, BrowserActivity::class.java)); return }
        if (api.token.isNotEmpty()) homeViewModel.refresh(HomeScope(provider, query))
    }
    private fun <T> async(work: () -> T, done: (T) -> Unit, notify: Boolean = true, onError: () -> Unit = {}) {
        if (closed) return
        worker.execute {
            try { val result = work(); runOnUiThread { if (!closed) done(result) } }
            catch (e: Exception) { runOnUiThread { if (!closed) { onError(); if (notify) error(e) } } }
        }
    }
    private fun error(e: Exception) {
        val message = if (e is GatewayFailure) getString(when (e.status) {
            401 -> R.string.error_pairing; 403 -> R.string.error_admin; 409 -> R.string.error_conflict; 429 -> R.string.error_busy
            else -> R.string.error_request
        }) else getString(R.string.error_network)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    private fun field(parent: LinearLayout, label: Int, secret: Boolean = false): EditText {
        parent.addView(text(getString(label), 14f, muted))
        return EditText(this).apply {
            setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(muted)
            inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            isSaveEnabled = false
            parent.addView(this, LinearLayout.LayoutParams(-1, dp(48)))
        }
    }
    private fun dialogForm(): Pair<ScrollView, LinearLayout> {
        val form = column(); form.setPadding(dp(18), dp(8), dp(18), dp(12))
        val scroll = ScrollView(this); scroll.addView(form); return Pair(scroll, form)
    }
    private fun pairing() {
        val (scroll, form) = dialogForm()
        form.addView(text(getString(R.string.lan_notice), 14f, muted))
        val address = field(form, R.string.gateway_address)
        address.setText(api.base); address.hint = getString(R.string.gateway_hint)
        val code = field(form, R.string.operator_code, true)
        val dialog = AlertDialog.Builder(this).setTitle(R.string.connect_gateway).setView(scroll).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.connect, null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val candidate = address.text.toString().trim().trimEnd('/')
                try { val u = URL(candidate); require(u.protocol in arrayOf("http", "https") && u.host.isNotEmpty() && u.userInfo == null && u.query == null && u.ref == null) }
                catch (_: Exception) { address.error = getString(R.string.invalid_address); return@setOnClickListener }
                val pairingCode = code.text.toString(); code.setText("")
                val identifier = prefs.getString("installation", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("installation", it).commit() }
                val payload = registration(identifier).put("pairingCode", pairingCode)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                // Use a separate connection profile until pairing succeeds.
                async({ val connection = GatewayApi(); connection.base = candidate; connection.request("POST", "/v1/devices/register", payload) }, { result ->
                    stopPlayback(); api.disconnect(); api.configure(candidate, result.getString("deviceId"), result.getString("deviceToken"))
                    prefs.edit().putString("gateway", api.base).putString("device", api.device).putString("token", api.token).commit()
                    dialog.dismiss(); receiverViewModel.reset(); homeViewModel.reset(); refresh(); receiverViewModel.refresh()
                }, onError = { dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true })
            }
        }
        dialog.setOnDismissListener { code.setText("") }
        dialog.show()
    }
    private fun registration(id: String): JSONObject {
        val display = resources.displayMetrics
        val touch = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        val memory = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return JSONObject().put("clientVersion", "0.1.0-dev.4").put("protocolVersion", 1).put("installationId", id)
            .put("platform", JSONObject().put("androidApi", Build.VERSION.SDK_INT).put("release", Build.VERSION.RELEASE).put("manufacturer", Build.MANUFACTURER).put("model", Build.MODEL).put("abis", JSONArray().put(Build.CPU_ABI)))
            .put("display", JSONObject().put("width", display.widthPixels).put("height", display.heightPixels).put("dpi", display.densityDpi).put("touch", touch).put("dpad", resources.configuration.navigation == Configuration.NAVIGATION_DPAD || !touch))
            .put("memory", JSONObject().put("memoryClassMb", memory.memoryClass).put("physicalMb", 0))
    }
    private fun settings() {
        AlertDialog.Builder(this).setTitle(R.string.settings).setItems(arrayOf(getString(R.string.connect_gateway), getString(R.string.configure_services), getString(R.string.diagnostics), getString(R.string.language), getString(R.string.presentation_mode), getString(R.string.advanced), getString(R.string.receive_cast), getString(R.string.gateway_services))) { _, index ->
            when (index) { 0 -> pairing(); 1 -> providerList(); 2 -> diagnostics(); 3 -> language(); 4 -> mode(); 5 -> advanced(); 6 -> receiverSettings(); 7 -> startActivity(Intent(this, ServicesActivity::class.java)) }
        }.setNegativeButton(R.string.close, null).show()
    }
    private fun providerList() {
        if (api.token.isEmpty()) { pairing(); return }
        async({ api.request("GET", "/v1/providers").getJSONArray("providers") }, { providers ->
            val labels = Array(providers.length()) { i -> val p = providers.getJSONObject(i); serviceTitle(p.getString("id")) + " · " + getString(if (p.optBoolean("enabled")) R.string.enabled else R.string.disabled) }
            AlertDialog.Builder(this).setTitle(R.string.configure_services).setItems(labels) { _, index -> providerForm(providers.getJSONObject(index)) }.setNegativeButton(R.string.close, null).show()
        })
    }
    private fun providerForm(provider: JSONObject) {
        val id = provider.getString("id")
        if (provider.optBoolean("managedByServer")) { AlertDialog.Builder(this).setTitle(serviceTitle(id)).setMessage(R.string.server_managed).setPositiveButton(R.string.close, null).show(); return }
        val (scroll, form) = dialogForm()
        form.addView(text(getString(if (provider.optBoolean("implemented")) R.string.secret_policy else R.string.adapter_pending), 14f, muted))
        val enabled = CheckBox(this).apply { setText(R.string.enabled); isChecked = provider.optBoolean("enabled"); setTextColor(Color.WHITE) }; form.addView(enabled)
        val address = field(form, if (id == "iptv") R.string.playlist_url else R.string.service_url, true)
        address.hint = getString(if (provider.optBoolean("configured")) R.string.keep_existing else R.string.optional_url)
        val token = field(form, R.string.service_token, true)
        token.hint = getString(if (provider.optBoolean("hasToken")) R.string.keep_existing else R.string.optional_token)
        val clear = CheckBox(this).apply { setText(R.string.remove_token); setTextColor(Color.WHITE) }; form.addView(clear)
        val user = if (id == "jellyfin") field(form, R.string.service_user) else null
        val epg = if (id == "iptv") field(form, R.string.epg_url, true) else null
        val catalog = if (id == "stremio") field(form, R.string.catalog_id) else null
        val media = if (id == "stremio") field(form, R.string.media_type) else null
        val code = field(form, R.string.operator_code, true)
        val dialog = AlertDialog.Builder(this).setTitle(serviceTitle(id)).setView(scroll).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.save, null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val patch = JSONObject().put("enabled", enabled.isChecked)
            for ((key, view) in arrayOf("url" to address, "token" to token, "userId" to user, "epgUrl" to epg, "catalogId" to catalog, "mediaType" to media)) {
                if (view != null && view.text.toString().isNotBlank()) patch.put(key, view.text.toString().trim())
            }
            if (clear.isChecked) patch.put("token", "")
            val admin = code.text.toString(); code.setText(""); token.setText(""); address.setText(""); epg?.setText("")
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            async({ api.request("PUT", "/v1/providers/$id", patch, admin) }, { dialog.dismiss(); Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show(); refresh() }, onError = { dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true })
        } }
        dialog.setOnDismissListener { code.setText(""); token.setText(""); address.setText(""); epg?.setText("") }
        dialog.show()
    }
    private fun advanced() {
        AlertDialog.Builder(this).setTitle(R.string.advanced).setItems(arrayOf(getString(R.string.audio_focus_backend),getString(R.string.playback_backend))) {_,index ->
            if(index==0)audioSettings() else {
                val modes=arrayOf("AUTO","DIRECT_PLAY","REMUX","TRANSCODE","EXTERNAL_PLAYER")
                val labels=arrayOf(R.string.automatic,R.string.direct_play,R.string.remux,R.string.transcode,R.string.external_player).map {getString(it)}.toTypedArray()
                AlertDialog.Builder(this).setTitle(R.string.playback_backend).setSingleChoiceItems(labels,modes.indexOf(prefs.getString("playbackMode","AUTO"))) {dialog,selection -> prefs.edit().putString("playbackMode",modes[selection]).commit();dialog.dismiss()}.setNegativeButton(R.string.close,null).show()
            }
        }.setNegativeButton(R.string.close,null).show()
    }
    private fun audioSettings() {
        AlertDialog.Builder(this).setTitle(R.string.audio_focus_backend)
            .setSingleChoiceItems(arrayOf(getString(R.string.backend_auto), getString(R.string.backend_compatibility)),
                if (prefs.getBoolean("audioFocusCompatibility", false)) 1 else 0) { dialog, index ->
                player.pause(); audioController.release()
                prefs.edit().putBoolean("audioFocusCompatibility", index == 1).commit()
                audioController = AudioFocusFactory.create(Build.VERSION.SDK_INT, getSystemService(AUDIO_SERVICE) as AudioManager, audioFocus, index == 1)
                dialog.dismiss()
            }.setNegativeButton(R.string.close, null).show()
    }
    private fun diagnostics() {
        val report = getString(R.string.device_report, Build.MANUFACTURER, Build.MODEL, Build.VERSION.SDK_INT, Build.CPU_ABI, (getSystemService(ACTIVITY_SERVICE) as ActivityManager).memoryClass)
        AlertDialog.Builder(this).setTitle(R.string.diagnostics).setMessage(report).setPositiveButton(R.string.close, null).show()
    }
    private fun language() { AlertDialog.Builder(this).setTitle(R.string.language).setItems(arrayOf("English", "Español")) { _, which ->
        prefs.edit().putString("language", if (which == 0) "en" else "es").commit()
        savePreferences(); Toast.makeText(this, R.string.restart_language, Toast.LENGTH_LONG).show()
    }.show() }
    private fun mode() { AlertDialog.Builder(this).setTitle(R.string.presentation_mode).setItems(arrayOf(getString(R.string.automatic), getString(R.string.tv), getString(R.string.docked), getString(R.string.handheld))) { _, which ->
        prefs.edit().putString("mode", arrayOf("AUTO", "TV", "DOCKED", "HANDHELD")[which]).commit(); savePreferences(); render()
    }.show() }
    private fun savePreferences() {
        if (api.token.isEmpty()) return
        val payload = JSONObject().put("mode", prefs.getString("mode", "AUTO")).put("uiLanguage", prefs.getString("language", "en"))
            .put("audioLanguages", JSONArray().put("en").put("es")).put("subtitleLanguages", JSONArray().put("en").put("es")).put("subtitleMode", "auto")
        async({ val current=api.request("GET", "/v1/device/preferences"); payload.put("allowCasting",current.optBoolean("allowCasting")); api.request("PUT", "/v1/device/preferences", payload) }, {})
    }
    private fun receiverSettings() {
        if(api.token.isEmpty()){pairing();return}
        val repository=GatewayReceiverRepository(api)
        async({repository.enabled()},{enabled ->
            AlertDialog.Builder(this).setTitle(R.string.receive_cast).setMessage(R.string.receive_cast_detail)
                .setPositiveButton(if(enabled)R.string.disable else R.string.enable){_,_ -> async({repository.setEnabled(!enabled)},{receiverViewModel.refresh()})}
                .setNegativeButton(R.string.cancel,null).show()
        })
    }
    private fun receiveCast(plan:ReceiverPlan?) {
        if(!foreground)return
        when(val change=receiverViewModel.transition(plan,PlaybackContext(currentItem,full,lastState=="PLAYING"))) {
            is ReceiverChange.Restore -> {
                stopPlayback(keepReceiver=true)
                change.previous?.let { previous -> previous.item?.let { startPlayback(it,previous.fullscreen,previous.playing) } }
            }
            is ReceiverChange.Begin -> {
                stopPlayback(keepReceiver=true)
                session=change.plan.sessionId;stream=api.base+change.plan.path;mime=change.plan.mime
                itemTitle=getString(R.string.screen_mirroring);lastReport=0;lastState="";setFullscreen(true)
                if(audioController.acquire())player.play(stream,0)
            }
            null -> Unit
        }
    }
    private fun isTV(): Boolean = when (prefs.getString("mode", "AUTO")) {
        "TV", "DOCKED" -> true; "HANDHELD" -> false; else -> !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    }
    private fun search() {
        val input = EditText(this); input.setSingleLine(true)
        AlertDialog.Builder(this).setTitle(R.string.search).setView(input).setPositiveButton(R.string.search) { _, _ -> refresh(query = input.text.toString()) }.setNegativeButton(R.string.cancel, null).show()
    }
    private fun catalogPage(provider: String, offset: Int = 0) {
        async({ api.request("GET", "/v1/catalog?provider=" + URLEncoder.encode(provider, "UTF-8") + "&offset=$offset&q=" + URLEncoder.encode(homeViewModel.state.scope.query, "UTF-8")) }, { page ->
            val items = page.getJSONArray("items")
            val labels = Array(items.length()) { i -> val item = items.getJSONObject(i); item.optString("title") + item.optString("subtitle").takeIf { it.isNotEmpty() }?.let { " — $it" }.orEmpty() }
            val dialog = AlertDialog.Builder(this).setTitle(serviceTitle(provider)).setItems(labels) { _, index -> details(GatewayHomeRepository.decodeItem(items.getJSONObject(index))) }.setNegativeButton(R.string.close, null)
            val next = page.optInt("nextOffset", -1)
            if (next >= 0) dialog.setPositiveButton(R.string.next_page) { _, _ -> catalogPage(provider, next) }
            if (offset > 0) dialog.setNeutralButton(R.string.previous_page) { _, _ -> catalogPage(provider, (offset - 40).coerceAtLeast(0)) }
            dialog.show()
        })
    }
    private fun details(item: MediaItem) {
        val description = StringBuilder(item.description.takeIf { it.isNotEmpty() } ?: serviceTitle(item.provider))
        for (programme in item.programmes) {
            val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(programme.start * 1000))
            description.append("\n\n").append(time).append(" · ").append(programme.title)
        }
        AlertDialog.Builder(this).setTitle(item.title).setMessage(description.toString())
            .setPositiveButton(R.string.play) { _, _ -> startPlayback(item) }.setNegativeButton(R.string.close, null).show()
    }
    private fun createPlayer() {
        playerLayer = column(); playerLayer.setBackgroundColor(Color.BLACK); playerLayer.visibility = View.GONE
        val surface = VideoSurface(this)
        videoSurface = surface
        surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) { player.surface(holder) }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { }
            override fun surfaceDestroyed(holder: SurfaceHolder) { player.surface(null) }
        })
        val viewport = FrameLayout(this)
        viewport.addView(surface, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        playerLayer.addView(viewport, LinearLayout.LayoutParams(-1, 0, 1f))
        playerStatus = text("", 12f, muted); playerLayer.addView(playerStatus)
        val controls = horizontal(playerLayer)
        controls.addView(button(R.string.seek_back) { player.seek(-10000) })
        controls.addView(button(R.string.play_pause) { togglePlayback() })
        controls.addView(button(R.string.seek_forward) { player.seek(10000) })
        controls.addView(button(R.string.minimize) { setFullscreen(!full) })
        controls.addView(button(R.string.external_player) { external() })
        controls.addView(button(R.string.stop) { stopPlayback() })
        playerFocus.rebuild(listOf(Pair("player", controls)), false)
        root.addView(playerLayer, FrameLayout.LayoutParams(-1, -1))
    }
    private fun togglePlayback() { if (session.isNotEmpty() && foreground && audioController.acquire()) player.toggle() }
    private fun startPlayback(item: MediaItem, fullscreen:Boolean=true,autoplay:Boolean=true) {
        stopPlayback()
        val generation = playbackGeneration
        async({ api.request("POST", "/v1/playback", JSONObject().put("itemId", item.id).put("mode",prefs.getString("playbackMode","AUTO"))) }, { plan ->
            if (generation != playbackGeneration) {
                val abandoned = plan.getString("sessionId")
                async({ api.request("DELETE", "/v1/playback/$abandoned") }, {}, false)
                return@async
            }
            session = plan.getString("sessionId"); stream = api.base + plan.getString("url"); mime = plan.optString("mimeType", "video/mp4")
            currentItem=item; itemTitle = item.title; now.text = itemTitle; lastState = ""; lastReport = 0; lastPosition = 0; lastDuration = 0
            setFullscreen(fullscreen)
            if(plan.optString("mode")=="EXTERNAL_PLAYER"){external();return@async}
            if (audioController.acquire()) {
                player.play(stream, plan.optInt("resumePositionMs"))
                if (!foreground || !autoplay) player.pause()
            }
        })
    }
    private fun setFullscreen(value: Boolean) {
        full = value; playerLayer.visibility = View.VISIBLE
        playerLayer.layoutParams = if (full) FrameLayout.LayoutParams(-1, -1) else FrameLayout.LayoutParams(dp(320), dp(230), Gravity.BOTTOM or Gravity.RIGHT).apply { bottomMargin = dp(58); rightMargin = dp(12) }
        content.descendantFocusability = if (full) ViewGroup.FOCUS_BLOCK_DESCENDANTS else ViewGroup.FOCUS_AFTER_DESCENDANTS
        bottom.descendantFocusability = content.descendantFocusability
        playerLayer.bringToFront()
        if (full) playerFocus.restore() else homeFocus.restore()
    }
    private fun stopPlayback(keepReceiver:Boolean=false) {
        if(!keepReceiver && receiverViewModel.activeSession.isNotEmpty())receiverViewModel.dismiss(receiverViewModel.activeSession)
        currentItem=null
        audioController.release()
        playbackGeneration++
        if (session.isNotEmpty()) {
            val old = session
            val progress = JSONObject().put("state", if (lastState == "ENDED") "ENDED" else "STOPPED").put("positionMs", lastPosition).put("durationMs", lastDuration)
            async({ try { api.request("PUT", "/v1/playback/$old/progress", progress) } finally { api.request("DELETE", "/v1/playback/$old") } }, {}, false)
        }
        session = ""; stream = ""; full = false; player.stop(); playerLayer.visibility = View.GONE; now.setText(R.string.nothing_playing)
        content.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        bottom.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        homeFocus.restore()
    }
    private fun external() {
        if (stream.isEmpty()) return
        player.pause()
        try { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(stream), mime)) }
        catch (_: Exception) { Toast.makeText(this, R.string.no_external_player, Toast.LENGTH_LONG).show() }
    }
    private fun formatTime(ms: Int): String { val seconds = ms.coerceAtLeast(0) / 1000; return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60) }
    private fun serviceTitle(id: String): String = getString(when (id) {
        "local" -> R.string.local_library; "youtube" -> R.string.youtube; "plex" -> R.string.plex; "jellyfin" -> R.string.jellyfin; "stremio" -> R.string.stremio
        "spotify" -> R.string.spotify; "iptv" -> R.string.iptv; "airplay" -> R.string.airplay; "android_mirror" -> R.string.android_mirror; "rebrowser" -> R.string.browser
        else -> R.string.apps_content
    })
    private fun localizedState(state: String): String = getString(when (state) {
        "HEALTHY" -> R.string.ready; "STARTING", "BUFFERING" -> R.string.loading; "DISABLED" -> R.string.disabled
        "PLAYING" -> R.string.playing; "PAUSED" -> R.string.paused; "ENDED" -> R.string.ended; "STOPPED" -> R.string.stopped
        else -> R.string.unavailable
    })
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val key = event.keyCode
        if (currentFocus !is EditText) {
            if (event.action == KeyEvent.ACTION_DOWN && (if (full) playerFocus else homeFocus).move(key, currentFocus)) return true
            // Preserve native CENTER/ENTER activation; gamepad A follows the same key-up contract.
            if (key == KeyEvent.KEYCODE_BUTTON_A) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) gamepadClick = currentFocus
                if (event.action == KeyEvent.ACTION_UP) {
                    if (!event.isCanceled && gamepadClick === currentFocus) gamepadClick?.performClick()
                    gamepadClick = null
                }
                return true
            }
            if (key == KeyEvent.KEYCODE_BUTTON_B) {
                if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onBackPressed()
                return true
            }
            if (session.isNotEmpty() && key in intArrayOf(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_STOP,
                    KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)) {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) when (key) {
                    KeyEvent.KEYCODE_MEDIA_PLAY -> if (audioController.acquire()) player.resume()
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> player.pause()
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> togglePlayback()
                    KeyEvent.KEYCODE_MEDIA_STOP -> stopPlayback()
                    KeyEvent.KEYCODE_MEDIA_REWIND -> player.seek(-10000)
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> player.seek(10000)
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onBackPressed() { if (full) setFullscreen(false) else if (session.isNotEmpty()) stopPlayback() else super.onBackPressed() }
    override fun onResume() { super.onResume(); foreground = true; if(::receiverViewModel.isInitialized && api.token.isNotEmpty())receiverViewModel.refresh() }
    override fun onPause() { foreground = false; player.pause(); super.onPause() }
    override fun onDestroy() {
        audioController.release()
        closed = true; receiverViewModel.close(); homeViewModel.close(); foreground = false; handler.removeCallbacksAndMessages(null); api.close(); player.close(); worker.shutdownNow(); poller.shutdownNow(); super.onDestroy()
    }
}
