package com.d_ave.bowels.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d_ave.bowels.BowelsApp
import com.d_ave.bowels.data.AccentKey
import com.d_ave.bowels.data.AppSettings
import com.d_ave.bowels.data.DailyHealthInput
import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.ImportSummary
import com.d_ave.bowels.data.InsightItem
import com.d_ave.bowels.data.Screen
import com.d_ave.bowels.data.SessionDraft
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.data.ThemeMode
import com.d_ave.bowels.data.today
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.AnalyticsSummary
import com.d_ave.bowels.domain.DemoData
import com.d_ave.bowels.domain.WeeklyWrappedSummary
import com.d_ave.bowels.health.HealthConnectManager
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.wear.WatchConnector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant

data class UiState(
    val loading: Boolean = true,
    val screen: Screen = Screen.Home,
    val sessions: List<SessionWithAnswers> = emptyList(),
    val dailyHealth: List<DailyHealthRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val analytics: AnalyticsSummary? = null,
    val weekly: WeeklyWrappedSummary? = null,
    val insights: List<InsightItem> = emptyList(),
    val draft: SessionDraft = SessionDraft(),
    val usingDemo: Boolean = true,
    val connections: Connections = Connections(),
) {
    val latestHealth: DailyHealthInput get() = dailyHealth.firstOrNull()?.toInput() ?: DailyHealthInput()
    val filledHealthToday: Boolean get() = dailyHealth.firstOrNull()?.day == today().toString()

    /** Your own last check-in for the questions screen; demo answers must never pass as yours. */
    val ownHealth: DailyHealthInput get() = if (usingDemo) DailyHealthInput() else latestHealth
    val ownFilledToday: Boolean get() = !usingDemo && filledHealthToday
}

/** Health Connect and watch status, so screens can offer one-tap fixes instead of sending people to settings. */
data class Connections(
    val healthConnect: HealthConnectManager.Status = HealthConnectManager.Status.Unsupported,
    val healthConnectGranted: Boolean = false,
    val watch: WatchConnector.State = WatchConnector.State.NoWatch,
    val watchPromptDismissed: Boolean = false,
    val fetchingVitals: Boolean = false,
)

