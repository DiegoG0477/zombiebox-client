package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.SystemPlayback

@TargetApi(26)
class Api26PlaybackNotification : PlaybackNotification {
    override fun build(context: Context, state: SystemPlayback, token: Any?): Notification {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                "playback",
                context.getString(R.string.play),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        return Api21PlaybackNotification.content(
            Notification.Builder(context, "playback"),
            context,
            state,
            token,
        )
    }
}
