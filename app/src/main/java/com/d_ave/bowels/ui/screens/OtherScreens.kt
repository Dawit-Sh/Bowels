package com.d_ave.bowels.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Spacer
import com.d_ave.bowels.domain.AchievementGroup
import com.d_ave.bowels.domain.AchievementProgress
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalUriHandler
import com.d_ave.bowels.domain.Advice
import com.d_ave.bowels.domain.Guidance
import com.d_ave.bowels.domain.HealthTip
import com.d_ave.bowels.domain.Research
import com.d_ave.bowels.ui.components.PaperLink
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.d_ave.bowels.data.BackgroundKey
import com.d_ave.bowels.ui.components.FieldLabel
import com.d_ave.bowels.ui.theme.accentSwatch
import com.d_ave.bowels.ui.theme.backgroundSwatch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.R
import com.d_ave.bowels.data.AccentKey
import com.d_ave.bowels.data.AppSettings
import com.d_ave.bowels.data.ThemeMode
import com.d_ave.bowels.domain.Milestone
import com.d_ave.bowels.domain.MilestoneTone
import com.d_ave.bowels.domain.finalMilestone
import com.d_ave.bowels.domain.healthInfo
import com.d_ave.bowels.domain.milestones
import com.d_ave.bowels.domain.nextMilestone
import com.d_ave.bowels.health.HealthConnectManager
import com.d_ave.bowels.ui.Connections
import com.d_ave.bowels.ui.components.BCard
import com.d_ave.bowels.ui.components.BText
import com.d_ave.bowels.ui.components.ChipRow
import com.d_ave.bowels.ui.components.GradientButton
import com.d_ave.bowels.ui.components.ScreenColumn
import com.d_ave.bowels.ui.components.SectionTitle
import com.d_ave.bowels.ui.theme.LocalPalette
import com.d_ave.bowels.wear.WatchConnector

@Composable
fun BadgesScreen(progressDays: Int, achievements: List<AchievementProgress>, onShare: (Milestone) -> Unit) {
    val p = LocalPalette.current
    val next = nextMilestone(progressDays)
    val progress = (progressDays.toFloat() / next.days).coerceIn(0f, 1f)
    ScreenColumn(gap = 24.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                BText("CURRENT RHYTHM", p.onSurfaceVariant.copy(alpha = 0.8f), 13.sp, FontWeight.Medium, letterSpacing = 1.2.sp)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BText(progressDays.toString(), p.primary, 56.sp, FontWeight.ExtraBold, letterSpacing = (-2).sp)
                    BText(if (progressDays == 1) "day" else "days", p.primary.copy(alpha = 0.6f), 24.sp, FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BText("Next: ${next.title}", p.onSurfaceVariant, 13.sp, FontWeight.Medium)
                BText("${maxOf(0, next.days - progressDays)} days to go", p.primary, 14.sp, FontWeight.Bold)
            }
        }
        Box(Modifier.fillMaxWidth().height(16.dp).clip(CircleShape).background(p.surfaceContainerHighest).padding(4.dp)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(p.primary))
        }

        val earnedCount = achievements.count { it.earned }
        Row(verticalAlignment = Alignment.Bottom) {
            BText("Achievements", p.onSurface, 26.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            BText("$earnedCount of ${achievements.size}", p.primary, 14.sp, FontWeight.Bold)
        }
        AchievementGroup.entries.forEach { group ->
            val items = achievements.filter { it.achievement.group == group }
            if (items.isEmpty()) return@forEach
            BText(group.label.uppercase(), p.onSurfaceVariant, 11.sp, FontWeight.Bold, letterSpacing = 1.4.sp)
            items.sortedByDescending { it.earned }.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { AchievementTile(it, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            BText("Day milestones", p.onSurface, 26.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            BText("${milestones.count { progressDays >= it.days }} of ${milestones.size}", p.primary, 14.sp, FontWeight.Bold)
        }
        val nextIndex = milestones.indexOfFirst { progressDays < it.days }
        milestones.forEachIndexed { index, m ->
            MilestoneCard(m, earned = progressDays >= m.days, next = index == nextIndex, progressDays = progressDays, onShare = { onShare(m) })
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(48.dp)).background(p.surfaceContainerHigh.copy(alpha = 0.67f)).padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.size(192.dp).background(Brush.verticalGradient(listOf(p.tertiary.copy(alpha = 0.33f), p.tertiary.copy(alpha = 0.13f))), CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(150.dp).alpha(0.55f))
            }
            BText(finalMilestone.title, p.onSurfaceVariant, 34.sp, FontWeight.ExtraBold, align = TextAlign.Center)
            BText(finalMilestone.body, p.onSurfaceVariant.copy(alpha = 0.8f), 16.sp, align = TextAlign.Center, lineHeight = 24.sp)
        }
    }
}

