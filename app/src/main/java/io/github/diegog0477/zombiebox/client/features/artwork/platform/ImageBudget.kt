package io.github.diegog0477.zombiebox.client.features.artwork.platform

import android.app.ActivityManager
import android.content.Context
import io.github.diegog0477.zombiebox.client.features.diagnostics.platform.HardwareMemory

data class ImageBudget(val lowMemory: Boolean) {
    val encodedBytes: Int
        get() = if (lowMemory) 2 * 1024 * 1024 else 4 * 1024 * 1024

    val heroWidth: Int
        get() = if (lowMemory) 640 else 960

    val cardWidth: Int
        get() = if (lowMemory) 240 else 320

    companion object {
        fun discover(context: Context): ImageBudget {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val physical = HardwareMemory.physicalMb()
            return ImageBudget(manager.memoryClass <= 96 || physical in 1..768)
        }
    }
}
