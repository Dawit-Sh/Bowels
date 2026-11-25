package com.d_ave.bowels

import android.app.Application
import com.d_ave.bowels.dashboard.DashboardSync
import com.d_ave.bowels.data.BowelsDatabase
import com.d_ave.bowels.data.Prefs
import com.d_ave.bowels.data.Repository
import com.d_ave.bowels.domain.Achievements
import com.d_ave.bowels.widget.BowelsWidgets
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.milestones
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.wear.WatchConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BowelsApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var repository: Repository
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var watch: WatchConnector
        private set
    lateinit var dashboard: DashboardSync
        private set

    private companion object {
        const val SEEDED = "_seeded"
        const val NEW_MILESTONES_SEEDED = -32
    }

    override fun onCreate() {
        super.onCreate()
        repository = Repository(BowelsDatabase(this))
        prefs = Prefs(this).also { it.migrateFromAsyncStorage() }
        watch = WatchConnector(this)
        dashboard = DashboardSync(this)
        Notifications.createChannels(this)

        appScope.launch { repository.reload() }
        watchMilestones()
        keepWidgetsFresh()
        keepDashboardFed()
    }

    /**
     * Once a dashboard is paired: what's new goes a few seconds after any change (a log on the phone
     * or the watch, vitals filled in later, an import) and when the app starts, with an hourly fallback.
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun keepDashboardFed() {
        appScope.launch {
            repository.data.filterNotNull().debounce(1500).collect {
                if (!dashboard.isPaired) return@collect
                DashboardSync.schedule(this@BowelsApp)
                dashboard.refresh()
                if (dashboard.status.value.waiting > 0) DashboardSync.kick(this@BowelsApp)
            }
        }
    }

    /** Widgets redraw a moment after the data changes (a log, an import, a new theme). */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun keepWidgetsFresh() {
        appScope.launch {
            repository.data.filterNotNull().drop(1).debounce(800).collect {
                runCatching { BowelsWidgets.refresh(this@BowelsApp) }
            }
        }
    }

    suspend fun markHasRealData() {
        val current = repository.data.value?.settings ?: repository.reload().settings
        if (!current.hasRealData) repository.saveSettings(current.copy(hasRealData = true, hasOnboarded = true))
    }

    /** Announces milestones as soon as they are reached, whether the log came from the phone or the watch. */
    private fun watchMilestones() {
        appScope.launch {
            repository.data.filterNotNull()
                .map { if (it.settings.hasRealData) Analytics.build(it.sessions).milestoneProgressDays else null }
                .distinctUntilChanged()
                .collect { days ->
                    if (days == null) return@collect
                    val unlocked = prefs.unlockedMilestones
                    val reached = milestones.filter { days >= it.days && it.days !in unlocked }
                    // 3.2 added milestones (3, 14, 60, 180 days); ones already passed before the update are recorded quietly.
                    if (unlocked.isNotEmpty() && NEW_MILESTONES_SEEDED !in unlocked) {
                        prefs.unlockedMilestones = unlocked + reached.map { it.days } + NEW_MILESTONES_SEEDED
                        return@collect
                    }
                    if (reached.isEmpty()) return@collect
                    // A history arriving at once (a backup import) passes several: announce only the highest.
                    Notifications.showMilestone(this@BowelsApp, reached.maxBy { it.days })
                    prefs.unlockedMilestones = unlocked + reached.map { it.days } + NEW_MILESTONES_SEEDED
                }
        }
        appScope.launch {
            repository.data.filterNotNull()
                .map { if (it.settings.hasRealData) Achievements.progress(it.sessions).filter { a -> a.earned }.map { a -> a.achievement } else null }
                .distinctUntilChanged()
                .collect { earned ->
                    if (earned == null) return@collect
                    val known = prefs.unlockedAchievements
                    // The first time (an update from 3.1 with history), record what's already earned without a burst of notifications.
                    if (SEEDED !in known) {
                        prefs.unlockedAchievements = known + SEEDED + earned.map { it.id }
                        return@collect
                    }
                    val fresh = earned.filter { it.id !in known }
                    fresh.take(2).forEach { Notifications.showAchievement(this@BowelsApp, it) }
                    if (fresh.isNotEmpty()) prefs.unlockedAchievements = known + fresh.map { it.id }
                }
        }
    }
}
