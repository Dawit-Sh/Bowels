package com.d_ave.bowels.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.d_ave.bowels.BowelsApp
import com.d_ave.bowels.MainActivity
import com.d_ave.bowels.data.AccentKey
import com.d_ave.bowels.data.SessionDraft
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.ThemeMode
import com.d_ave.bowels.data.localDate
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.Outlook
import com.d_ave.bowels.domain.Rhythm
import com.d_ave.bowels.domain.stoolTypeMeta
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.ui.theme.Palette
import com.d_ave.bowels.ui.theme.buildPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Colors that follow the app's theme and the phone's light or dark mode. */
private class WidgetColors(light: Palette, dark: Palette) {
    val bg = ColorProvider(light.surfaceContainerLowest, dark.surfaceContainerLow)
    val tile = ColorProvider(light.surfaceContainerHigh, dark.surfaceContainerHigh)
    val text = ColorProvider(light.onSurface, dark.onSurface)
    val muted = ColorProvider(light.onSurfaceVariant, dark.onSurfaceVariant)
    val accent = ColorProvider(light.primary, dark.primaryFixedDim)
    val accentSoft = ColorProvider(light.primaryFixedDim, dark.primary)
    val onAccent = ColorProvider(Color.White, Color.White)
    val button = ColorProvider(light.primary, dark.primary)
}

/** What every widget needs, read straight from the app's data. */
private class WidgetData(
    val colors: WidgetColors,
    val hasData: Boolean,
    val running: SessionDraft?,
    val nextLabel: String,
    val today: Int,
    val streak: Int,
    val outlook: Outlook?,
    val week: List<Pair<LocalDate, Int>>,
    val weekVisits: Int,
    val weekMinutes: Int,
    val weekSteady: Int?,
) {
    companion object {
        suspend fun load(context: Context): WidgetData {
            val app = context.applicationContext as BowelsApp
            val stored = app.repository.data.value ?: app.repository.reload()
            val s = stored.settings
            val dynamicOk = s.accent == AccentKey.Wallpaper && Build.VERSION.SDK_INT >= 31
            val light = buildPalette(false, s.accent, s.background, if (dynamicOk) dynamicLightColorScheme(context) else null)
            val dark = buildPalette(true, s.accent, s.background, if (dynamicOk) dynamicDarkColorScheme(context) else null)
            // Follow the app's Light / Dark choice; "System" follows the phone.
            val colors = when (s.themeMode) {
                ThemeMode.Light -> WidgetColors(light, light)
                ThemeMode.Dark -> WidgetColors(dark, dark)
                ThemeMode.System -> WidgetColors(light, dark)
            }
            val real = s.hasRealData
            val sessions = if (real) stored.sessions else emptyList()
            val visits = Rhythm.distinct(sessions).filter { it.session.kind == SessionKind.Bowel }
            val today = LocalDate.now()
            val analytics = if (real && visits.isNotEmpty()) Analytics.build(sessions, stored.dailyHealth) else null
            val week = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { d -> d to visits.count { it.session.startTime.localDate() == d } }
            val weekly = visits.filter { it.session.startTime.localDate() > today.minusDays(7) }
            val typed = weekly.mapNotNull { it.stoolType }
            return WidgetData(
                colors = colors,
                hasData = real && visits.isNotEmpty(),
                running = app.prefs.activeDraft?.takeIf { it.isRunning },
                nextLabel = analytics?.predictedNextTimeLabel ?: "Log a few visits",
                today = week.last().second,
                streak = analytics?.patterns?.currentStreak ?: 0,
                outlook = analytics?.forecast?.outlook,
                week = week,
                weekVisits = weekly.size,
                weekMinutes = weekly.takeIf { it.isNotEmpty() }?.let { w -> (w.sumOf { it.session.durationSeconds } / 60.0 / w.size).roundToInt() } ?: 0,
                weekSteady = typed.takeIf { it.isNotEmpty() }?.let { t -> (100.0 * t.count { it in 3..4 } / t.size).roundToInt() },
            )
        }
    }
}

private fun open(context: Context, destination: String) = Intent(context, MainActivity::class.java).apply {
    action = "com.d_ave.bowels.WIDGET.$destination"
    putExtra(Notifications.EXTRA_DESTINATION, destination)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
}

@Composable
private fun Card(c: WidgetColors, onClick: Intent? = null, content: @Composable () -> Unit) {
    var m = GlanceModifier.fillMaxSize().background(c.bg).cornerRadius(24.dp).padding(14.dp)
    if (onClick != null) m = m.clickable(actionStartActivity(onClick))
    Box(m) { content() }
}

@Composable
private fun Label(text: String, c: WidgetColors) =
    Text(text.uppercase(), style = TextStyle(color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold))

private val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).withZone(ZoneId.systemDefault())

private val Version = longPreferencesKey("bowels_widget_version")

/**
 * Glance keeps a widget's composition alive for a while after an update, and a second update
 * in that time only recomposes it: data loaded once in provideGlance would stay stale (the
 * session widget kept saying "running" after Finish). [BowelsWidgets.refresh] bumps a version
 * in the widget's state, which reloads the data here.
 */
@Composable
private fun rememberData(context: Context, first: WidgetData): WidgetData {
    val version = currentState(Version) ?: 0L
    var data by remember { mutableStateOf(first) }
    LaunchedEffect(version) { if (version != 0L) data = WidgetData.load(context) }
    return data
}

