package io.github.diegog0477.zombiebox.client.features.catalog.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.widget.*
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogBookmark
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogOverlay
import io.github.diegog0477.zombiebox.client.features.catalog.domain.model.CatalogScreen
import io.github.diegog0477.zombiebox.client.features.catalog.presentation.viewmodel.CatalogViewModel

class CatalogDialogs(
    private val activity: Activity,
    private val model: CatalogViewModel,
    private val query: () -> String,
    private val searchQuery: (String) -> Unit,
    private val play: (MediaItem) -> Unit,
    private val error: (Exception) -> Unit,
) {
    private val ui = TvWidgets(activity)

    private var overlay: AlertDialog? = null
    private var overlayCapture: () -> CatalogOverlay = { CatalogOverlay() }
    private var guide: GuideDialog? = null

    fun overlaySnapshot(): CatalogOverlay =
        guide?.snapshot()?.takeIf { it.kind.isNotEmpty() }
            ?: if (overlay?.isShowing == true) overlayCapture() else CatalogOverlay()

    fun search(draft: String = query()) {
        val input = EditText(activity)
        input.setSingleLine(true)
        input.setText(draft)
        overlay?.dismiss()
        overlayCapture = { CatalogOverlay("home_search", input.text.toString()) }
        overlay =
            AlertDialog.Builder(activity)
                .setTitle(R.string.search)
                .setView(input)
                .setPositiveButton(R.string.search) { _, _ -> searchQuery(input.text.toString()) }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private var browser: AlertDialog? = null
    private var detail: AlertDialog? = null
    val visible: Boolean
        get() =
            browser?.isShowing == true ||
                detail?.isShowing == true ||
                overlay?.isShowing == true ||
                guide?.visible == true

    var detailItemId: String = ""
        private set

    private var captureViewport: (() -> Unit)? = null

    fun snapshot(): List<CatalogBookmark> {
        captureViewport?.invoke()
        return model.bookmarks()
    }

    fun restore(
        path: List<CatalogBookmark>,
        detailId: String = "",
        display: Boolean = true,
        savedOverlay: CatalogOverlay = CatalogOverlay(),
    ) {
        if (path.isEmpty()) {
            if (display && savedOverlay.kind == "home_search") search(savedOverlay.draft)
            return
        }
        val work = {
            model.restore(
                path,
                { screen ->
                    if (display) {
                        showPage(screen)
                        when (savedOverlay.kind) {
                            "home_search" -> search(savedOverlay.draft)
                            "provider_search" -> searchPage(screen, savedOverlay.draft)
                            "guide" -> openGuide(screen, savedOverlay)
                        }
                        screen.page.items
                            .firstOrNull { it.id == detailId }
                            ?.let { item ->
                                browser?.dismiss()
                                showDetails(item) { model.screen?.let(::showPage) }
                            }
                    }
                },
                { failure -> if (display) loadFailed(failure) },
            )
        }
        if (display) load(work) else work()
    }

    fun resume(): Boolean {
        val current = model.screen ?: return false
        showPage(current)
        return true
    }

    fun close() {
        overlay?.dismiss()
        overlay = null
        guide?.close()
        guide = null
        captureViewport = null
        detail?.dismiss()
        detail = null
        browser?.dismiss()
        browser = null
    }

    fun page(provider: String) {
        load { model.open(provider, query(), ::showPage, ::loadFailed) }
    }

    private fun load(work: () -> Unit) {
        browser?.dismiss()
        browser =
            AlertDialog.Builder(activity)
                .setMessage(R.string.loading)
                .setNegativeButton(R.string.cancel) { _, _ -> cancelLoad() }
                .create()
                .also {
                    it.setOnCancelListener { cancelLoad() }
                    it.show()
                }
        work()
    }

    private fun cancelLoad() {
        model.cancelPending()
        model.screen?.let(::showPage) ?: model.dismiss()
    }

    private fun loadFailed(failure: Exception) {
        browser?.dismiss()
        model.screen?.let(::showPage)
        error(failure)
    }

    private fun showPage(screen: CatalogScreen) {
        detailItemId = ""
        detail?.dismiss()
        detail = null
        browser?.dismiss()
        val provider = screen.location.provider
        val list = CatalogListView(activity, screen, ui.providerAccent(provider))
        fun remember() {
            model.rememberViewport(list.viewport())
        }
        captureViewport = ::remember
        val content =
            ui.column().apply {
                setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8))
                setBackgroundColor(ui.background)
                addView(
                    ui.text(
                        activity.getString(R.string.catalog_scope, ui.serviceTitle(provider)),
                        14f,
                        ui.providerAccent(provider),
                    )
                )
                if (provider == "iptv")
                    addView(
                        ui.button(R.string.guide) {
                            remember()
                            openGuide(model.screen ?: screen)
                        }
                    )
                if (screen.location.query.isNotEmpty())
                    addView(ui.text(screen.location.query, 14f, ui.muted))
                if (screen.page.items.isEmpty())
                    addView(ui.text(activity.getString(R.string.catalog_empty), 16f, ui.muted))
                else addView(list, LinearLayout.LayoutParams(-1, ui.dp(320)))
                addView(
                    ui.action(activity.getString(R.string.search), ui.providerAccent(provider)) {
                        remember()
                        searchPage(screen)
                    }
                )
            }
        list.setOnItemClickListener { _, _, index, _ ->
            model.rememberViewport(list.viewport(index))
            val item = screen.page.items[index]
            if (item.browseId.isNotEmpty()) load { model.enter(item, ::showPage, ::loadFailed) }
            else {
                browser?.dismiss()
                showDetails(item) { model.screen?.let(::showPage) }
            }
        }
        val builder =
            AlertDialog.Builder(activity)
                .setTitle(screen.page.title.ifEmpty { ui.serviceTitle(provider) })
                .setView(content)
                .setNegativeButton(R.string.close) { _, _ -> model.dismiss() }
        if (screen.page.nextOffset >= 0)
            builder.setPositiveButton(R.string.next_page) { _, _ ->
                remember()
                load { model.next(::showPage, ::loadFailed) }
            }
        if (model.canBack)
            builder.setNeutralButton(R.string.back) { _, _ -> model.back(::showPage, ::loadFailed) }
        browser =
            builder.create().also { dialog ->
                dialog.setOnCancelListener {
                    if (!model.back(::showPage, ::loadFailed)) model.dismiss()
                }
                dialog.show()
                list.restoreViewport()
            }
    }

    private fun openGuide(screen: CatalogScreen, saved: CatalogOverlay = CatalogOverlay()) {
        guide?.close()
        guide =
            GuideDialog(
                    activity,
                    screen.page.items,
                    { item ->
                        browser?.dismiss()
                        play(item)
                    },
                    { updated ->
                        model.refresh(
                            { current -> updated(current.page.items) },
                            { updated(model.screen?.page?.items ?: emptyList()) },
                        )
                    },
                )
                .also { it.show(saved) }
    }

    private fun searchPage(screen: CatalogScreen, draft: String = screen.location.query) {
        overlay?.dismiss()
        browser?.dismiss()
        val input =
            EditText(activity).apply {
                setSingleLine(true)
                setText(draft)
            }
        overlayCapture = { CatalogOverlay("provider_search", input.text.toString()) }
        overlay =
            AlertDialog.Builder(activity)
                .setTitle(
                    activity.getString(
                        R.string.catalog_scope,
                        ui.serviceTitle(screen.location.provider),
                    )
                )
                .setView(input)
                .setPositiveButton(R.string.search) { _, _ ->
                    load { model.search(input.text.toString(), ::showPage, ::loadFailed) }
                }
                .setNegativeButton(R.string.cancel) { _, _ -> model.screen?.let(::showPage) }
                .create()
                .also {
                    it.setOnCancelListener { model.screen?.let(::showPage) }
                    it.show()
                }
    }

    fun details(item: MediaItem) = showDetails(item, null)

    private fun showDetails(item: MediaItem, closed: (() -> Unit)?) {
        detail?.dismiss()
        detailItemId = item.id
        val description =
            StringBuilder(
                item.description.takeIf { it.isNotEmpty() } ?: ui.serviceTitle(item.provider)
            )
        for (programme in item.programmes) {
            val time =
                java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
                    .format(java.util.Date(programme.start * 1000))
            description.append("\n\n").append(time).append(" · ").append(programme.title)
        }
        val builder =
            AlertDialog.Builder(activity)
                .setTitle(item.title)
                .setMessage(description.toString())
                .setNegativeButton(R.string.close) { _, _ -> closed?.invoke() }
        if (item.playable) builder.setPositiveButton(R.string.play) { _, _ -> play(item) }
        val dialog = builder.create()
        detail = dialog
        dialog.setOnCancelListener { closed?.invoke() }
        dialog.show()
    }
}
