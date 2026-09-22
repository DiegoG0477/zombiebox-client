package io.github.diegog0477.zombiebox.client.features.diagnostics.platform

import android.annotation.TargetApi
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.DisplayHint
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.DisplayModeHint
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.DisplayDiscovery

/** Some OEMs report synthetic modes. Inventory never changes mode or certifies output. */
@TargetApi(23)
class Api23DisplayDiscovery(private val context: Context) : DisplayDiscovery {
    override fun displays(): List<DisplayHint> {
        val manager =
            context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                ?: return emptyList()
        return manager.displays.take(8).mapNotNull { display ->
            try {
                val active = display.mode
                val modes =
                    (listOf(active) + display.supportedModes.take(32))
                        .filter {
                            it.modeId > 0 &&
                                it.physicalWidth in 1..16384 &&
                                it.physicalHeight in 1..16384 &&
                                it.refreshRate.isFinite() &&
                                it.refreshRate in 0f..480f
                        }
                        .distinctBy { it.modeId }
                        .take(16)
                if (modes.none { it.modeId == active.modeId }) return@mapNotNull null
                DisplayHint(
                    display.displayId,
                    display.displayId == Display.DEFAULT_DISPLAY,
                    display.flags and Display.FLAG_PRESENTATION != 0,
                    active.physicalWidth,
                    active.physicalHeight,
                    (active.refreshRate * 1000).toInt(),
                    active.modeId,
                    modes.map {
                        DisplayModeHint(
                            it.modeId,
                            it.physicalWidth,
                            it.physicalHeight,
                            (it.refreshRate * 1000).toInt(),
                        )
                    },
                )
            } catch (_: Exception) {
                null
            } catch (_: LinkageError) {
                null
            }
        }
    }
}
