package io.github.diegog0477.zombiebox.client.features.catalog.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import android.widget.*
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.core.model.MediaItem
import io.github.diegog0477.zombiebox.client.core.ui.TvWidgets
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

    fun page(provider: String, offset: Int = 0) {
        model.page(
            provider,
            query(),
            offset,
            { page ->
                val labels =
                    page.items
                        .map { it.title + if (it.subtitle.isEmpty()) "" else " — " + it.subtitle }
                        .toTypedArray()
                val dialog =
                    AlertDialog.Builder(activity)
                        .setTitle(ui.serviceTitle(provider))
                        .setItems(labels) { _, index -> details(page.items[index]) }
                        .setNegativeButton(R.string.close, null)
                if (page.nextOffset >= 0)
                    dialog.setPositiveButton(R.string.next_page) { _, _ ->
                        page(provider, page.nextOffset)
                    }
                if (offset > 0)
                    dialog.setNeutralButton(R.string.previous_page) { _, _ ->
                        page(provider, (offset - 40).coerceAtLeast(0))
                    }
                dialog.show()
            },
            error,
        )
    }

    fun details(item: MediaItem) {
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
        AlertDialog.Builder(activity)
            .setTitle(item.title)
            .setMessage(description.toString())
            .setPositiveButton(R.string.play) { _, _ -> play(item) }
            .setNegativeButton(R.string.close, null)
            .show()
    }
}