private fun milestoneIcon(days: Int): ImageVector = when (days) {
    1, 3 -> Icons.Filled.Spa
    7, 14 -> Icons.Filled.Waves
    30, 60 -> Icons.Filled.Favorite
    else -> Icons.Filled.EmojiEvents
}

@Composable
private fun AchievementTile(a: AchievementProgress, modifier: Modifier) {
    val p = LocalPalette.current
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(if (a.earned) p.surfaceContainerLowest else p.surfaceContainerLow)
            .border(if (a.earned) 2.dp else 0.dp, if (a.earned) p.primary.copy(alpha = 0.35f) else Color.Transparent, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BText(a.achievement.emoji, p.onSurface, 28.sp, modifier = Modifier.alpha(if (a.earned) 1f else 0.4f).weight(1f))
            if (!a.earned) Icon(Icons.Filled.Lock, null, tint = p.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
        }
        BText(a.achievement.title, if (a.earned) p.onSurface else p.onSurfaceVariant, 15.sp, FontWeight.Bold, maxLines = 1)
        BText(a.achievement.body, p.onSurfaceVariant, 12.sp, lineHeight = 16.sp)
        if (a.earned) {
            BText("EARNED", p.primary, 10.sp, FontWeight.ExtraBold, letterSpacing = 1.sp)
        } else {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(p.surfaceContainerHighest)) {
                Box(Modifier.fillMaxWidth(a.fraction).fillMaxHeight().clip(CircleShape).background(p.primary))
            }
            BText("${minOf(a.current, a.achievement.target)} / ${a.achievement.target}", p.onSurfaceVariant, 10.sp, FontWeight.Bold)
        }
    }
}

@Composable
private fun MilestoneCard(m: Milestone, earned: Boolean, next: Boolean, progressDays: Int, onShare: () -> Unit) {
    val p = LocalPalette.current
    val accent = if (m.tone == MilestoneTone.Secondary) p.secondary else p.primary
    val transition = rememberInfiniteTransition(label = "badge")
    val pulse by transition.animateFloat(1f, if (earned) 1.05f else 1f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "pulse")
    val spin by transition.animateFloat(0f, if (earned) 360f else 0f, infiniteRepeatable(tween(8000)), label = "spin")
    val gradient = when {
        !earned -> listOf(p.surfaceContainerHighest, p.surfaceContainerHigh)
        m.tone == MilestoneTone.Secondary -> listOf(p.secondaryFixed, p.secondary)
        else -> listOf(p.primaryFixedDim, p.primaryContainer)
    }

    Column(
        Modifier.fillMaxWidth().alpha(if (earned) 1f else if (next) 0.72f else 0.52f)
            .clip(RoundedCornerShape(40.dp)).background(if (earned) p.surfaceContainerLowest else p.surfaceContainerLow).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (!earned) Icon(Icons.Filled.Lock, null, tint = p.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp).align(Alignment.Start))
        Box(
            Modifier.size(128.dp).scale(pulse).background(Brush.verticalGradient(gradient), CircleShape).border(4.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(milestoneIcon(m.days), null, tint = if (earned) accent else p.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(64.dp).rotate(spin))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            BText(m.title, if (earned) p.onSurface else p.onSurfaceVariant, 22.sp, FontWeight.Bold, align = TextAlign.Center)
            BText(m.body, p.onSurfaceVariant, 14.sp, align = TextAlign.Center, lineHeight = 20.sp)
        }
        Box(Modifier.clip(CircleShape).background(if (earned) accent.copy(alpha = 0.08f) else p.surfaceContainerHighest).padding(horizontal = 16.dp, vertical = 8.dp)) {
            BText(
                when {
                    earned -> "${m.days} Day${if (m.days == 1) "" else "s"} Achieved"
                    next -> "Locked ($progressDays/${m.days})"
                    else -> "${m.days} Days Goal"
                }.uppercase(),
                if (earned) accent else p.onSurfaceVariant.copy(alpha = 0.55f), 11.sp, FontWeight.Bold, letterSpacing = 1.1.sp,
            )
        }
        if (earned) {
            Row(
                Modifier.clip(CircleShape).clickable(onClick = onShare).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Share, null, tint = accent, modifier = Modifier.size(18.dp))
                BText("Share achievement", accent, 14.sp, FontWeight.Bold)
            }
        }
    }
}

