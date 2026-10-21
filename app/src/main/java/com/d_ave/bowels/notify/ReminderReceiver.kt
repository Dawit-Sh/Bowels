package com.d_ave.bowels.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.d_ave.bowels.data.Prefs

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DAILY -> Notifications.showDailyReminder(context)
            ACTION_WEEKLY -> Notifications.showWeeklyReport(context)
        }
        // Alarms fire once; queue the next one.
        Notifications.rescheduleFromSaved(context)
    }

    companion object {
        const val ACTION_DAILY = "com.d_ave.bowels.REMINDER_DAILY"
        const val ACTION_WEEKLY = "com.d_ave.bowels.REMINDER_WEEKLY"

        fun pendingIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, ReminderReceiver::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

/** Alarms and notifications are wiped on reboot and on app update; put them back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifications.rescheduleFromSaved(context)
        // A session that was running before the restart keeps its notification and its time.
        Prefs(context).activeDraft?.takeIf { it.isRunning }?.startTime?.let { Notifications.showSession(context, it) }
    }
}
