package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import io.github.diegog0477.zombiebox.client.MainActivity
import io.github.diegog0477.zombiebox.client.R

interface PlaybackNotification {
    fun build(context: Context, title: String, playing: Boolean): Notification
}

object PlaybackNotifications {
    fun requestPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            Class.forName(
                    "io.github.diegog0477.zombiebox.client.features.playback.platform.Api33NotificationPermission"
                )
                .getMethod("request", Activity::class.java)
                .invoke(null, activity)
        }
    }

    fun create(): PlaybackNotification {
        if (Build.VERSION.SDK_INT >= 26) {
            return Class.forName(
                    "io.github.diegog0477.zombiebox.client.features.playback.platform.Api26PlaybackNotification"
                )
                .getConstructor()
                .newInstance() as PlaybackNotification
        }
        return LegacyPlaybackNotification()
    }

    fun action(context: Context, command: String): PendingIntent =
        PendingIntent.getService(
            context,
            command.hashCode(),
            Intent(context, PlaybackService::class.java).setAction(command),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0,
        )

    fun open(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0,
        )
}

@Suppress("DEPRECATION")
class LegacyPlaybackNotification : PlaybackNotification {
    override fun build(context: Context, title: String, playing: Boolean): Notification {
        val views = RemoteViews(context.packageName, R.layout.notification_playback)
        views.setTextViewText(R.id.notification_title, title)
        views.setTextViewText(R.id.notification_toggle, context.getString(R.string.play_pause))
        views.setOnClickPendingIntent(
            R.id.notification_toggle,
            PlaybackNotifications.action(context, "toggle"),
        )
        views.setOnClickPendingIntent(
            R.id.notification_next,
            PlaybackNotifications.action(context, "next"),
        )
        views.setOnClickPendingIntent(
            R.id.notification_stop,
            PlaybackNotifications.action(context, "stop"),
        )
        return Notification(R.drawable.ic_launcher, title, System.currentTimeMillis()).apply {
            contentView = views
            contentIntent = PlaybackNotifications.open(context)
            flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_ONLY_ALERT_ONCE
        }
    }
}
