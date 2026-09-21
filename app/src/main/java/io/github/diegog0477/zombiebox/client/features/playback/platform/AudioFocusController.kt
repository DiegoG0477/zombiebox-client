package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.media.AudioManager
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.StrategyHealth

interface AudioFocusController {
    fun acquire(): Boolean

    fun release()
}

/** New platform classes are named as strings, never referenced by the legacy verifier. */
object AudioFocusFactory {
    fun create(
        api: Int,
        manager: AudioManager,
        listener: AudioManager.OnAudioFocusChangeListener,
        compatibility: Boolean = false,
        loadHealth: () -> StrategyHealth = { StrategyHealth() },
        saveHealth: (StrategyHealth) -> Unit = {},
        clock: () -> Long = { System.currentTimeMillis() },
    ): AudioFocusController {
        val legacy = LegacyAudioFocus(manager, listener)
        if (api >= 26 && !compatibility && loadHealth().available(clock()))
            try {
                val native =
                    Class.forName(
                            "io.github.diegog0477.zombiebox.client.features.playback.platform.Api26AudioFocus"
                        )
                        .getConstructor(
                            AudioManager::class.java,
                            AudioManager.OnAudioFocusChangeListener::class.java,
                        )
                        .newInstance(manager, listener) as AudioFocusController
                return RecoveringAudioFocus(native, legacy, loadHealth, saveHealth, clock)
            } catch (_: Exception) {
                saveHealth(loadHealth().failed(clock()))
                // Construction failure falls back to the baseline backend.
            } catch (_: LinkageError) {
                saveHealth(loadHealth().failed(clock()))
            }
        return legacy
    }
}

/** Native operations are tried first; focus denial is respected, never bypassed. */
internal class RecoveringAudioFocus(
    private val native: AudioFocusController,
    private val fallback: AudioFocusController,
    private val load: () -> StrategyHealth,
    private val save: (StrategyHealth) -> Unit,
    private val clock: () -> Long,
) : AudioFocusController {
    private var active: AudioFocusController = native

    override fun acquire(): Boolean {
        val candidate = if (load().available(clock())) native else fallback
        if (candidate !== active) {
            release()
            active = candidate
        }
        return try {
            val granted = active.acquire()
            if (granted && active === native) save(load().succeeded())
            granted
        } catch (_: Exception) {
            recover()
        } catch (_: LinkageError) {
            recover()
        }
    }

    private fun recover(): Boolean {
        if (active === fallback) return false
        save(load().failed(clock()))
        try {
            active.release()
        } catch (_: Exception) {} catch (_: LinkageError) {}
        active = fallback
        return try {
            fallback.acquire()
        } catch (_: Exception) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    override fun release() {
        try {
            active.release()
        } catch (_: Exception) {} catch (_: LinkageError) {}
    }
}

@Suppress("DEPRECATION")
class LegacyAudioFocus(
    private val manager: AudioManager,
    private val listener: AudioManager.OnAudioFocusChangeListener,
) : AudioFocusController {
    override fun acquire() =
        manager.requestAudioFocus(
            listener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN,
        ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    override fun release() {
        manager.abandonAudioFocus(listener)
    }
}
