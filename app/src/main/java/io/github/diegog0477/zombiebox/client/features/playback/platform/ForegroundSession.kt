package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.app.Notification
import android.app.Service
import android.os.Build

interface ForegroundSession {
    fun show(service: Service, notification: Notification, receiving: Boolean, playing: Boolean)

    companion object {
        fun create(): ForegroundSession =
            if (Build.VERSION.SDK_INT >= 29)
                Class.forName(
                        "io.github.diegog0477.zombiebox.client.features.playback.platform.Api29ForegroundSession"
                    )
                    .getConstructor()
                    .newInstance() as ForegroundSession
            else LegacyForegroundSession()
    }
}

private class LegacyForegroundSession : ForegroundSession {
    override fun show(
        service: Service,
        notification: Notification,
        receiving: Boolean,
        playing: Boolean,
    ) {
        service.startForeground(1001, notification)
    }
}
