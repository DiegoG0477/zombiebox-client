package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import io.github.diegog0477.zombiebox.client.R

@TargetApi(26)
class Api26PlaybackNotification : PlaybackNotification {
    override fun build(context: Context, title: String, playing: Boolean): Notification {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                "playback",
                context.getString(R.string.play),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        return Notification.Builder(context, "playback")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentIntent(PlaybackNotifications.open(context))
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.play_pause),
                PlaybackNotifications.action(context, "toggle"),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                context.getString(R.string.next_item),
                PlaybackNotifications.action(context, "next"),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.stop),
                PlaybackNotifications.action(context, "stop"),
            )
            .build()
    }
}
