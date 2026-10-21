package com.d_ave.bowels.ui.screens

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.data.InsightItem
import com.d_ave.bowels.data.Screen
import com.d_ave.bowels.data.localDate
import com.d_ave.bowels.domain.healthOptions
import com.d_ave.bowels.domain.quickLogPresets
import com.d_ave.bowels.domain.stoolTypeMeta
import com.d_ave.bowels.health.HealthConnectManager
import com.d_ave.bowels.ui.UiState
import com.d_ave.bowels.ui.components.BCard
import com.d_ave.bowels.ui.components.BText
import com.d_ave.bowels.ui.components.ChipRow
import com.d_ave.bowels.ui.components.ConnectCard
import com.d_ave.bowels.ui.components.FieldLabel
import com.d_ave.bowels.ui.components.GradientButton
import com.d_ave.bowels.ui.components.Pill
import com.d_ave.bowels.ui.components.ScreenColumn
import com.d_ave.bowels.ui.components.SectionTitle
import com.d_ave.bowels.ui.theme.LocalPalette
import com.d_ave.bowels.wear.WatchConnector
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

class HomeActions(
    val setScreen: (Screen) -> Unit,
    val startSession: () -> Unit,
    val quickLog: (Int) -> Unit,
    val saveHealth: (com.d_ave.bowels.data.DailyHealthInput) -> Unit,
    val connectHealth: () -> Unit,
    val installOnWatch: () -> Unit,
    val dismissWatch: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(state: UiState, actions: HomeActions) {
    val p = LocalPalette.current
    val analytics = state.analytics ?: return
    val sessions = state.sessions
    val today = LocalDate.now()
    val counts = sessions.groupingBy { it.session.startTime.localDate() }.eachCount()
    val bars = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { it to (counts[it] ?: 0) }
    val todayCount = bars.last().second
    val weekCount = bars.sumOf { it.second }
    val avg = weekCount / 7.0

    val status = when {
        sessions.isEmpty() -> "Start tracking your bowel health"
        Duration.between(sessions.first().session.startTime, Instant.now()).toHours() > 48 -> "No activity in 2+ days"
        todayCount == 0 && Duration.between(sessions.first().session.startTime, Instant.now()).toHours() < 24 -> "No logs today yet"
        todayCount > 0 && todayCount == avg.roundToInt() -> "Your rhythm is steady today"
        todayCount > 0 && todayCount > avg -> "More active than usual today"
        todayCount > 0 -> "Less active than usual today"
        kotlin.math.abs(weekCount - 7) <= 1 -> "Consistent daily rhythm"
        else -> "Tracking your bowel health"
    }
    val consistency = if (weekCount > 0) minOf(100, (weekCount / 7.0 * 100).roundToInt()) else 0
    val subtitle = when {
        sessions.isEmpty() -> "Begin your journey to better digestive health with quick logging and insights."
        consistency >= 90 -> "$weekCount sessions this week. Excellent tracking consistency!"
        consistency >= 70 -> "$weekCount sessions this week. Good tracking habits."
        consistency >= 50 -> "$weekCount sessions this week. Keep building the habit."
        weekCount > 0 -> "$weekCount session${if (weekCount == 1) "" else "s"} this week. Track daily for better insights."
        else -> "Fast, local-first tracking with quick log, daily health, and predictive insights."
    }

    ScreenColumn {
        SectionTitle(status, subtitle)

        state.insights.firstOrNull()?.let { insight ->
            val edge = when (insight.severity) {
                InsightItem.Severity.High -> p.error
                InsightItem.Severity.Warning -> p.secondary
                InsightItem.Severity.Info -> p.primary
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(edge)) {
                BCard(Modifier.padding(start = 4.dp).fillMaxWidth()) {
                    BText(insight.title, p.onSurface, 20.sp, FontWeight.Bold)
                    BText(insight.body, p.onSurfaceVariant, 14.sp, lineHeight = 20.sp)
                }
            }
        }

        ConnectionCards(state, actions)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Next likely bowel time: ${analytics.predictedNextTimeLabel}", p.primary, p.primary.copy(alpha = 0.08f))
            Pill("Avg duration ${(analytics.averageDurationSeconds / 60.0).roundToInt()}m", p.secondary, p.secondary.copy(alpha = 0.08f))
            analytics.vitals.averageHeartRate?.let { Pill("Avg heart rate $it bpm", p.tertiary, p.tertiary.copy(alpha = 0.08f)) }
        }

        BCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BText("Daily Rhythm", p.primary, 20.sp, FontWeight.Bold)
                BText("Last 7 days", p.onSurfaceVariant, 13.sp, FontWeight.Medium)
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 130.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                bars.forEachIndexed { index, (day, count) ->
                    val active = index == bars.lastIndex
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .width(if (active) 34.dp else 28.dp)
                                .height((34 + minOf(count, 6) * 22).dp)
                                .background(if (active) p.primary else if (count > 0) p.primaryFixedDim.copy(alpha = 0.8f) else p.surfaceContainerHigh, CircleShape),
                        )
                        BText(day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3).uppercase(), if (active) p.primary else p.outline, 10.sp, FontWeight.Bold)
                    }
                }
            }
        }

        GradientButton("Start Session", Icons.Filled.PlayArrow, onClick = actions.startSession)

        BCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BText("Quick Log", p.primary, 20.sp, FontWeight.Bold)
                BText("One tap", p.onSurfaceVariant, 13.sp, FontWeight.Medium)
            }
            quickLogPresets.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (type, title) ->
                        val meta = stoolTypeMeta.getValue(type)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(p.surfaceContainerHigh).clickable { actions.quickLog(type) }.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BText(meta.emoji, p.onSurface, 28.sp)
                            BText(title, p.onSurface, 16.sp, FontWeight.Bold)
                            BText(meta.description, p.onSurfaceVariant, 12.sp, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        BCard(Modifier.fillMaxWidth()) {
            BText("Daily Health Check", p.primary, 20.sp, FontWeight.Bold)
            val health = state.latestHealth
            healthOptions.forEach { (field, options) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel(field.replaceFirstChar { it.uppercase() })
                    ChipRow(options, selected = { health[field] == it }) { actions.saveHealth(health.with(field, it)) }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            QuickLink(Icons.Filled.AutoAwesome, "Weekly Wrapped") { actions.setScreen(Screen.Weekly) }
            QuickLink(Icons.Filled.EmojiEvents, "Milestones") { actions.setScreen(Screen.Badges) }
            QuickLink(Icons.Filled.HealthAndSafety, "Gut health tips") { actions.setScreen(Screen.Health) }
        }
    }
}

