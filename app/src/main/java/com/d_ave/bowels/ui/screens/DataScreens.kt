package com.d_ave.bowels.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.domain.AnalyticsSummary
import com.d_ave.bowels.domain.WeeklyWrappedSummary
import com.d_ave.bowels.data.InsightItem
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.ui.components.BCard
import com.d_ave.bowels.ui.components.BText
import com.d_ave.bowels.ui.components.GradientButton
import com.d_ave.bowels.ui.components.ScreenColumn
import com.d_ave.bowels.ui.components.SecondaryButton
import com.d_ave.bowels.ui.components.SectionTitle
import com.d_ave.bowels.ui.theme.LocalPalette
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private val rowTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault())

@Composable
fun HistoryScreen(sessions: List<SessionWithAnswers>, onExport: () -> Unit, onImport: () -> Unit) {
    val p = LocalPalette.current
    ScreenColumn {
        SectionTitle("Data & Privacy", "Manage archives, keep exports local, and review your recent history.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BCard(Modifier.weight(1f).heightIn(min = 240.dp)) {
                BText("Export Data", p.onSurface, 20.sp, FontWeight.Bold)
                BText("Save sessions, answers, vitals and health check-ins as a local archive.", p.onSurfaceVariant, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                GradientButton("Archive", Icons.Filled.Download, onClick = onExport)
            }
            BCard(Modifier.weight(1f).heightIn(min = 240.dp)) {
                BText("Import Data", p.onSurface, 20.sp, FontWeight.Bold)
                BText("Merge a previous Bowels backup into this device.", p.onSurfaceVariant, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                GradientButton("Backup", Icons.Filled.UploadFile, onClick = onImport)
            }
        }
        BCard(Modifier.fillMaxWidth()) {
            BText("Recent Sessions", p.primary, 20.sp, FontWeight.Bold)
            sessions.take(12).forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = p.outline.copy(alpha = 0.13f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BText(if (item.session.kind == SessionKind.Bowel) "Bowel session" else "Urine session", p.onSurface, 15.sp, FontWeight.Bold)
                            if (item.answers[VitalsKeys.WATCH_ID] != null) Icon(Icons.Filled.Watch, "Logged on watch", tint = p.outline, modifier = Modifier.size(14.dp))
                        }
                        BText(rowTime.format(item.session.startTime), p.onSurfaceVariant)
                        vitalsLine(item)?.let { BText(it, p.secondary, 12.sp, FontWeight.Medium) }
                    }
                    BText("${(item.session.durationSeconds / 60.0).roundToInt()}m", p.primary, 18.sp, FontWeight.ExtraBold)
                }
            }
        }
    }
}

private fun vitalsLine(item: SessionWithAnswers): String? {
    val v = item.vitals
    val parts = buildList {
        v.heartRateAvg?.let { add("♥ $it bpm") }
        if (v.hasBloodPressure) add("BP ${v.systolic}/${v.diastolic}")
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

@Composable
fun AnalyticsScreen(analytics: AnalyticsSummary, insights: List<InsightItem>) {
    val p = LocalPalette.current
    ScreenColumn {
        SectionTitle("14-Day Insights", "Patterns from what you've logged over the last two weeks.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BCard(Modifier.weight(1f).heightIn(min = 150.dp)) {
                BText(analytics.totalVisits.toString(), p.onSurface, 36.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                BText("TOTAL VISITS", p.onSurfaceVariant, 11.sp, FontWeight.Bold, letterSpacing = 1.sp)
            }
            BCard(Modifier.weight(1f).heightIn(min = 150.dp), color = p.primaryContainer) {
                BText("${(analytics.averageDurationSeconds / 60.0).roundToInt()}m", p.onPrimary, 36.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                BText("AVG DURATION", p.onPrimary.copy(alpha = 0.8f), 11.sp, FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        val v = analytics.vitals
        BCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Favorite, null, tint = p.secondary)
                BText("Heart & blood pressure", p.onSurface, 22.sp, FontWeight.Bold)
            }
            if (v.isEmpty) {
                BText("No readings yet. Log a session on your watch, or connect Health Connect, and heart rate and blood pressure from around each session show up here.", p.onSurfaceVariant, lineHeight = 20.sp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VitalTile("AVG HEART RATE", v.averageHeartRate?.let { "$it" } ?: "—", "bpm", Modifier.weight(1f))
                    VitalTile("PEAK", v.peakHeartRate?.let { "$it" } ?: "—", "bpm", Modifier.weight(1f))
                    VitalTile("LATEST BP", v.latestBloodPressure?.let { "${it.first}/${it.second}" } ?: "—", "mmHg", Modifier.weight(1.3f))
                }
                BText("${v.sessionsWithHeartRate} session${if (v.sessionsWithHeartRate == 1) "" else "s"} with heart rate, ${v.bloodPressureReadings} with blood pressure in the last 14 days.", p.onSurfaceVariant, 13.sp)
            }
        }

        BCard(Modifier.fillMaxWidth()) {
            BText("Bristol Distribution", p.onSurface, 22.sp, FontWeight.Bold)
            Row(Modifier.fillMaxWidth().heightIn(min = 180.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                analytics.stoolDistribution.forEach { (type, count) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BText(count.toString(), p.primary, 11.sp, FontWeight.Bold)
                        Box(Modifier.fillMaxWidth().height((22 + minOf(count, 10) * 12).dp).background(if (type == 3 || type == 4) p.primary else p.surfaceContainerHighest, CircleShape))
                        BText("T$type", p.onSurfaceVariant, 11.sp, FontWeight.Bold)
                    }
                }
            }
        }

        BCard(Modifier.fillMaxWidth()) {
            BText("Fiber Effect", p.onSurface, 22.sp, FontWeight.Bold)
            BText(analytics.fiberCorrelationLabel, p.onSurfaceVariant, lineHeight = 20.sp)
        }

        insights.forEach { item ->
            BCard(Modifier.fillMaxWidth()) {
                val tone = when (item.severity) {
                    InsightItem.Severity.High -> p.error
                    InsightItem.Severity.Warning -> p.secondary
                    InsightItem.Severity.Info -> p.primary
                }
                BText(item.title, tone, 20.sp, FontWeight.Bold)
                BText(item.body, p.onSurfaceVariant, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun VitalTile(label: String, value: String, unit: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(p.surfaceContainerHigh).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BText(label, p.onSurfaceVariant, 10.sp, FontWeight.Bold, letterSpacing = 0.8.sp, maxLines = 1)
        BText(value, p.primary, 22.sp, FontWeight.ExtraBold, maxLines = 1)
        BText(unit, p.outline, 11.sp)
    }
}

/** Shows the exact image that gets shared, so what you see is what you post. */
@Composable
fun WeeklyWrappedScreen(weekly: WeeklyWrappedSummary, preview: ImageBitmap?, onShare: () -> Unit, onSave: () -> Unit) {
    val p = LocalPalette.current
    ScreenColumn {
        SectionTitle("Weekly Wrapped", if (weekly.totalVisits > 0) "Your last seven days, ready to share." else "Log a few sessions and your week fills in here.")
        Box(
            Modifier.fillMaxWidth(0.86f).align(Alignment.CenterHorizontally).aspectRatio(9f / 16f)
                .shadow(16.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).background(p.surfaceContainer),
        ) {
            if (preview != null) Image(preview, "Weekly Wrapped image", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f))
        }
        GradientButton("Share", Icons.Filled.Share, onClick = onShare)
        SecondaryButton("Save to Gallery", Icons.Filled.SaveAlt, onClick = onSave)
    }
}