/** Start or finish a session, or quick-log a visit, without opening the app. */
class SessionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val first = WidgetData.load(context)
        provideContent {
            val d = rememberData(context, first)
            val c = d.colors
            Card(c) {
                Column(GlanceModifier.fillMaxSize()) {
                    val running = d.running
                    if (running != null) {
                        Label("Session running", c)
                        Text("Since ${clock.format(running.startTime)}", style = TextStyle(color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        Spacer(GlanceModifier.defaultWeight())
                        Box(
                            GlanceModifier.fillMaxWidth().height(44.dp).background(c.button).cornerRadius(16.dp)
                                .clickable(actionStartActivity(open(context, Notifications.DESTINATION_FINISH))),
                            contentAlignment = Alignment.Center,
                        ) { Text("Finish session", style = TextStyle(color = c.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)) }
                    } else {
                        Box(
                            GlanceModifier.fillMaxWidth().height(44.dp).background(c.button).cornerRadius(16.dp)
                                .clickable(actionStartActivity(open(context, Notifications.DESTINATION_START))),
                            contentAlignment = Alignment.Center,
                        ) { Text("▶  Start session", style = TextStyle(color = c.onAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)) }
                        Spacer(GlanceModifier.height(10.dp))
                        Label("Quick log", c)
                        Spacer(GlanceModifier.height(4.dp))
                        Row(GlanceModifier.fillMaxWidth()) {
                            listOf(1, 3, 4, 6).forEachIndexed { i, type ->
                                if (i > 0) Spacer(GlanceModifier.width(6.dp))
                                Box(
                                    GlanceModifier.defaultWeight().height(40.dp).background(c.tile).cornerRadius(12.dp)
                                        .clickable(actionRunCallback<QuickLogAction>(actionParametersOf(QuickLogAction.Type to type))),
                                    contentAlignment = Alignment.Center,
                                ) { Text(stoolTypeMeta.getValue(type).emoji, style = TextStyle(fontSize = 18.sp)) }
                            }
                        }
                        Spacer(GlanceModifier.defaultWeight())
                        if (d.hasData) {
                            Text("Next: ${d.nextLabel}", style = TextStyle(color = c.muted, fontSize = 11.sp), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Next likely time, today's count, your streak and tomorrow's outlook. */
class RhythmWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val first = WidgetData.load(context)
        provideContent {
            val d = rememberData(context, first)
            val c = d.colors
            Card(c, onClick = open(context, "Analytics")) {
                Column(GlanceModifier.fillMaxSize()) {
                    Label("Next likely", c)
                    Text(d.nextLabel, style = TextStyle(color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold), maxLines = 2)
                    Spacer(GlanceModifier.defaultWeight())
                    Row(GlanceModifier.fillMaxWidth()) {
                        Stat("${d.today}", "today", c)
                        Spacer(GlanceModifier.width(8.dp))
                        Stat("${d.streak}", "day streak", c)
                        Spacer(GlanceModifier.width(8.dp))
                        Stat(
                            when (d.outlook) { Outlook.Steady -> "Steady"; Outlook.Looser -> "Looser"; Outlook.Harder -> "Harder"; null -> "—" },
                            "tomorrow", c,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.glance.layout.RowScope.Stat(value: String, label: String, c: WidgetColors) {
    Column(GlanceModifier.defaultWeight().background(c.tile).cornerRadius(12.dp).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(value, style = TextStyle(color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1)
        Text(label, style = TextStyle(color = c.muted, fontSize = 10.sp), maxLines = 1)
    }
}

/** The last seven days as bars, with this week's visits, typical length and Type 3-4 share. */
class WeekWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val first = WidgetData.load(context)
        provideContent {
            val d = rememberData(context, first)
            val c = d.colors
            Card(c, onClick = open(context, "Weekly")) {
                Column(GlanceModifier.fillMaxSize()) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Column(GlanceModifier.defaultWeight()) {
                            Label("Last 7 days", c)
                            Text("${d.weekVisits} visits", style = TextStyle(color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${d.weekMinutes} min avg", style = TextStyle(color = c.muted, fontSize = 12.sp))
                            Text(d.weekSteady?.let { "$it% Type 3-4" } ?: "No types yet", style = TextStyle(color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    val top = (d.week.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        d.week.forEachIndexed { i, (day, n) ->
                            Column(GlanceModifier.defaultWeight().padding(horizontal = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    GlanceModifier.fillMaxWidth().height((8 + 64 * n / top).dp).cornerRadius(6.dp)
                                        .background(if (i == d.week.lastIndex) c.accent else if (n > 0) c.accentSoft else c.tile),
                                ) {}
                                Text(day.dayOfWeek.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault()), style = TextStyle(color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Saves a one-tap visit from the widget, like Quick Log in the app. */
class QuickLogAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val type = parameters[Type] ?: return
        val app = context.applicationContext as BowelsApp
        app.markHasRealData()
        val start = Instant.now()
        app.repository.saveSession(SessionDraft(stoolType = type, startTime = start, endTime = start.plusSeconds(60), durationSeconds = 60))
        app.enrichRecentVitals()
        BowelsWidgets.refresh(context)
    }

    companion object {
        val Type = ActionParameters.Key<Int>("stool_type")
    }
}

class SessionWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = SessionWidget() }
class RhythmWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = RhythmWidget() }
class WeekWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = WeekWidget() }

object BowelsWidgets {
    /** Redraws every Bowels widget on the home screen with fresh data (cheap when there are none). */
    suspend fun refresh(context: Context) {
        val manager = GlanceAppWidgetManager(context)
        val widgets = listOf(SessionWidget(), RhythmWidget(), WeekWidget())
        for (widget in widgets) {
            for (id in manager.getGlanceIds(widget.javaClass)) {
                updateAppWidgetState(context, id) { it[Version] = System.currentTimeMillis() }
                widget.update(context, id)
            }
        }
    }
}
