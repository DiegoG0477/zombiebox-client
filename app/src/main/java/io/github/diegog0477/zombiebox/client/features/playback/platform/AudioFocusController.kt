package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.media.AudioManager

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
    ): AudioFocusController {
        if (api >= 26 && !compatibility)
            try {
                return Class.forName(
                        "io.github.diegog0477.zombiebox.client.features.playback.platform.Api26AudioFocus"
                    )
                    .getConstructor(
                        AudioManager::class.java,
                        AudioManager.OnAudioFocusChangeListener::class.java,
                    )
                    .newInstance(manager, listener) as AudioFocusController
            } catch (_: Exception) {
                // Construction failure falls back to the baseline backend.
            } catch (_: LinkageError) {}
        return LegacyAudioFocus(manager, listener)
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