/** Gut Practices: advice written from your own logs first, then the general practices, each with its research. */
@Composable
fun HealthInfoScreen(advice: List<Advice>) {
    val p = LocalPalette.current
    val uri = LocalUriHandler.current
    ScreenColumn {
        SectionTitle("Gut Practices", "Read from your own logs, with the research behind each one. Tap a study to read it.")
        BText("FOR YOU", p.onSurfaceVariant, 12.sp, FontWeight.Bold, letterSpacing = 1.5.sp)
        advice.forEach { a ->
            BCard(Modifier.fillMaxWidth(), color = if (a.urgent) p.error.copy(alpha = 0.08f) else null) {
                BText(a.title, if (a.urgent) p.error else p.onSurface, 20.sp, FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BText("WHAT WE NOTICED", p.primary, 10.sp, FontWeight.Bold, letterSpacing = 1.sp)
                    BText(a.noticed, p.onSurface, 14.sp, lineHeight = 20.sp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BText("WHAT YOU COULD TRY", p.primary, 10.sp, FontWeight.Bold, letterSpacing = 1.sp)
                    a.tryThis.forEach { tip ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BText("•", p.primary, 14.sp, FontWeight.Bold)
                            BText(tip, p.onSurface, 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
                a.papers.forEach { PaperLink(it) }
                BText(
                    "Find more studies on PubMed", p.primary, 13.sp, FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).clickable { runCatching { uri.openUri(Research.pubmedSearch(a.topic)) } }.padding(vertical = 4.dp),
                )
            }
        }
        BText(
            "Bowels isn't a doctor. If something worries you, or blood, weight loss or pain keeps coming back, see one.",
            p.onSurfaceVariant, 12.sp, lineHeight = 17.sp,
        )
        BText("GOOD PRACTICES", p.onSurfaceVariant, 12.sp, FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 8.dp))
        healthInfo.forEach { tip -> PracticeCard(tip) }
    }
}

@Composable
private fun PracticeCard(tip: HealthTip) {
    val p = LocalPalette.current
    var open by rememberSaveable(tip.id) { mutableStateOf(false) }
    BCard(Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BText(tip.title, p.onSurface, 18.sp, FontWeight.Bold)
                BText(tip.summary, p.primary, 13.sp, FontWeight.Bold, lineHeight = 18.sp)
            }
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = p.outline)
        }
        if (open) {
            BText(tip.body, p.onSurfaceVariant, 14.sp, lineHeight = 21.sp)
            Guidance.practicePapers[tip.id].orEmpty().forEach { PaperLink(it) }
        }
    }
}

class SettingsActions(
    val setTheme: (ThemeMode) -> Unit,
    val setAccent: (AccentKey) -> Unit,
    val setBackground: (BackgroundKey) -> Unit,
    val setReminderTimes: (List<LocalTime>) -> Unit,
    val setRemindersOn: (Boolean) -> Unit,
    val connectHealth: () -> Unit,
    val installOnWatch: () -> Unit,
    val checkUpdates: () -> Unit,
    val recheckWatch: () -> Unit,
    val markWatchInstalled: () -> Unit,
)

private val reminderFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

