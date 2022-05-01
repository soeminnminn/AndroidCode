package com.s16.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat


class NotificationHelper(context: Context) : ContextWrapper(context) {

    /**
     * Method checks if the app is in background or not
     *
     * @param context
     * @return
     */
    @Suppress("DEPRECATION")
    @SuppressLint("ObsoleteSdkInt")
    fun isAppIsInBackground(): Boolean {
        var isInBackground = true
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            val runningProcesses = am.runningAppProcesses
            for (processInfo in runningProcesses) {
                if (processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (activeProcess in processInfo.pkgList) {
                        if (activeProcess == packageName) {
                            isInBackground = false
                        }
                    }
                }
            }
        } else {
            val taskInfo = am.getRunningTasks(1)
            val componentInfo = taskInfo[0].topActivity
            if (componentInfo!!.packageName == packageName) {
                isInBackground = false
            }
        }

        return isInBackground
    }

    private fun createNotification(title: String, message: String, intent: Intent,
                                   @DrawableRes smallIcon: Int, @DrawableRes icon: Int): Notification {
        val resultPendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )

        val inboxStyle = NotificationCompat.InboxStyle()

        val builder = NotificationCompat.Builder(this, NotificationCompat.EXTRA_CHANNEL_ID)
        return builder
            .setSmallIcon(smallIcon)
            .setTicker(title)
            .setWhen(0)
            .setAutoCancel(true)
            .setContentTitle(title)
            .setStyle(inboxStyle)
            .setContentIntent(resultPendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setLargeIcon(BitmapFactory.decodeResource(resources, icon))
            .setContentText(message)
            .build()
    }

    fun showNotification(title: String, message: String, intent: Intent,
                         @DrawableRes smallIcon: Int, @DrawableRes icon: Int) {

        if (message.isEmpty()) {
            return
        }

        if (isAppIsInBackground()) {
            val notification: Notification = createNotification(title, message, intent, smallIcon, icon)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
        } else {
            intent.putExtra("title", title)
            intent.putExtra("message", message)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

    }

    companion object {
        private const val NOTIFICATION_ID = 100
    }
}