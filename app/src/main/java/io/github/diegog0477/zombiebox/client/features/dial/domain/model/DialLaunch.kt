package io.github.diegog0477.zombiebox.client.features.dial.domain.model

/** DIAL is discovery/launch, not pairing, a credential carrier or a media command. */
object DialLaunch {
    const val ACTION = "io.github.diegog0477.zombiebox.client.DIAL_HOME"

    fun accepts(action: String?, payload: String?): Boolean =
        action == ACTION && (payload.isNullOrEmpty() || payload == "screen=home")
}
