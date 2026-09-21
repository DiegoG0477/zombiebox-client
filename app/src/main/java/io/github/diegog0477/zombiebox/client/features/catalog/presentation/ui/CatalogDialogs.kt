package io.github.diegog0477.zombiebox.client.features.catalog.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.widget.*
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
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

    fun search() {
        val input = EditText(activity)
        input.setSingleLine(true)
        AlertDialog.Builder(activity)
            .setTitle(R.string.search)
            .setView(input)
            .setPositiveButton(R.string.search) { _, _ -> searchQuery(input.text.toString()) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private var browser: AlertDialog? = null

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
        browser?.dismiss()
        val provider = screen.location.provider
        val list = CatalogListView(activity, screen, ui.providerAccent(provider))
        fun remember() {
            model.rememberViewport(list.viewport())
        }
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
            builder.setNeutralButton(R.string.back) { _, _ -> model.back(::showPage) }
        browser =
            builder.create().also { dialog ->
                dialog.setOnCancelListener { if (!model.back(::showPage)) model.dismiss() }
                dialog.show()
                list.restoreViewport()
            }
    }

    private fun searchPage(screen: CatalogScreen) {
        browser?.dismiss()
        val input =
            EditText(activity).apply {
                setSingleLine(true)
                setText(screen.location.query)
            }
        browser =
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
        dialog.setOnCancelListener { closed?.invoke() }
        dialog.show()
    }
}
