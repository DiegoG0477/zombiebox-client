package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.annotation.TargetApi
import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo

@TargetApi(29)
class Api29ForegroundSession : ForegroundSession {
    override fun show(
        service: Service,
        notification: Notification,
        receiving: Boolean,
        playing: Boolean,
    ) {
        val types =
            (if (receiving) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0) or
                (if (playing) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0)
        service.startForeground(1001, notification, types)
    }
}
