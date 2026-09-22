package io.github.diegog0477.zombiebox.client.features.diagnostics.platform

import android.annotation.TargetApi
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.DisplayHint
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.DisplayDiscovery

/** API17 logical display metrics do not establish the active native output mode. */
@TargetApi(17)
class Api17DisplayDiscovery(private val context: Context) : DisplayDiscovery {
    override fun displays(): List<DisplayHint> {
        val manager =
            context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                ?: return emptyList()
        val presentationIds =
            manager
                .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
                .take(8)
                .map { it.displayId }
                .toSet()
        return manager.displays.take(8).mapNotNull { display ->
            try {
                val metrics = DisplayMetrics()
                display.getRealMetrics(metrics)
                DisplayHint(
                    display.displayId,
                    display.displayId == Display.DEFAULT_DISPLAY,
                    display.displayId in presentationIds,
                    metrics.widthPixels,
                    metrics.heightPixels,
                    (display.refreshRate * 1000).toInt(),
                )
            } catch (_: Exception) {
                null
            } catch (_: LinkageError) {
                null
            }
        }
    }
}