private data class Local(val screen: Screen?, val draft: SessionDraft, val connections: Connections)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as BowelsApp
    private val repository = app.repository
    private val prefs = app.prefs

    private val local = MutableStateFlow(
        (prefs.activeDraft ?: SessionDraft()).let { draft -> Local(resumeScreen(draft), draft, Connections(watchPromptDismissed = prefs.watchPromptDismissed)) },
    )

    /**
     * However the app is opened (icon, notification, after being swiped away), an unfinished
     * session comes back where it was: the timer if it's running, the questions if it ended
     * but wasn't saved.
     */
    private fun resumeScreen(draft: SessionDraft): Screen? = when {
        draft.isRunning -> Screen.Active
        draft.startTime != null && draft.endTime != null -> Screen.Questions
        else -> null
    }

    val state: StateFlow<UiState> = combine(repository.data, local) { data, l ->
        if (data == null) return@combine UiState(loading = true, draft = l.draft)
        val demo = !data.settings.hasRealData
        val sessions = if (demo) DemoData.sessions() else data.sessions
        val health = if (demo) DemoData.dailyHealth() else data.dailyHealth
        UiState(
            loading = false,
            screen = l.screen ?: if (data.settings.hasOnboarded) Screen.Home else Screen.Onboarding,
            sessions = sessions,
            dailyHealth = health,
            settings = data.settings,
            analytics = Analytics.build(sessions, health),
            weekly = Analytics.weeklyWrapped(sessions),
            insights = Analytics.insights(sessions, health),
            draft = l.draft,
            usingDemo = demo,
            connections = l.connections,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    private var pendingDestination: String? = null

    init {
        viewModelScope.launch {
            val data = repository.data.value ?: repository.reload()
            if (!data.hasSettingsRows) repository.saveSettings(AppSettings())
            pendingDestination?.let { applyDestination(it) }
            pendingDestination = null
        }
    }

    private fun setDraft(draft: SessionDraft) {
        prefs.activeDraft = draft
        local.update { it.copy(draft = draft) }
    }

    fun setScreen(screen: Screen) = local.update { it.copy(screen = screen) }

    /** Called with the destination a notification carries (see Notifications.openIntent). */
    fun openDestination(destination: String?) {
        destination ?: return
        if (state.value.loading) pendingDestination = destination else applyDestination(destination)
    }

    private fun applyDestination(destination: String) {
        if (destination == Notifications.DESTINATION_FINISH) {
            if (state.value.draft.isRunning) finishSession() else setScreen(Screen.Active)
            return
        }
        val screen = runCatching { Screen.valueOf(destination) }.getOrNull() ?: return
        // A notification can arrive before onboarding is finished; the weekly report still opens.
        setScreen(screen)
    }

    /** Re-checks Health Connect and the watch. Called whenever the app comes to the front. */
    fun refreshConnections() {
        viewModelScope.launch {
            val hc = app.healthConnect
            val status = hc.status()
            val granted = status == HealthConnectManager.Status.Available && hc.hasAnyPermission()
            val watch = app.watch.state()
            local.update { it.copy(connections = it.connections.copy(healthConnect = status, healthConnectGranted = granted, watch = watch)) }
            if (granted) app.enrichRecentVitals()
            val draft = state.value.draft
            if (draft.isRunning) Notifications.showSession(app, draft.startTime!!)
        }
    }

    fun dismissWatchPrompt() {
        prefs.watchPromptDismissed = true
        local.update { it.copy(connections = it.connections.copy(watchPromptDismissed = true)) }
    }

    fun installOnWatch() {
        val watch = state.value.connections.watch as? WatchConnector.State.NeedsInstall ?: return
        viewModelScope.launch { app.watch.openInstallOnWatch(watch.nodeId) }
    }

    private suspend fun persistSettings(next: AppSettings) {
        repository.saveSettings(next)
        Notifications.scheduleReminders(app, next.reminderHour, next.notificationsEnabled)
    }

    fun startSession() {
        val draft = SessionDraft(startTime = Instant.now())
        setDraft(draft)
        setScreen(Screen.Active)
        Notifications.showSession(app, draft.startTime!!)
    }

    fun finishSession() {
        val current = state.value.draft
        val start = current.startTime ?: return
        val end = Instant.now()
        setDraft(current.copy(endTime = end, durationSeconds = maxOf(1, end.epochSecond - start.epochSecond)))
        Notifications.dismissSession(app)
        setScreen(Screen.Questions)
        fetchDraftVitals()
    }

    fun cancelSession() {
        setDraft(SessionDraft())
        Notifications.dismissSession(app)
        setScreen(Screen.Home)
    }

    fun updateDraft(transform: (SessionDraft) -> SessionDraft) {
        val next = transform(state.value.draft)
        setDraft(next.copy(adaptiveTags = next.adaptiveTags.take(5)))
    }

    fun setKind(kind: SessionKind) = updateDraft { d ->
        val bowel = kind == SessionKind.Bowel
        d.copy(
            kind = kind,
            stoolType = if (bowel) d.stoolType ?: 4 else null,
            pain = if (bowel) d.pain ?: "none" else null,
            blood = if (bowel) d.blood ?: false else null,
            urgency = if (bowel) d.urgency ?: 3 else null,
            completion = if (bowel) d.completion ?: "normal" else null,
            urineUrgency = if (!bowel) d.urineUrgency ?: "medium" else null,
            urineColor = if (!bowel) d.urineColor ?: "normal" else null,
        )
    }

    /** Pulls heart rate and blood pressure for the session that just ended, without asking. */
    fun fetchDraftVitals() {
        val draft = state.value.draft
        val start = draft.startTime ?: return
        val end = draft.endTime ?: Instant.now()
        viewModelScope.launch {
            local.update { it.copy(connections = it.connections.copy(fetchingVitals = true)) }
            val found = app.healthConnect.vitalsAround(start, end)
            local.update { it.copy(connections = it.connections.copy(fetchingVitals = false)) }
            if (!found.isEmpty) updateDraft { it.copy(vitals = it.vitals.mergeMissing(found)) }
        }
    }

    fun setManualVitals(heartRate: Int?, systolic: Int?, diastolic: Int?) = updateDraft { d ->
        var v = d.vitals
        if (heartRate != null && heartRate != v.heartRateAvg) v = v.copy(heartRateAvg = heartRate, heartRateMin = null, heartRateMax = null, heartRateSource = VitalsKeys.SOURCE_MANUAL)
        if (heartRate == null && v.heartRateSource == VitalsKeys.SOURCE_MANUAL) v = v.copy(heartRateAvg = null, heartRateSource = null)
        v = if (systolic != null && diastolic != null) {
            if (systolic != v.systolic || diastolic != v.diastolic) v.copy(systolic = systolic, diastolic = diastolic, bloodPressureTime = Instant.now(), bloodPressureSource = VitalsKeys.SOURCE_MANUAL) else v
        } else if (v.bloodPressureSource == VitalsKeys.SOURCE_MANUAL) {
            v.copy(systolic = null, diastolic = null, bloodPressureTime = null, bloodPressureSource = null)
        } else v
        d.copy(vitals = v)
    }

    fun saveQuestions(health: DailyHealthInput?) {
        viewModelScope.launch {
            app.markHasRealData()
            if (health != null) repository.upsertDailyHealth(today().toString(), health)
            val draft = state.value.draft
            val vitalsDone = !draft.vitals.isEmpty
            val id = repository.saveSession(draft)
            if (vitalsDone) repository.putAnswers(id, mapOf(VitalsKeys.VITALS_CHECKED to "true"))
            setDraft(SessionDraft())
            setScreen(Screen.History)
        }
    }

    fun saveDailyHealth(input: DailyHealthInput) {
        viewModelScope.launch {
            app.markHasRealData()
            repository.upsertDailyHealth(today().toString(), input)
        }
    }

    fun quickLog(stoolType: Int) {
        viewModelScope.launch {
            app.markHasRealData()
            val start = Instant.now()
            repository.saveSession(SessionDraft(stoolType = stoolType, startTime = start, endTime = start.plusSeconds(60), durationSeconds = 60))
            app.enrichRecentVitals()
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { persistSettings(transform(state.value.settings)) }
    }

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }
    fun setAccent(accent: AccentKey) = updateSettings { it.copy(accent = accent) }
    fun setReminderHour(hour: Int) = updateSettings { it.copy(reminderHour = hour) }

    fun completeOnboarding() {
        viewModelScope.launch {
            persistSettings(state.value.settings.copy(hasOnboarded = true))
            setScreen(Screen.Home)
        }
    }

    suspend fun exportJson(): String = repository.exportArchive().toString(2)

    suspend fun importJson(content: String): ImportSummary {
        val summary = repository.importArchive(JSONObject(content))
        setScreen(Screen.History)
        app.enrichRecentVitals()
        return summary
    }

    fun resetToDemo() {
        viewModelScope.launch {
            repository.clearAllData()
            prefs.unlockedMilestones = emptySet()
            persistSettings(state.value.settings.copy(hasRealData = false))
            setScreen(Screen.Home)
        }
    }
}
