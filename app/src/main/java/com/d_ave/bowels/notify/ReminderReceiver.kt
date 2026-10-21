package com.d_ave.bowels.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

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

/** Alarms are wiped on reboot and on app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifications.rescheduleFromSaved(context)
    }
}
