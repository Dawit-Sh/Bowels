package com.d_ave.bowels.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.text.style.TextAlign
import com.d_ave.bowels.domain.Forecast
import com.d_ave.bowels.domain.Outlook
import java.time.DayOfWeek
import java.time.format.TextStyle
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.d_ave.bowels.domain.stoolTypeMeta
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
fun HistoryScreen(sessions: List<SessionWithAnswers>, onExport: () -> Unit, onImport: () -> Unit, onOpen: (Long) -> Unit = {}) {
    val p = LocalPalette.current
    var shown by rememberSaveable { mutableIntStateOf(20) }
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
            BText("Tap a session to see how it compares with your usual.", p.onSurfaceVariant, 13.sp)
            sessions.take(shown).forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = p.outline.copy(alpha = 0.13f))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onOpen(item.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            BText(if (item.session.kind == SessionKind.Bowel) "Bowel session" else "Urine session", p.onSurface, 15.sp, FontWeight.Bold)
                            if (item.answers[VitalsKeys.WATCH_ID] != null) Icon(Icons.Filled.Watch, "Logged on watch", tint = p.outline, modifier = Modifier.size(14.dp))
                        }
                        BText(rowTime.format(item.session.startTime), p.onSurfaceVariant)
                        vitalsLine(item)?.let { BText(it, p.secondary, 12.sp, FontWeight.Medium) }
                    }
                    item.stoolType?.let { BText(stoolTypeMeta[it]?.emoji.orEmpty(), p.onSurface, 18.sp, modifier = Modifier.padding(end = 10.dp)) }
                    BText("${(item.session.durationSeconds / 60.0).roundToInt()}m", p.primary, 18.sp, FontWeight.ExtraBold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = p.outline, modifier = Modifier.size(20.dp))
                }
            }
            if (sessions.size > shown) {
                BText("Show more", p.primary, 14.sp, FontWeight.Bold, modifier = Modifier.clip(CircleShape).clickable { shown += 30 }.padding(vertical = 6.dp))
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
fun AnalyticsScreen(analytics: AnalyticsSummary, insights: List<InsightItem>, onGuidance: () -> Unit = {}) {
    val p = LocalPalette.current
    val pat = analytics.patterns
    ScreenColumn {
        SectionTitle("Your patterns", "From what you've logged, measured against your own usual.")

        analytics.forecast?.let { ForecastCard(it) }

        pat?.regularity?.let { score ->
            BCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        BText("Regularity", p.onSurface, 22.sp, FontWeight.Bold)
                        BText("Last 30 days", p.onSurfaceVariant, 13.sp)
                    }
                    BText("$score", p.primary, 44.sp, FontWeight.ExtraBold)
                    BText("/100", p.outline, 14.sp, FontWeight.Bold, modifier = Modifier.padding(start = 2.dp, top = 14.dp))
                }
                pat.regularityParts.forEach { (name, value) -> Meter(name, value) }
                BText(
                    "Timing: how close your first visit of the day stays to the same time. Form: how many were Type 3-4. Frequency: days within the normal 3 a week to 3 a day.",
                    p.onSurfaceVariant, 12.sp, lineHeight = 17.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(analytics.totalVisits.toString(), "TOTAL VISITS", Modifier.weight(1f))
            StatTile("${(analytics.averageDurationSeconds / 60.0).roundToInt()}m", "AVG LENGTH", Modifier.weight(1f), highlight = true)
        }
        if (pat != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${pat.currentStreak}", "DAY STREAK", Modifier.weight(1f))
                StatTile("${pat.longestStreak}", "LONGEST", Modifier.weight(1f))
                StatTile(pat.usualGapHours?.let { "${it.roundToInt()}h" } ?: "—", "USUAL GAP", Modifier.weight(1f))
            }
        }

        BCard(Modifier.fillMaxWidth()) {
            BText("Your Rhythm", p.onSurface, 22.sp, FontWeight.Bold)
            BText("Next likely: ${analytics.predictedNextTimeLabel}", p.primary, 16.sp, FontWeight.Bold)
            BText(analytics.predictionNote, p.onSurfaceVariant, lineHeight = 20.sp)
        }

        if (pat != null && pat.hourly.sum() > 0) {
            BCard(Modifier.fillMaxWidth()) {
                BText("Time of day", p.onSurface, 22.sp, FontWeight.Bold)
                BText("When your visits happened in the last 30 days.", p.onSurfaceVariant, 13.sp)
                val peak = pat.hourly.max().coerceAtLeast(1)
                Row(Modifier.fillMaxWidth().height(90.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    pat.hourly.forEach { n ->
                        Box(Modifier.weight(1f).fillMaxHeight(0.06f + 0.94f * n / peak).clip(RoundedCornerShape(3.dp)).background(if (n == peak) p.primary else if (n > 0) p.primaryFixedDim else p.surfaceContainerHigh))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("12a", "6a", "12p", "6p", "12a").forEach { BText(it, p.outline, 10.sp, FontWeight.Bold) }
                }
            }
        }

        if (pat != null && pat.weekday.any { it > 0 }) {
            BCard(Modifier.fillMaxWidth()) {
                BText("By weekday", p.onSurface, 22.sp, FontWeight.Bold)
                BText("Average visits on each day, last 8 weeks.", p.onSurfaceVariant, 13.sp)
                val top = pat.weekday.max().coerceAtLeast(0.1)
                Row(Modifier.fillMaxWidth().height(110.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pat.weekday.forEachIndexed { i, v ->
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            BText("%.1f".format(v), p.primary, 10.sp, FontWeight.Bold)
                            Box(Modifier.fillMaxWidth().fillMaxHeight((0.08f + 0.72f * (v / top)).toFloat()).clip(RoundedCornerShape(8.dp)).background(if (v == top) p.primary else p.primaryFixedDim))
                            BText(DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, Locale.getDefault()), p.outline, 11.sp, FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (pat != null && pat.weeks.any { it.visits > 0 }) {
            BCard(Modifier.fillMaxWidth()) {
                BText("8-week trend", p.onSurface, 22.sp, FontWeight.Bold)
                FieldLabelSmall("Type 3-4 share")
                WeekBars(pat.weeks.map { it.steadyShare }, { v -> "${(v * 100).roundToInt()}%" }, 1.0)
                FieldLabelSmall("Visits per week")
                WeekBars(pat.weeks.map { it.visits.toDouble().takeIf { v -> v > 0 } }, { v -> "${v.roundToInt()}" }, pat.weeks.maxOf { it.visits }.toDouble().coerceAtLeast(1.0))
                FieldLabelSmall("Typical length (min)")
                WeekBars(pat.weeks.map { it.medianMinutes }, { v -> "${v.roundToInt()}" }, pat.weeks.mapNotNull { it.medianMinutes }.maxOrNull() ?: 1.0)
            }
        }

        if (pat != null && pat.symptoms.isNotEmpty()) {
            BCard(Modifier.fillMaxWidth()) {
                BText("Symptoms", p.onSurface, 22.sp, FontWeight.Bold)
                BText("Share of visits in the last 30 days, against the 60 before.", p.onSurfaceVariant, 13.sp)
                pat.symptoms.forEach { sym ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BText(sym.name, p.onSurface, 14.sp, FontWeight.Medium, modifier = Modifier.weight(1f))
                        sym.before?.let { b ->
                            val d = sym.recent - b
                            BText(
                                when { d >= 5 -> "▲ $d"; d <= -5 -> "▼ ${-d}"; else -> "=" },
                                when { d >= 5 -> p.secondary; d <= -5 -> p.primary; else -> p.outline }, 12.sp, FontWeight.Bold,
                                modifier = Modifier.padding(end = 10.dp),
                            )
                        }
                        BText("${sym.recent}%", p.primary, 15.sp, FontWeight.ExtraBold)
                    }
                }
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
                val top = analytics.stoolDistribution.maxOf { it.second }.coerceAtLeast(1)
                analytics.stoolDistribution.forEach { (type, count) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BText(count.toString(), p.primary, 11.sp, FontWeight.Bold)
                        Box(Modifier.fillMaxWidth().height((22 + 120 * count / top).dp).background(if (type == 3 || type == 4) p.primary else p.surfaceContainerHighest, CircleShape))
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
        SecondaryButton("What could help: Gut Practices", Icons.Filled.Spa, onClick = onGuidance)
    }
}

/** Tomorrow's outlook from the on-device model, with what's driving it and how it has done. */
@Composable
fun ForecastCard(f: Forecast) {
    val p = LocalPalette.current
    BCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.AutoAwesome, null, tint = p.tertiary)
            BText("Tomorrow's outlook", p.onSurface, 22.sp, FontWeight.Bold)
        }
        val tone = when (f.outlook) { Outlook.Steady -> p.primary; Outlook.Looser -> p.secondary; Outlook.Harder -> p.tertiary }
        BText(
            when (f.outlook) {
                Outlook.Steady -> "Likely steady"
                Outlook.Looser -> "Leaning looser"
                Outlook.Harder -> "Leaning harder"
            },
            tone, 26.sp, FontWeight.ExtraBold,
        )
        Outlook.entries.forEach { o -> Meter(o.label, ((f.chance[o] ?: 0.0) * 100).roundToInt(), suffix = "%") }
        if (f.drivers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BText("WHAT'S PUSHING IT", p.primary, 10.sp, FontWeight.Bold, letterSpacing = 1.sp)
                f.drivers.forEach { d -> BText("${d.text} → ${d.towards.label.lowercase()}", p.onSurface, 14.sp, lineHeight = 19.sp) }
            }
        }
        BText(
            buildString {
                append("A small model trained on this phone from ${f.daysLearned} days of your logs, starting from what research says about water, fiber, stress, sleep, caffeine and alcohol.")
                if (f.checked >= 5) append(" Checked on your last ${f.checked} days, it was right on ${f.hits}; guessing \"like usual\" would have got ${f.baselineHits}.")
                if (!f.usedTodaysCheck) append(" Fill in today's health check to sharpen it.")
            },
            p.onSurfaceVariant, 12.sp, lineHeight = 17.sp,
        )
    }
}

@Composable
private fun Meter(label: String, value: Int, suffix: String = "") {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BText(label, p.onSurfaceVariant, 12.sp, FontWeight.Bold, modifier = Modifier.width(78.dp))
        Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(p.surfaceContainerHigh)) {
            Box(Modifier.fillMaxWidth(value.coerceIn(0, 100) / 100f).fillMaxHeight().clip(CircleShape).background(p.primary))
        }
        BText("$value$suffix", p.primary, 12.sp, FontWeight.ExtraBold, modifier = Modifier.width(40.dp), align = TextAlign.End)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier, highlight: Boolean = false) {
    val p = LocalPalette.current
    BCard(modifier.heightIn(min = 110.dp), color = if (highlight) p.primaryContainer else null) {
        BText(value, if (highlight) p.onPrimary else p.onSurface, 30.sp, FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1)
        BText(label, if (highlight) p.onPrimary.copy(alpha = 0.8f) else p.onSurfaceVariant, 10.sp, FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1)
    }
}

@Composable
private fun FieldLabelSmall(text: String) {
    val p = LocalPalette.current
    BText(text.uppercase(), p.onSurfaceVariant, 10.sp, FontWeight.Bold, letterSpacing = 1.sp)
}

/** Eight small bars, one per week, oldest first; empty weeks stay flat. */
@Composable
private fun WeekBars(values: List<Double?>, label: (Double) -> String, max: Double) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                if (v != null) BText(label(v), p.primary, 9.sp, FontWeight.Bold, maxLines = 1)
                Box(
                    Modifier.fillMaxWidth().fillMaxHeight(if (v == null) 0.05f else (0.08f + 0.62f * (v / max)).toFloat().coerceIn(0.05f, 0.7f))
                        .clip(RoundedCornerShape(6.dp)).background(if (i == values.lastIndex) p.primary else p.primaryFixedDim),
                )
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
