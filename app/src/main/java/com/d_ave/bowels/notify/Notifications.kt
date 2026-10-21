package com.d_ave.bowels.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.d_ave.bowels.MainActivity
import com.d_ave.bowels.R
import com.d_ave.bowels.data.Screen
import com.d_ave.bowels.domain.Milestone
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Every notification opens the screen it is about. The old app only routed taps on the
 * session notification, so the weekly report notification opened Home (or nothing at all).
 */
object Notifications {
    const val CHANNEL_REMINDERS = "bowels-reminders"
    const val CHANNEL_SESSION = "bowels-session"
    const val CHANNEL_MILESTONES = "bowels-milestones"

    private const val ID_DAILY = 1001
    private const val ID_WEEKLY = 1002
    private const val ID_SESSION = 1003
    private const val ID_MILESTONE_BASE = 2000

    const val EXTRA_DESTINATION = "destination"
    const val DESTINATION_FINISH = "finish_session"

    // The weekly report goes out on Saturday at 18:00, as before.
    private val WEEKLY_DAY = DayOfWeek.SATURDAY
    private val WEEKLY_TIME: LocalTime = LocalTime.of(18, 0)

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, "Reminders and weekly report", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_SESSION, "Running session", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
                NotificationChannel(CHANNEL_MILESTONES, "Milestones", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun canPost(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Opens the app on [destination]. Each destination gets its own PendingIntent. */
    fun openIntent(context: Context, destination: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.d_ave.bowels.OPEN.$destination"
            putExtra(EXTRA_DESTINATION, destination)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the post.
        }
    }

    private fun base(context: Context, channel: String) =
        NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF56642B.toInt())

    fun showDailyReminder(context: Context) = post(
        context, ID_DAILY,
        base(context, CHANNEL_REMINDERS)
            .setContentTitle("Bowels check-in")
            .setContentText("Log today's rhythm and health check in under a minute.")
            .setContentIntent(openIntent(context, Screen.Home.name, ID_DAILY))
            .setAutoCancel(true),
    )

    fun showWeeklyReport(context: Context) = post(
        context, ID_WEEKLY,
        base(context, CHANNEL_REMINDERS)
            .setContentTitle("Weekly report ready")
            .setContentText("Your Weekly Wrapped is available. Tap to see your week.")
            .setContentIntent(openIntent(context, Screen.Weekly.name, ID_WEEKLY))
            .setAutoCancel(true),
    )

    fun showMilestone(context: Context, milestone: Milestone) = post(
        context, ID_MILESTONE_BASE + milestone.days,
        base(context, CHANNEL_MILESTONES)
            .setContentTitle("🎉 Milestone Unlocked: ${milestone.title}")
            .setContentText("${milestone.days} day${if (milestone.days == 1) "" else "s"} achieved! ${milestone.body}")
            .setContentIntent(openIntent(context, Screen.Badges.name, ID_MILESTONE_BASE + milestone.days))
            .setAutoCancel(true),
    )

    /** One ongoing notification with a live system chronometer; no re-posting every 10 seconds. */
    fun showSession(context: Context, startTime: Instant) = post(
        context, ID_SESSION,
        base(context, CHANNEL_SESSION)
            .setContentTitle("Session running")
            .setContentText("Tap Finish Session to complete and answer questions.")
            .setWhen(startTime.toEpochMilli())
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openIntent(context, Screen.Active.name, ID_SESSION))
            .addAction(0, "Finish Session", openIntent(context, DESTINATION_FINISH, ID_SESSION + 1)),
    )

    fun dismissSession(context: Context) = NotificationManagerCompat.from(context).cancel(ID_SESSION)

    fun showWatchSessionSaved(context: Context, durationSeconds: Long, heartRate: Int?) = post(
        context, ID_SESSION + 10,
        base(context, CHANNEL_SESSION)
            .setContentTitle("Session from your watch saved")
            .setContentText(buildString {
                append("${durationSeconds / 60}m ${durationSeconds % 60}s")
                if (heartRate != null) append(" · heart rate $heartRate bpm")
            })
            .setContentIntent(openIntent(context, Screen.History.name, ID_SESSION + 10))
            .setAutoCancel(true),
    )

    // Scheduling. Inexact alarms are enough for a check-in and need no special permission.

    private fun schedulePrefs(context: Context) = context.getSharedPreferences("bowels-schedule", Context.MODE_PRIVATE)

    fun scheduleReminders(context: Context, hour: Int, enabled: Boolean = true) {
        schedulePrefs(context).edit().putInt("hour", hour).putBoolean("enabled", enabled).apply()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val daily = ReminderReceiver.pendingIntent(context, ReminderReceiver.ACTION_DAILY)
        val weekly = ReminderReceiver.pendingIntent(context, ReminderReceiver.ACTION_WEEKLY)
        alarms.cancel(daily)
        alarms.cancel(weekly)
        if (!enabled) return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextDaily(hour).toInstant().toEpochMilli(), daily)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextWeekly().toInstant().toEpochMilli(), weekly)
    }

    fun rescheduleFromSaved(context: Context) {
        val prefs = schedulePrefs(context)
        if (prefs.contains("hour")) scheduleReminders(context, prefs.getInt("hour", 20), prefs.getBoolean("enabled", true))
    }

    private fun nextDaily(hour: Int): ZonedDateTime {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val today = now.toLocalDate().atTime(hour, 0).atZone(now.zone)
        return if (today.isAfter(now.plusMinutes(1))) today else today.plusDays(1)
    }

    private fun nextWeekly(): ZonedDateTime {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val date: LocalDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(WEEKLY_DAY))
        val candidate = date.atTime(WEEKLY_TIME).atZone(now.zone)
        return if (candidate.isAfter(now.plusMinutes(1))) candidate else candidate.plusWeeks(1)
    }
}
