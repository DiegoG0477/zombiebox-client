package io.github.diegog0477.zombiebox.client.features.diagnostics.platform

import android.content.Context
import android.os.Build
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.CodecHint
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.model.DisplayHint
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.CodecDiscovery
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.CodecImplementations
import io.github.diegog0477.zombiebox.client.features.diagnostics.domain.repository.DisplayDiscovery

/** API-isolated inventory with conservative fallback after vendor/linkage failures. */
class PlatformInventory(private val context: Context) {
    data class Snapshot(
        val decoders: List<CodecHint>,
        val encoders: List<CodecHint>,
        val displays: List<DisplayHint>,
    )

    fun scan(): Snapshot {
        val api = Build.VERSION.SDK_INT
        val codec =
            if (api >= 21)
                load("Api21CodecDiscovery", CodecDiscovery::class.java)
                    ?: load("Api16CodecDiscovery", CodecDiscovery::class.java)
            else if (api >= 16) load("Api16CodecDiscovery", CodecDiscovery::class.java) else null
        val acceleration =
            if (api >= 29)
                safely {
                    load("Api29CodecImplementations", CodecImplementations::class.java)
                        ?.declaredAcceleration() ?: emptyMap()
                } ?: emptyMap()
            else emptyMap()
        fun codecs(encoder: Boolean): List<CodecHint> {
            var values = safely { if (encoder) codec?.encoders() else codec?.decoders() }
            if (values.isNullOrEmpty() && api >= 21) {
                val fallback = load("Api16CodecDiscovery", CodecDiscovery::class.java)
                values = safely { if (encoder) fallback?.encoders() else fallback?.decoders() }
            }
            return (values ?: emptyList()).take(if (encoder) 32 else 128).map {
                it.copy(acceleration = acceleration[it.name] ?: "UNKNOWN")
            }
        }
        var displays =
            if (api >= 23)
                safely {
                    load("Api23DisplayDiscovery", DisplayDiscovery::class.java, true)?.displays()
                } ?: emptyList()
            else emptyList()
        if (displays.isEmpty() && api >= 17)
            displays =
                safely {
                    load("Api17DisplayDiscovery", DisplayDiscovery::class.java, true)?.displays()
                } ?: emptyList()
        return Snapshot(
            codecs(false),
            codecs(true),
            displays
                .filter {
                    it.id >= 0 &&
                        it.width in 1..16384 &&
                        it.height in 1..16384 &&
                        it.refreshMilliHz in 0..480000
                }
                .distinctBy { it.id }
                .take(8),
        )
    }

    private fun <T> load(name: String, type: Class<T>, needsContext: Boolean = false): T? = safely {
        val implementation =
            Class.forName(
                "io.github.diegog0477.zombiebox.client.features.diagnostics.platform.$name"
            )
        type.cast(
            if (needsContext)
                implementation.getConstructor(Context::class.java).newInstance(context)
            else implementation.getConstructor().newInstance()
        )
    }

    private fun <T> safely(action: () -> T): T? =
        try {
            action()
        } catch (_: Exception) {
            null
        } catch (_: LinkageError) {
            null
        }
}