@Composable
private fun ConnectionCards(state: UiState, actions: HomeActions) {
    val c = state.connections
    val hcOffer = when {
        c.healthConnectGranted -> null
        c.healthConnect == HealthConnectManager.Status.Available -> "Connect"
        c.healthConnect == HealthConnectManager.Status.NeedsInstall || c.healthConnect == HealthConnectManager.Status.NeedsUpdate -> "Get Health Connect"
        else -> null
    }
    if (hcOffer != null) {
        ConnectCard(
            Icons.Filled.Favorite,
            "See heart rate and blood pressure",
            "Bowels reads what your watch or cuff recorded during each session. It never writes or shares anything.",
            hcOffer,
            actions.connectHealth,
        )
    }
    val watch = c.watch
    if (watch is WatchConnector.State.NeedsInstall && !c.watchPromptDismissed) {
        ConnectCard(
            Icons.Filled.Watch,
            "Track from ${watch.name}",
            "Add Bowels to your watch to log when your phone is in another room. It measures heart rate during the session and syncs on its own.",
            "Install on watch",
            actions.installOnWatch,
            actions.dismissWatch,
        )
    }
}

@Composable
private fun QuickLink(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier.size(80.dp).shadow(4.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).background(p.surfaceContainerLowest).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = p.primary, modifier = Modifier.size(32.dp))
    }
}