@Composable
fun SettingsScreen(settings: AppSettings, connections: Connections, version: String, actions: SettingsActions) {
    val p = LocalPalette.current
    // null: closed. -1: adding a new time. Otherwise the index being changed.
    var editing by remember { mutableStateOf<Int?>(null) }
    ScreenColumn {
        SectionTitle("Settings", "Look, reminders and connections. Everything stays on this phone.")
        BCard(Modifier.fillMaxWidth()) {
            BText("Theme", p.onSurface, 20.sp, FontWeight.Bold)
            ChipRow(ThemeMode.entries, { settings.themeMode == it }, { it.key }, actions.setTheme)
            FieldLabel("Background")
            SwatchRow(
                BackgroundKey.entries, { settings.background == it }, { it.label },
                { backgroundSwatch(it, p.dark) }, actions.setBackground,
            )
            FieldLabel("Accent")
            val accents = AccentKey.entries.filter { it != AccentKey.Wallpaper || Build.VERSION.SDK_INT >= 31 }
            SwatchRow(accents, { settings.accent == it }, { it.label }, { if (it == AccentKey.Wallpaper) p.primary else accentSwatch(it) }, actions.setAccent)
            if (settings.accent == AccentKey.Wallpaper) {
                BText("Uses the colors of your wallpaper, and changes with it.", p.onSurfaceVariant, 12.sp)
            }
        }
        BCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BText("Daily reminders", p.onSurface, 20.sp, FontWeight.Bold)
                    BText(
                        if (settings.notificationsEnabled) "A reminder is skipped when you've already logged a visit and done the health check that day."
                        else "Off. The weekly report is off too.",
                        p.onSurfaceVariant, 13.sp, lineHeight = 18.sp,
                    )
                }
                Switch(checked = settings.notificationsEnabled, onCheckedChange = actions.setRemindersOn)
            }
            if (settings.notificationsEnabled) {
                settings.reminderTimes.forEachIndexed { index, time ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.surfaceContainerHigh).clickable { editing = index }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Alarm, null, tint = p.primary, modifier = Modifier.size(20.dp))
                        BText(time.format(reminderFormat), p.onSurface, 18.sp, FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
                        Icon(
                            Icons.Filled.Close, "Remove", tint = p.onSurfaceVariant,
                            modifier = Modifier.size(32.dp).clip(CircleShape).clickable { actions.setReminderTimes(settings.reminderTimes - time) }.padding(6.dp),
                        )
                    }
                }
                if (settings.reminderTimes.size < AppSettings.MAX_REMINDERS) {
                    Row(
                        Modifier.clip(CircleShape).clickable { editing = -1 }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, tint = p.primary, modifier = Modifier.size(18.dp))
                        BText(if (settings.reminderTimes.isEmpty()) "Add a reminder" else "Add another reminder", p.primary, 14.sp, FontWeight.Bold)
                    }
                }
            }
        }
        BCard(Modifier.fillMaxWidth()) {
            BText("Connections", p.onSurface, 20.sp, FontWeight.Bold)
            ConnectionRow(
                Icons.Filled.Favorite, "Health Connect",
                when {
                    connections.healthConnectGranted -> "Connected. Heart rate and blood pressure fill in on their own."
                    connections.healthConnect == HealthConnectManager.Status.Unsupported -> "Not available on this phone."
                    connections.healthConnect == HealthConnectManager.Status.Available -> "Not connected yet."
                    else -> "Not installed yet."
                },
                action = if (!connections.healthConnectGranted && connections.healthConnect != HealthConnectManager.Status.Unsupported) "Connect" else null,
                onAction = actions.connectHealth,
            )
            val watch = connections.watch
            ConnectionRow(
                Icons.Filled.Watch, "Watch",
                when {
                    connections.checkingWatch -> "Looking for your watch..."
                    watch is WatchConnector.State.Ready -> "${watch.name} is ready. Sessions logged there sync here."
                    watch is WatchConnector.State.NeedsInstall -> "${watch.name} is paired but Bowels wasn't found on it."
                    else -> "No Wear OS watch found. Is it connected to this phone?"
                },
                action = if (watch is WatchConnector.State.NeedsInstall) "Install" else null,
                onAction = actions.installOnWatch,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                BText("Check again", p.primary, 13.sp, FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable(onClick = actions.recheckWatch).padding(vertical = 6.dp))
                if (watch is WatchConnector.State.NeedsInstall) {
                    BText("It's already installed", p.primary, 13.sp, FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable(onClick = actions.markWatchInstalled).padding(vertical = 6.dp))
                }
            }
        }
        BCard(Modifier.fillMaxWidth()) {
            BText("App Updates", p.onSurface, 20.sp, FontWeight.Bold)
            BText("You're on $version. Check GitHub for the latest version.", p.onSurfaceVariant, lineHeight = 20.sp)
            GradientButton("Check for Updates", Icons.Filled.SystemUpdate, onClick = actions.checkUpdates)
        }
    }

    editing?.let { index ->
        val initial = settings.reminderTimes.getOrNull(index) ?: LocalTime.of(20, 0)
        ReminderTimeDialog(initial, onDismiss = { editing = null }) { picked ->
            val times = settings.reminderTimes.toMutableList()
            if (index in times.indices) times[index] = picked else times += picked
            actions.setReminderTimes(times)
            editing = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val p = LocalPalette.current
    val state = rememberTimePickerState(initial.hour, initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { BText("Reminder time", p.onSurface, 20.sp, FontWeight.Bold) },
        text = {
            TimePicker(
                state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = p.surfaceContainerHigh,
                    selectorColor = p.primary,
                    timeSelectorSelectedContainerColor = p.primary.copy(alpha = 0.18f),
                    timeSelectorUnselectedContainerColor = p.surfaceContainerHigh,
                    periodSelectorSelectedContainerColor = p.primary.copy(alpha = 0.18f),
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) { BText("Save", p.primary, weight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { BText("Cancel", p.onSurfaceVariant, weight = FontWeight.Bold) } },
    )
}

/** Round color swatches with their names, the chosen one ringed. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SwatchRow(options: List<T>, selected: (T) -> Boolean, label: (T) -> String, color: (T) -> Color, onSelect: (T) -> Unit) {
    val p = LocalPalette.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            val active = selected(option)
            Column(
                Modifier.width(58.dp).clip(RoundedCornerShape(16.dp)).clickable { onSelect(option) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.size(40.dp)
                        .border(if (active) 3.dp else 1.dp, if (active) p.primary else p.outline.copy(alpha = 0.35f), CircleShape)
                        .padding(if (active) 5.dp else 1.dp)
                        .background(color(option), CircleShape),
                )
                BText(label(option), if (active) p.primary else p.onSurfaceVariant, 11.sp, if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ConnectionRow(icon: ImageVector, title: String, body: String, action: String?, onAction: () -> Unit) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = p.primary)
        Column(Modifier.weight(1f)) {
            BText(title, p.onSurface, 15.sp, FontWeight.Bold)
            BText(body, p.onSurfaceVariant, 13.sp, lineHeight = 18.sp)
        }
        if (action != null) {
            BText(action, p.onPrimary, 13.sp, FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(p.primary).clickable(onClick = onAction).padding(horizontal = 14.dp, vertical = 8.dp))
        }
    }
}

@Composable
fun OnboardingScreen(onBegin: () -> Unit, onImport: () -> Unit) {
    val p = LocalPalette.current
    val features = listOf(
        Triple(Icons.Filled.VisibilityOff, "Discrete Tracking", "Elegant, quiet entries that blend into your daily flow."),
        Triple(Icons.Filled.Favorite, "Local-First Privacy", "Your data never leaves your device. Total ownership."),
        Triple(Icons.Filled.Watch, "Works With Your Watch", "Log from your wrist and see heart rate and blood pressure from around each session."),
        Triple(Icons.Filled.Spa, "Gentle Insights", "Meaningful patterns shared without judgment."),
    )
    ScreenColumn(gap = 24.dp) {
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.mipmap.ic_launcher_foreground), null, Modifier.size(96.dp))
            BText("Bowels", p.primary, 22.sp, FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BText("Welcome to Bowels.", p.onSurface, 38.sp, FontWeight.ExtraBold, align = TextAlign.Center, lineHeight = 44.sp)
            BText("A private, serene space to understand your body's natural rhythm.", p.onSurfaceVariant, 18.sp, align = TextAlign.Center, lineHeight = 28.sp)
        }
        features.forEachIndexed { index, (icon, title, body) ->
            BCard(Modifier.fillMaxWidth().padding(horizontal = if (index == 1) 12.dp else 0.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(48.dp).background(if (index % 2 == 0) p.primaryFixedDim else p.secondaryFixed, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = if (index % 2 == 0) p.primary else p.secondary)
                    }
                    BText(title, p.onSurface, 18.sp, FontWeight.Bold, align = TextAlign.Center)
                    BText(body, p.onSurfaceVariant, 14.sp, align = TextAlign.Center, lineHeight = 20.sp)
                }
            }
        }
        GradientButton("Begin Your Journey", onClick = onBegin)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.surfaceContainerLowest)
                .border(1.dp, p.primary.copy(alpha = 0.27f), RoundedCornerShape(24.dp)).clickable(onClick = onImport).padding(vertical = 15.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.UploadFile, null, tint = p.primary, modifier = Modifier.size(18.dp))
            BText("  Import from a previous Bowels version", p.primary, 14.sp, FontWeight.Bold)
        }
        BText(
            "No accounts. No cloud. Just you and your data. Starting asks for notifications and Health Connect once, so reminders and vitals work without extra setup.",
            p.onSurfaceVariant, 12.sp, FontWeight.Medium, align = TextAlign.Center, lineHeight = 17.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(p.surfaceContainerHigh).padding(horizontal = 18.dp, vertical = 14.dp),
        )
    }
}
