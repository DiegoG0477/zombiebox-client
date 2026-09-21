package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.annotation.TargetApi
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** Loaded only by AudioFocusFactory on API 26+. */
@TargetApi(26)
class Api26AudioFocus(
    private val manager: AudioManager,
    listener: AudioManager.OnAudioFocusChangeListener,
) : AudioFocusController {
    private val request =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            .setOnAudioFocusChangeListener(listener)
            .setWillPauseWhenDucked(true)
            .build()

    override fun acquire() =
        manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    override fun release() {
        manager.abandonAudioFocusRequest(request)
    }
}
