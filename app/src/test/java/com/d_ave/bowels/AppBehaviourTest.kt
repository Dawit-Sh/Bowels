package com.d_ave.bowels

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.d_ave.bowels.data.AccentKey
import com.d_ave.bowels.data.Screen
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.DemoData
import com.d_ave.bowels.domain.milestones
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.notify.ReminderReceiver
import com.d_ave.bowels.share.ShareCards
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.shared.WatchSessionPayload
import com.d_ave.bowels.ui.AppViewModel
import com.d_ave.bowels.ui.theme.buildPalette
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppBehaviourTest {
    private val app = ApplicationProvider.getApplicationContext<BowelsApp>()

    @Test
    fun `weekly report notification opens Weekly Wrapped`() {
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        ReminderReceiver().onReceive(app, Intent(ReminderReceiver.ACTION_WEEKLY))

        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = shadowOf(manager).allNotifications.single()
        val tapped = shadowOf(posted.contentIntent).savedIntent
        assertEquals(Screen.Weekly.name, tapped.getStringExtra(Notifications.EXTRA_DESTINATION))
        assertEquals(MainActivity::class.java.name, tapped.component?.className)

        // And the app actually lands there when that intent arrives.
        val vm = AppViewModel(app)
        waitUntil { !vm.state.value.loading }
        vm.openDestination(tapped.getStringExtra(Notifications.EXTRA_DESTINATION))
        waitUntil { vm.state.value.screen == Screen.Weekly }
        assertEquals(Screen.Weekly, vm.state.value.screen)
    }

    @Test
    fun `a notification tapped while the app is still loading is not lost`() {
        val vm = AppViewModel(app)
        vm.openDestination(Screen.Weekly.name)
        waitUntil { !vm.state.value.loading && vm.state.value.screen == Screen.Weekly }
        assertEquals(Screen.Weekly, vm.state.value.screen)
    }

    @Test
    fun `a notification tapped on a cold start still opens its screen`() = runBlocking {
        // The data finished loading before the activity passed the notification on,
        // which is the order that used to drop the tap.
        app.repository.saveSettings(com.d_ave.bowels.data.AppSettings(hasOnboarded = true, hasRealData = true))
        val vm = AppViewModel(app)
        vm.openDestination(Screen.Weekly.name)
        waitUntil { vm.state.value.screen == Screen.Weekly }
        assertEquals(Screen.Weekly, vm.state.value.screen)
    }

    @Test
    fun `a running session survives the app being closed`() {
        // Started 7 minutes ago, then the app was swiped away (the process is gone, the prefs stay).
        val start = java.time.Instant.now().minusSeconds(7 * 60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
        app.prefs.activeDraft = com.d_ave.bowels.data.SessionDraft(startTime = start)

        // Opening it again from the icon, with no notification extra, lands on the timer.
        val vm = AppViewModel(app)
        waitUntil { !vm.state.value.loading }
        assertEquals(Screen.Active, vm.state.value.screen)
        assertEquals(start, vm.state.value.draft.startTime)

        // "Finish Session" on the notification finishes that same session, with the full time.
        vm.openDestination(Notifications.DESTINATION_FINISH)
        waitUntil { vm.state.value.screen == Screen.Questions }
        assertEquals(Screen.Questions, vm.state.value.screen)
        assertTrue(vm.state.value.draft.durationSeconds >= 7 * 60)

        // Closed again before saving: it comes back to the questions, not a blank Home.
        val again = AppViewModel(app)
        waitUntil { !again.state.value.loading }
        assertEquals(Screen.Questions, again.state.value.screen)
    }

    @Test
    fun `a restart brings the running session's notification back`() {
        shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        app.prefs.activeDraft = com.d_ave.bowels.data.SessionDraft(startTime = java.time.Instant.now().minusSeconds(120))
        com.d_ave.bowels.notify.BootReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = shadowOf(manager).allNotifications.single()
        assertEquals("Session running", posted.extras.getString(android.app.Notification.EXTRA_TITLE))
        assertTrue(posted.extras.getBoolean(android.app.Notification.EXTRA_SHOW_CHRONOMETER))
    }

    @Test
    fun `watch sessions are saved once with their heart rate`() = runBlocking {
        val payload = WatchSessionPayload(
            id = "watch-1", kind = "bowel", startTime = "2026-10-21T07:30:00.000Z", endTime = "2026-10-21T07:36:00.000Z",
            durationSeconds = 360, answers = mapOf("stool_type" to "4", "pain" to "none", "blood" to "false"),
            heartRateAvg = 78, heartRateMin = 70, heartRateMax = 96,
        )
        val roundTripped = WatchSessionPayload.fromJson(payload.toJson())
        assertEquals(payload, roundTripped)

        assertTrue(app.repository.saveWatchSession(roundTripped))
        assertFalse("a resend must not duplicate", app.repository.saveWatchSession(roundTripped))
        val saved = app.repository.data.value!!.sessions.single()
        assertEquals(78, saved.vitals.heartRateAvg)
        assertEquals(96, saved.vitals.heartRateMax)
        assertEquals(VitalsKeys.SOURCE_WATCH, saved.vitals.heartRateSource)
        assertEquals(4, saved.stoolType)
    }

    @Test
    fun `backups from the Expo app import, including the legacy format`() = runBlocking {
        val current = JSONObject(
            """{"sessions":[{"id":7,"kind":"bowel","startTime":"2026-10-01T08:00:00.000Z","endTime":"2026-10-01T08:05:00.000Z","durationSeconds":300,"createdAt":"2026-10-01T08:05:00.000Z"}],
               "answers":[{"sessionId":7,"key":"stool_type","value":"3"}],
               "dailyHealth":[{"day":"2026-10-01","water":"Great","fiber":"High","meals":"Balanced","stress":"Low","sleep":"Great","exercise":"Light","createdAt":"x","updatedAt":"x"}],
               "settings":[{"key":"accent","value":"rose","updatedAt":"x"}]}""",
        )
        val first = app.repository.importArchive(current)
        assertEquals(1, first.importedSessions)
        assertEquals(1, first.importedHealthDays)
        val again = app.repository.importArchive(current)
        assertEquals(0, again.importedSessions)
        assertEquals(1, again.skippedSessions)

        val legacy = JSONObject(
            """{"sessions":[{"id":1,"type":"bowel","start_time":"2025-01-02T09:00:00.000Z","end_time":"2025-01-02T09:03:00.000Z","duration_seconds":180}],
               "session_answers":[{"session_id":1,"question_key":"bowel_stool_type","value":"5"},{"session_id":1,"question_key":"bowel_blood","value":"yes"}],
               "settings":[{"key":"app_settings_v1","value":"{\"accent\":\"orange\",\"dailyReminderTime\":\"21:30\"}"}]}""",
        )
        assertEquals(1, app.repository.importArchive(legacy).importedSessions)
        val data = app.repository.data.value!!
        val old = data.sessions.first { it.session.startTime.toString().startsWith("2025-01-02") }
        assertEquals("5", old.answers["stool_type"])
        assertEquals("true", old.answers["blood"])
        assertEquals(AccentKey.Coral, data.settings.accent)
        assertEquals(21, data.settings.reminderHour)
        assertTrue(data.settings.hasRealData)
    }

    @Test
    fun `share images are a true 1080x1920 story, light and dark`() {
        val cards = ShareCards(app)
        val weekly = Analytics.weeklyWrapped(DemoData.sessions())
        val out = File("build/share-previews").apply { mkdirs() }
        for ((name, dark) in listOf("light" to false, "dark" to true)) {
            val palette = buildPalette(dark, AccentKey.Olive)
            val bitmaps = mapOf(
                "weekly-$name" to cards.weeklyWrapped(weekly, palette),
                "milestone-$name" to cards.milestone(milestones[1], 9, palette),
            )
            bitmaps.forEach { (file, bitmap) ->
                assertEquals(1080, bitmap.width)
                assertEquals(1920, bitmap.height)
                File(out, "$file.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        assertNotNull(File(out, "weekly-light.png").takeIf { it.length() > 20_000 })
    }

    private fun waitUntil(condition: () -> Boolean) {
        repeat(200) {
            ShadowLooper.idleMainLooper()
            if (condition()) return
            Thread.sleep(20)
        }
    }
}
