package com.d_ave.bowels

import android.app.Application
import com.d_ave.bowels.data.BowelsDatabase
import com.d_ave.bowels.data.Prefs
import com.d_ave.bowels.data.Repository
import com.d_ave.bowels.domain.Achievements
import com.d_ave.bowels.widget.BowelsWidgets
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.milestones
import com.d_ave.bowels.health.HealthConnectManager
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.wear.WatchConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant

class BowelsApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var repository: Repository
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var healthConnect: HealthConnectManager
        private set
    lateinit var watch: WatchConnector
        private set

    private val enrichLock = Mutex()

    private companion object {
        const val SEEDED = "_seeded"
        const val NEW_MILESTONES_SEEDED = -32
    }

    override fun onCreate() {
        super.onCreate()
        repository = Repository(BowelsDatabase(this))
        prefs = Prefs(this).also { it.migrateFromAsyncStorage() }
        healthConnect = HealthConnectManager(this)
        watch = WatchConnector(this)
        Notifications.createChannels(this)

        appScope.launch { repository.reload() }
        watchMilestones()
        keepWidgetsFresh()
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
                .map { if (it.settings.hasRealData) Analytics.build(it.sessions, it.dailyHealth).milestoneProgressDays else null }
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
                .map { if (it.settings.hasRealData) Achievements.progress(it.sessions, it.dailyHealth).filter { a -> a.earned }.map { a -> a.achievement } else null }
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

    /**
     * Looks up heart rate and blood pressure in Health Connect for recent sessions that don't
     * have them yet. Runs when the app opens, so vitals show up without anyone asking.
     */
    fun enrichRecentVitals() {
        appScope.launch {
            enrichLock.withLock {
                if (!healthConnect.hasAnyPermission()) return@launch
                val since = Instant.now().minus(Duration.ofDays(7))
                val pending = repository.data.value?.sessions.orEmpty().filter {
                    it.session.startTime.isAfter(since) && it.answers[VitalsKeys.VITALS_CHECKED] == null
                }
                // Watches and cuffs sync to Health Connect late, so keep looking for a few hours.
                val settled = Instant.now().minus(Duration.ofHours(6))
                for (session in pending) {
                    val found = healthConnect.vitalsAround(session.session.startTime, session.session.endTime)
                    val done = session.session.endTime.isBefore(settled)
                    if (found.isEmpty && !done) continue
                    val merged = session.vitals.mergeMissing(found)
                    repository.putAnswers(session.id, merged.toAnswers() + if (done) mapOf(VitalsKeys.VITALS_CHECKED to "true") else emptyMap())
                }
            }
        }
    }
}
