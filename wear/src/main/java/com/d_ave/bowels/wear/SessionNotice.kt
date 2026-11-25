package com.d_ave.bowels.wear

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status

/**
 * Shows a running session as an ongoing activity on the watch face, with its stopwatch, so it
 * can be finished from there. The session itself lives in [WatchSession] and survives without it.
 */
object SessionNotice {
    private const val CHANNEL = "bowels-watch-session"
    private const val NOTIFICATION_ID = 7

    fun show(context: Context, startMillis: Long) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Running session", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(context, 0, Intent(context, WearActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Session running")
            .setContentText("Tap to finish")
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setUsesChronometer(true)
            .setWhen(startMillis)

        // The stopwatch on the watch face counts from boot time, not wall-clock time.
        val zero = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startMillis)
        OngoingActivity.Builder(context.applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_notification)
            .setTouchIntent(open)
            .setStatus(Status.Builder().addTemplate("#time#").addPart("time", Status.StopwatchPart(zero)).build())
            .build()
            .apply(context.applicationContext)
        // Without the notification permission this shows nothing; the session still runs.
        runCatching { manager.notify(NOTIFICATION_ID, builder.build()) }
    }

    fun hide(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
