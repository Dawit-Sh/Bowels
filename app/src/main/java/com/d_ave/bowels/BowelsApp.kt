package com.d_ave.bowels

import android.app.Application
import com.d_ave.bowels.data.BowelsDatabase
import com.d_ave.bowels.data.Prefs
import com.d_ave.bowels.data.Repository
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

    override fun onCreate() {
        super.onCreate()
        repository = Repository(BowelsDatabase(this))
        prefs = Prefs(this).also { it.migrateFromAsyncStorage() }
        healthConnect = HealthConnectManager(this)
        watch = WatchConnector(this)
        Notifications.createChannels(this)

        appScope.launch { repository.reload() }
        watchMilestones()
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
                    if (reached.isEmpty()) return@collect
                    reached.forEach { Notifications.showMilestone(this@BowelsApp, it) }
                    prefs.unlockedMilestones = unlocked + reached.map { it.days }
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
