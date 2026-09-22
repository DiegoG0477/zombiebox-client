package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.annotation.TargetApi
import android.app.Notification
import android.content.Context
import android.media.session.MediaSession
import io.github.diegog0477.zombiebox.client.R
import io.github.diegog0477.zombiebox.client.features.playback.domain.model.SystemPlayback

@TargetApi(21)
class Api21PlaybackNotification : PlaybackNotification {
    override fun build(context: Context, state: SystemPlayback, token: Any?): Notification =
        content(Notification.Builder(context), context, state, token)

    companion object {
        fun content(
            builder: Notification.Builder,
            context: Context,
            state: SystemPlayback,
            token: Any?,
        ): Notification {
            builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(state.title)
                .setContentText(state.subtitle)
                .setContentIntent(PlaybackNotifications.open(context))
                .setOnlyAlertOnce(true)
                .setOngoing(true)
            var actions = 0
            if (state.canPause) {
                builder.addAction(
                    if (state.status == "PLAYING") android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                    context.getString(R.string.play_pause),
                    PlaybackNotifications.action(context, "toggle"),
                )
                actions++
            }
            if (state.canNext) {
                builder.addAction(
                    android.R.drawable.ic_media_next,
                    context.getString(R.string.next_item),
                    PlaybackNotifications.action(context, "next"),
                )
                actions++
            }
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.stop),
                PlaybackNotifications.action(context, "stop"),
            )
            if (token is MediaSession.Token) {
                try {
                    val style = Notification.MediaStyle().setMediaSession(token)
                    if (actions > 0) style.setShowActionsInCompactView(0, actions)
                    else style.setShowActionsInCompactView(0)
                    builder.setStyle(style)
                    return builder.build()
                } catch (_: Exception) {
                    builder.setStyle(null)
                } catch (_: LinkageError) {
                    builder.setStyle(null)
                }
            }
            return builder.build()
        }
    }
}
