package com.d_ave.bowels.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.d_ave.bowels.BowelsApp
import com.d_ave.bowels.data.AccentKey
import com.d_ave.bowels.data.AppSettings
import com.d_ave.bowels.data.BackgroundKey
import com.d_ave.bowels.data.ImportSummary
import com.d_ave.bowels.data.InsightItem
import com.d_ave.bowels.data.Screen
import com.d_ave.bowels.data.SessionDraft
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.data.ThemeMode
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.AnalyticsSummary
import com.d_ave.bowels.domain.DemoData
import com.d_ave.bowels.domain.PatternAnalysis
import com.d_ave.bowels.domain.WeeklyWrappedSummary
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.wear.WatchConnector
import com.d_ave.bowels.widget.BowelsWidgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant

data class UiState(
    val loading: Boolean = true,
    val screen: Screen = Screen.Home,
    val sessions: List<SessionWithAnswers> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val analytics: AnalyticsSummary? = null,
    val weekly: WeeklyWrappedSummary? = null,
    val insights: List<InsightItem> = emptyList(),
    val draft: SessionDraft = SessionDraft(),
    val usingDemo: Boolean = true,
    val connections: Connections = Connections(),
    val sessionDetail: com.d_ave.bowels.domain.SessionDetail? = null,
    val guidance: List<com.d_ave.bowels.domain.Advice> = emptyList(),
    val achievements: List<com.d_ave.bowels.domain.AchievementProgress> = emptyList(),
)

/** Watch status, so screens can offer one-tap fixes instead of sending people to settings. */
data class Connections(
    val watch: WatchConnector.State = WatchConnector.State.NoWatch,
    val watchPromptDismissed: Boolean = false,
    val checkingWatch: Boolean = false,
)

private data class Local(val screen: Screen?, val draft: SessionDraft, val connections: Connections, val selectedSession: Long? = null)

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
        UiState(
            loading = false,
            screen = l.screen ?: if (data.settings.hasOnboarded) Screen.Home else Screen.Onboarding,
            sessions = sessions,
            settings = data.settings,
            analytics = Analytics.build(sessions),
            weekly = Analytics.weeklyWrapped(sessions),
            insights = Analytics.insights(sessions),
            draft = l.draft,
            usingDemo = demo,
            connections = l.connections,
            achievements = if (l.screen == Screen.Badges) com.d_ave.bowels.domain.Achievements.progress(sessions) else emptyList(),
            guidance = if (l.screen == Screen.Health) com.d_ave.bowels.domain.Guidance.forYou(sessions, PatternAnalysis.build(sessions)) else emptyList(),
            sessionDetail = l.selectedSession?.let { id ->
                sessions.firstOrNull { it.id == id }?.let { com.d_ave.bowels.domain.SessionDetails.build(it, sessions) }
            },
        )
    }.flowOn(Dispatchers.Default) // the rhythm model runs here; keep it off the main thread
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        viewModelScope.launch {
            val data = repository.data.value ?: repository.reload()
            if (!data.hasSettingsRows) repository.saveSettings(AppSettings())
        }
    }

    private fun setDraft(draft: SessionDraft) {
        val wasRunning = local.value.draft.isRunning
        prefs.activeDraft = draft
        local.update { it.copy(draft = draft) }
        // The session widget shows Start or Finish.
        if (wasRunning != draft.isRunning) app.appScope.launch { BowelsWidgets.refresh(app) }
    }

    fun setScreen(screen: Screen) = local.update { it.copy(screen = screen) }

    fun openSession(id: Long) = local.update { it.copy(screen = Screen.Session, selectedSession = id) }

    /** Called with the destination a notification carries (see Notifications.openIntent). */
    fun openDestination(destination: String?) {
        destination ?: return
        // Wait until the stored data is on screen, whichever finishes first: loading, or this
        // call. On a cold start from a notification either order happens, and checking
        // "loading" once used to drop the tap (the weekly report opened Home).
        viewModelScope.launch {
            state.first { !it.loading }
            applyDestination(destination)
        }
    }

    private fun applyDestination(destination: String) {
        if (destination == Notifications.DESTINATION_FINISH) {
            if (state.value.draft.isRunning) finishSession() else setScreen(Screen.Active)
            return
        }
        if (destination == Notifications.DESTINATION_START) {
            if (state.value.draft.isRunning) setScreen(Screen.Active) else startSession()
            return
        }
        val screen = runCatching { Screen.valueOf(destination) }.getOrNull() ?: return
        // A notification can arrive before onboarding is finished; the weekly report still opens.
        setScreen(screen)
    }

    /** Re-checks the watch. Called whenever the app comes to the front. */
    fun refreshConnections() {
        viewModelScope.launch {
            val watch = app.watch.state(prefs.knownWatches)
            local.update { it.copy(connections = it.connections.copy(watch = watch)) }
            val draft = state.value.draft
            if (draft.isRunning) Notifications.showSession(app, draft.startTime!!)
        }
    }

    fun dismissWatchPrompt() {
        prefs.watchPromptDismissed = true
        local.update { it.copy(connections = it.connections.copy(watchPromptDismissed = true)) }
    }

    /** "It's already on my watch": trust it, and stop offering the install. */
    fun markWatchInstalled() {
        val watch = state.value.connections.watch as? WatchConnector.State.NeedsInstall ?: return
        prefs.knownWatches = prefs.knownWatches + watch.nodeId
        local.update { it.copy(connections = it.connections.copy(watch = WatchConnector.State.Ready(watch.name))) }
    }

    /** Looks for the watch again, for the "Check again" button. */
    fun recheckWatch() {
        viewModelScope.launch {
            local.update { it.copy(connections = it.connections.copy(checkingWatch = true)) }
            val watch = app.watch.state(prefs.knownWatches)
            local.update { it.copy(connections = it.connections.copy(watch = watch, checkingWatch = false)) }
        }
    }

    fun installOnWatch() {
        val watch = state.value.connections.watch as? WatchConnector.State.NeedsInstall ?: return
        viewModelScope.launch { app.watch.openInstallOnWatch(watch.nodeId) }
    }

    private suspend fun persistSettings(next: AppSettings) {
        repository.saveSettings(next)
        Notifications.scheduleReminders(app, next.reminderTimes, next.notificationsEnabled)
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
    }

    fun cancelSession() {
        setDraft(SessionDraft())
        Notifications.dismissSession(app)
        setScreen(Screen.Home)
    }

    fun updateDraft(transform: (SessionDraft) -> SessionDraft) = setDraft(transform(state.value.draft))

    fun saveQuestions() {
        viewModelScope.launch {
            app.markHasRealData()
            repository.saveSession(state.value.draft)
            setDraft(SessionDraft())
            setScreen(Screen.History)
        }
    }

    fun quickLog(stoolType: Int) {
        viewModelScope.launch {
            app.markHasRealData()
            val start = Instant.now()
            repository.saveSession(SessionDraft(stoolType = stoolType, startTime = start, endTime = start.plusSeconds(60), durationSeconds = 60))
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { persistSettings(transform(state.value.settings)) }
    }

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }
    fun setAccent(accent: AccentKey) = updateSettings { it.copy(accent = accent) }
    fun setBackground(background: BackgroundKey) = updateSettings { it.copy(background = background) }
    fun setReminderTimes(times: List<java.time.LocalTime>) =
        updateSettings { it.copy(reminderTimes = times.distinct().sorted().take(AppSettings.MAX_REMINDERS)) }
    fun setRemindersOn(on: Boolean) = updateSettings { it.copy(notificationsEnabled = on) }

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
