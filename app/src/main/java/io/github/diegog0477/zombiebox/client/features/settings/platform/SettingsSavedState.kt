package io.github.diegog0477.zombiebox.client.features.settings.platform

import android.os.Bundle
import io.github.diegog0477.zombiebox.client.features.settings.presentation.viewmodel.SettingsNavigation

object SettingsSavedState {
    fun write(target: Bundle, path: List<SettingsNavigation.Menu>) {
        target.putStringArray("settingsRoutes", path.take(2).map { it.route }.toTypedArray())
        target.putStringArray(
            "settingsKeys",
            path.take(2).map { it.selectedKey.take(40) }.toTypedArray(),
        )
        target.putIntArray("settingsSelections", path.take(2).map { it.selected }.toIntArray())
    }

    fun read(source: Bundle?): List<SettingsNavigation.Menu> {
        val routes = source?.getStringArray("settingsRoutes") ?: return emptyList()
        val selections = source.getIntArray("settingsSelections") ?: return emptyList()
        val keys = source.getStringArray("settingsKeys")
        return routes.take(2).mapIndexed { index, route ->
            SettingsNavigation.Menu(
                route,
                selections.getOrNull(index) ?: 0,
                keys?.getOrNull(index) ?: "",
            )
        }
    }
}
