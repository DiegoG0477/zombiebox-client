package io.github.diegog0477.zombiebox.client.features.playback.platform

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager

/** Notification denial does not authorize stopping or restarting the media session. */
@TargetApi(33)
object Api33NotificationPermission {
    @JvmStatic
    fun request(activity: Activity) {
        val preferences = activity.getSharedPreferences("zombie", Context.MODE_PRIVATE)
        if (
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED ||
                preferences.getBoolean("notificationPermissionRequested", false)
        )
            return
        preferences.edit().putBoolean("notificationPermissionRequested", true).commit()
        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 3301)
    }
}
