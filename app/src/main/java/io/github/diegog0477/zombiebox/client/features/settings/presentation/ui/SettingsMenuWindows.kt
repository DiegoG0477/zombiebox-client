package io.github.diegog0477.zombiebox.client.features.settings.presentation.ui

import android.app.Activity
import android.app.AlertDialog
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.features.settings.presentation.viewmodel.SettingsNavigation

/** Keep the parent list alive beneath child dialogs, preserving native D-pad focus. */
class SettingsMenuWindows(private val activity: Activity, val navigation: SettingsNavigation) {
    private val windows = LinkedHashMap<String, AlertDialog>()
    private val itemKeys = HashMap<String, List<String>>()

    fun show(
        route: String,
        title: Int,
        labels: Array<String>,
        keys: List<String> = emptyList(),
        selected: (Int) -> Unit,
    ) {
        windows[route]
            ?.takeIf { it.isShowing }
            ?.let {
                return
            }
        navigation.open(route)
        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setItems(labels, null)
                .setNegativeButton(R.string.close, null)
                .create()
        windows[route] = dialog
        itemKeys[route] = keys
        dialog.setOnDismissListener {
            if (windows[route] === dialog) {
                val descendants = windows.keys.dropWhile { it != route }.drop(1)
                descendants.forEach { windows.remove(it)?.dismiss() }
                windows.remove(route)
                navigation.close(route)
            }
        }
        dialog.show()
        dialog.listView.setOnItemClickListener { _, _, index, _ ->
            navigation.activate(route, index, itemKeys[route]?.getOrNull(index) ?: "")
            selected(index)
        }
    }

    fun restoreSelection(route: String, index: Int, key: String = "") {
        val list = windows[route]?.listView ?: return
        val byKey = itemKeys[route]?.indexOf(key) ?: -1
        val selection =
            (if (byKey >= 0) byKey else index).coerceIn(0, (list.count - 1).coerceAtLeast(0))
        navigation.select(route, selection, itemKeys[route]?.getOrNull(selection) ?: "")
        list.setSelection(selection)
        list.requestFocus()
    }

    fun snapshot(): List<SettingsNavigation.Menu> {
        for ((route, dialog) in windows) {
            val index = dialog.listView.selectedItemPosition
            if (index >= 0) navigation.select(route, index, itemKeys[route]?.getOrNull(index) ?: "")
        }
        return navigation.path
    }

    fun close() {
        val closing = windows.values.toList()
        windows.clear()
        itemKeys.clear()
        navigation.reset()
        closing.reversed().forEach { it.dismiss() }
    }
}
