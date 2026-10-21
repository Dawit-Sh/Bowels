package com.d_ave.bowels.ui.screens

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
fun BadgesScreen(progressDays: Int, onShare: (Milestone) -> Unit) {
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

        milestones.forEachIndexed { index, m ->
            MilestoneCard(m, earned = progressDays >= m.days, index = index, progressDays = progressDays, onShare = { onShare(m) })
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
    1 -> Icons.Filled.Spa
    7 -> Icons.Filled.Waves
    30 -> Icons.Filled.Favorite
    else -> Icons.Filled.EmojiEvents
}

@Composable
private fun MilestoneCard(m: Milestone, earned: Boolean, index: Int, progressDays: Int, onShare: () -> Unit) {
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
        Modifier.fillMaxWidth().alpha(if (earned) 1f else if (index == 2) 0.72f else 0.52f)
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
                    index == 2 -> "Locked ($progressDays/${m.days})"
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

@Composable
fun HealthInfoScreen() {
    val p = LocalPalette.current
    ScreenColumn {
        SectionTitle("Gut Health Best Practices", "Private, lightweight guidance stored on your phone.")
        healthInfo.forEach { tip ->
            BCard(Modifier.fillMaxWidth()) {
                BText(tip.title, p.onSurface, 20.sp, FontWeight.Bold)
                BText(tip.summary, p.primary, 14.sp, FontWeight.Bold, lineHeight = 20.sp)
                BText(tip.body, p.onSurfaceVariant, 14.sp, lineHeight = 21.sp)
            }
        }
    }
}

class SettingsActions(
    val setTheme: (ThemeMode) -> Unit,
    val setAccent: (AccentKey) -> Unit,
    val setReminderHour: (Int) -> Unit,
    val connectHealth: () -> Unit,
    val installOnWatch: () -> Unit,
    val checkUpdates: () -> Unit,
)

@Composable
fun SettingsScreen(settings: AppSettings, connections: Connections, version: String, actions: SettingsActions) {
    val p = LocalPalette.current
    ScreenColumn {
        SectionTitle("Settings", "Theme, accent colors, reminders and connections. Everything stays on this phone.")
        BCard(Modifier.fillMaxWidth()) {
            BText("Theme", p.onSurface, 20.sp, FontWeight.Bold)
            ChipRow(ThemeMode.entries, { settings.themeMode == it }, { it.key }, actions.setTheme)
        }
        BCard(Modifier.fillMaxWidth()) {
            BText("Accent", p.onSurface, 20.sp, FontWeight.Bold)
            ChipRow(AccentKey.entries, { settings.accent == it }, { it.key }, actions.setAccent)
        }
        BCard(Modifier.fillMaxWidth()) {
            BText("Daily reminder hour", p.onSurface, 20.sp, FontWeight.Bold)
            ChipRow(listOf(8, 12, 18, 20, 22), { settings.reminderHour == it }, { "$it:00" }, actions.setReminderHour)
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
                when (watch) {
                    is WatchConnector.State.Ready -> "${watch.name} is ready. Sessions logged there sync here."
                    is WatchConnector.State.NeedsInstall -> "${watch.name} is paired but doesn't have Bowels yet."
                    WatchConnector.State.NoWatch -> "No Wear OS watch paired."
                },
                action = if (watch is WatchConnector.State.NeedsInstall) "Install" else null,
                onAction = actions.installOnWatch,
            )
        }
        BCard(Modifier.fillMaxWidth()) {
            BText("App Updates", p.onSurface, 20.sp, FontWeight.Bold)
            BText("You're on $version. Check GitHub for the latest version.", p.onSurfaceVariant, lineHeight = 20.sp)
            GradientButton("Check for Updates", Icons.Filled.SystemUpdate, onClick = actions.checkUpdates)
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
