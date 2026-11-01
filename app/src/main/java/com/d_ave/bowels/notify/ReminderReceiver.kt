package com.d_ave.bowels.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.d_ave.bowels.BowelsApp
import com.d_ave.bowels.data.Prefs
import com.d_ave.bowels.data.localDate
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DAILY -> {
                val app = context.applicationContext as BowelsApp
                val pending = goAsync()
                app.appScope.launch {
                    try {
                        dailyReminderText(app)?.let { Notifications.showDailyReminder(context, it) }
                        Notifications.rescheduleFromSaved(context)
                    } finally {
                        pending.finish()
                    }
                }
                return
            }
            ACTION_WEEKLY -> Notifications.showWeeklyReport(context)
        }
        // Alarms fire once; queue the next one.
        Notifications.rescheduleFromSaved(context)
    }

    /** What's still missing today, or null when there's nothing to remind about. */
    private suspend fun dailyReminderText(app: BowelsApp): String? {
        val data = app.repository.data.value ?: app.repository.reload()
        val today = LocalDate.now()
        val logged = data.settings.hasRealData && data.sessions.any { it.session.startTime.localDate() == today }
        val checked = data.settings.hasRealData && data.dailyHealth.firstOrNull()?.day == today.toString()
        return when {
            logged && checked -> null
            logged -> "Today's health check is still open. Ten taps."
            checked -> "No visit logged today yet. Tap to log one when it happens."
            else -> "Log today's rhythm and health check in under a minute."
        }
    }

    companion object {
        const val ACTION_DAILY = "com.d_ave.bowels.REMINDER_DAILY"
        const val ACTION_WEEKLY = "com.d_ave.bowels.REMINDER_WEEKLY"

        fun pendingIntent(context: Context, action: String, index: Int = 0): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode() + index,
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
