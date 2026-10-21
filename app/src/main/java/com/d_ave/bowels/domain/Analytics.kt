package com.d_ave.bowels.domain

import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.DayCount
import com.d_ave.bowels.data.InsightItem
import com.d_ave.bowels.data.InsightItem.Severity
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.data.localDate
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class VitalsSummary(
    val sessionsWithHeartRate: Int = 0,
    val averageHeartRate: Int? = null,
    val peakHeartRate: Int? = null,
    val latestBloodPressure: Pair<Int, Int>? = null,
    val latestBloodPressureAt: Instant? = null,
    val bloodPressureReadings: Int = 0,
) {
    val isEmpty get() = sessionsWithHeartRate == 0 && bloodPressureReadings == 0
}

data class AnalyticsSummary(
    val visitsPerDay: List<DayCount>,
    val averageDurationSeconds: Long,
    val stoolDistribution: List<Pair<Int, Int>>,
    val fiberCorrelationLabel: String,
    val totalVisits: Int,
    val totalDurationSeconds: Long,
    val predictedNextTimeLabel: String,
    val milestoneProgressDays: Int,
    val vitals: VitalsSummary,
)

data class WeeklyWrappedSummary(
    val totalVisits: Int,
    val averageDurationSeconds: Long,
    val totalDurationSeconds: Long,
    val visitsPerDay: List<DayCount>,
    val stoolDistribution: List<Pair<Int, Int>>,
    val mostCommonStoolType: Int?,
    val bestDay: DayCount?,
    val worstDay: DayCount?,
    val averageHeartRate: Int?,
    val weekStart: LocalDate,
)

object Analytics {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun lastDays(days: Int): List<LocalDate> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    private fun SessionWithAnswers.day() = session.startTime.localDate(zone)

    fun build(sessions: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>): AnalyticsSummary {
        val days = lastDays(14)
        val byDay = sessions.groupingBy { it.day() }.eachCount()
        val totalDuration = sessions.sumOf { it.session.durationSeconds }
        val fiberHeavyDays = dailyHealth.count { it.fiber.lowercase() in setOf("high", "great") }
        val smoothDays = sessions.count { it.stoolType in setOf(3, 4) }
        return AnalyticsSummary(
            visitsPerDay = days.map { DayCount(it, byDay[it] ?: 0) },
            averageDurationSeconds = if (sessions.isEmpty()) 0 else (totalDuration.toDouble() / sessions.size).roundToInt().toLong(),
            stoolDistribution = (1..7).map { type -> type to sessions.count { it.stoolType == type } },
            fiberCorrelationLabel = if (fiberHeavyDays > 0 && smoothDays > 0) "Higher-fiber days trend toward Type 3-4 results." else "More logs are needed for a reliable fiber correlation.",
            totalVisits = sessions.size,
            totalDurationSeconds = totalDuration,
            predictedNextTimeLabel = predictNext(sessions),
            milestoneProgressDays = sessions.map { it.day() }.toSet().size,
            vitals = vitalsSummary(sessions.filter { it.day() >= days.first() }),
        )
    }

    fun vitalsSummary(sessions: List<SessionWithAnswers>): VitalsSummary {
        val withHr = sessions.mapNotNull { it.vitals.heartRateAvg }
        val peaks = sessions.mapNotNull { it.vitals.heartRateMax ?: it.vitals.heartRateAvg }
        val withBp = sessions.filter { it.vitals.hasBloodPressure }
        val latest = withBp.maxByOrNull { it.vitals.bloodPressureTime ?: it.session.startTime }
        return VitalsSummary(
            sessionsWithHeartRate = withHr.size,
            averageHeartRate = withHr.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            peakHeartRate = peaks.maxOrNull(),
            latestBloodPressure = latest?.let { it.vitals.systolic!! to it.vitals.diastolic!! },
            latestBloodPressureAt = latest?.let { it.vitals.bloodPressureTime ?: it.session.startTime },
            bloodPressureReadings = withBp.size,
        )
    }

    private fun predictNext(sessions: List<SessionWithAnswers>): String {
        val bowelTimes = sessions.filter { it.session.kind == SessionKind.Bowel }
            .map { it.session.startTime.atZone(zone) }
            .sortedDescending()
        if (bowelTimes.size < 2) return "Need more data"
        if (bowelTimes.size < 3) return "Building pattern..."

        val recent = bowelTimes.take(21)
        val intervals = recent.zipWithNext { a, b -> Duration.between(b, a).toMinutes() / 60.0 }
        val avgInterval = if (intervals.isEmpty()) 24.0 else intervals.average()
        val weightedInterval = if (intervals.isEmpty()) 24.0 else
            intervals.withIndex().sumOf { (i, v) -> v * 0.85.pow(i) } / intervals.indices.sumOf { 0.85.pow(it) }

        val minutesOfDay = { d: ZonedDateTime -> d.hour * 60 + d.minute }
        val byWeekday = recent.groupBy { it.dayOfWeek }.mapValues { (_, v) -> v.map(minutesOfDay) }

        fun bucket(hour: Int) = when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }

        val variance = if (intervals.size > 1) intervals.sumOf { (it - avgInterval).pow(2) } / intervals.size else 0.0
        val consistency = if (avgInterval > 0) max(0.0, 1 - sqrt(variance) / avgInterval) else 0.0

        val stepMinutes = max(60L, (weightedInterval * 60).roundToInt().toLong())
        var predicted = recent.first().plusMinutes(stepMinutes)
        fun at(minutes: Double) = predicted.withHour((minutes / 60).toInt()).withMinute((minutes % 60).roundToInt().coerceAtMost(59))

        // A steady time on that weekday wins; otherwise your usual time of day. (The old app kept
        // the raw "last + gap" time when a weekday was irregular, which gave answers like 12:22 AM.)
        val dayTimes = byWeekday[predicted.dayOfWeek]
        val steadyDay = dayTimes?.takeIf { it.size >= 2 }?.let { times ->
            val mean = times.average()
            val dayVariance = times.sumOf { (it - mean).pow(2) } / times.size
            mean.takeIf { max(0.0, 1 - sqrt(dayVariance) / 60) > 0.7 }
        }
        if (steadyDay != null) {
            predicted = at(steadyDay)
        } else {
            val preferred = recent.groupingBy { bucket(it.hour) }.eachCount().maxByOrNull { it.value }?.key ?: "morning"
            val preferredTimes = recent.filter { bucket(it.hour) == preferred }.map(minutesOfDay)
            if (preferredTimes.isNotEmpty()) predicted = at(preferredTimes.average())
        }
        // Step whole days, so the time of day stays put.
        val now = ZonedDateTime.now(zone)
        while (predicted.isBefore(now)) predicted = predicted.plusDays(1)

        val day = predicted.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val time = predicted.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
        val confidence = when {
            consistency > 0.8 -> "🎯"
            consistency > 0.6 -> "📊"
            else -> "🔮"
        }
        return "$confidence $day $time"
    }

    fun weeklyWrapped(sessions: List<SessionWithAnswers>): WeeklyWrappedSummary {
        val days = lastDays(7)
        val window = days.toSet()
        val weekly = sessions.filter { it.session.kind == SessionKind.Bowel && it.day() in window }
        val byDay = weekly.groupingBy { it.day() }.eachCount()
        val perDay = days.map { DayCount(it, byDay[it] ?: 0) }
        val distribution = (1..7).map { type -> type to weekly.count { it.stoolType == type } }
        val total = weekly.sumOf { it.session.durationSeconds }
        val heartRates = weekly.mapNotNull { it.vitals.heartRateAvg }
        return WeeklyWrappedSummary(
            totalVisits = weekly.size,
            averageDurationSeconds = if (weekly.isEmpty()) 0 else (total.toDouble() / weekly.size).roundToInt().toLong(),
            totalDurationSeconds = total,
            visitsPerDay = perDay,
            stoolDistribution = distribution,
            mostCommonStoolType = distribution.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first,
            bestDay = perDay.maxByOrNull { it.count },
            worstDay = perDay.minByOrNull { it.count },
            averageHeartRate = heartRates.takeIf { it.isNotEmpty() }?.average()?.roundToInt(),
            weekStart = days.first(),
        )
    }

    fun insights(sessions: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>): List<InsightItem> {
        val items = mutableListOf<InsightItem>()
        val bowel = sessions.filter { it.session.kind == SessionKind.Bowel }
        val latest = bowel.firstOrNull()

        if (latest == null || Duration.between(latest.session.endTime, Instant.now()).toHours() >= 72) {
            items += InsightItem("no-bowel", "No bowel log in 3 days", "Your recent history suggests a gap in bowel activity. Consider hydration, movement, and fiber intake.", Severity.High)
        }
        val lastStool = latest?.stoolType ?: 0
        if (lastStool in 1..2) items += InsightItem("low-fiber", "Pattern suggests low fiber", "Recent bowel sessions trend toward Type 1-2, which may align with constipation or low fiber intake.", Severity.Warning)
        if (lastStool in 6..7) items += InsightItem("hydration-issue", "Hydration issue flagged", "Type 6-7 patterns can be associated with hydration imbalance or irritation.", Severity.Warning)

        if (sessions.count { it.session.durationSeconds >= 12 * 60 } >= 2) {
            items += InsightItem("long-session", "Long sessions recurring", "Multiple sessions exceeded 12 minutes. Consider whether straining, urgency, or discomfort is increasing.", Severity.Info)
        }
        if (bowel.take(5).count { it.answers["pain"] in setOf("moderate", "severe") } >= 2) {
            items += InsightItem("pain-pattern", "Pain pattern detected", "Multiple recent sessions with moderate to severe pain. Consider consulting a healthcare provider if this persists.", Severity.High)
        }
        val recent7 = bowel.take(7)
        if (recent7.any { it.answers["blood"] == "true" }) {
            items += InsightItem("blood-detected", "Blood detected in recent sessions", "Blood in stool should be evaluated by a healthcare provider, especially if recurring.", Severity.High)
        }
        if (recent7.count { (it.answers["urgency"]?.toIntOrNull() ?: 0) >= 4 } >= 3) {
            items += InsightItem("high-urgency", "Frequent high urgency", "Multiple sessions with high urgency (4-5). This may indicate digestive sensitivity or stress.", Severity.Warning)
        }
        val tags = recent7.flatMap { it.answers["adaptive_tags"].orEmpty().split(",").filter(String::isNotEmpty) }.groupingBy { it }.eachCount()
        if ((tags["straining"] ?: 0) >= 3) items += InsightItem("straining-pattern", "Frequent straining detected", "Straining during bowel movements may indicate low fiber, dehydration, or other digestive issues.", Severity.Warning)
        if ((tags["bloating"] ?: 0) >= 3) items += InsightItem("bloating-pattern", "Recurring bloating", "Frequent bloating may be related to diet, food sensitivities, or eating habits. Consider tracking trigger foods.", Severity.Info)

        items += vitalsInsights(bowel)

        if (dailyHealth.isNotEmpty()) {
            val health = dailyHealth.take(7)
            val stool7 = recent7.mapNotNull { it.stoolType }
            if (health.count { it.caffeine in setOf("High", "Very High", "Excessive") } >= 3 && stool7.count { it in 6..7 } >= 2) {
                items += InsightItem("caffeine-correlation", "Caffeine may be affecting digestion", "High caffeine intake correlates with looser stools. Consider reducing intake to see if symptoms improve.", Severity.Info)
            }
            if (health.count { it.stress in setOf("High", "Very High", "Extreme") } >= 3 && recent7.count { (it.answers["urgency"]?.toIntOrNull() ?: 0) >= 4 } >= 2) {
                items += InsightItem("stress-correlation", "Stress may be impacting digestion", "High stress levels correlate with increased urgency. Stress management techniques may help.", Severity.Info)
            }
            if (health.count { it.sleep in setOf("Poor", "Very Poor", "None") } >= 4) {
                items += InsightItem("sleep-pattern", "Poor sleep pattern detected", "Consistent poor sleep can affect digestive health. Aim for 7-9 hours of quality sleep.", Severity.Warning)
            }
            if (health.count { it.alcohol !in setOf("None", "") } >= 2 && stool7.count { it <= 2 || it >= 6 } >= 2) {
                items += InsightItem("alcohol-correlation", "Alcohol may be affecting regularity", "Alcohol consumption correlates with irregular stool patterns. Consider moderating intake.", Severity.Info)
            }
            if (health.count { it.mood in setOf("Sad", "Anxious", "Stressed", "Angry") } >= 4) {
                items += InsightItem("mood-pattern", "Mood affecting wellness", "Negative mood patterns can impact digestive health. Consider stress management or speaking with a professional.", Severity.Info)
            }
        }

        if (items.isEmpty()) {
            items += InsightItem("steady", "Rhythm looks steady", "Recent logs do not show a strong warning pattern. Keep your routine consistent.", Severity.Info)
        }
        return items
    }

    /** Calm, observational notes from heart rate and blood pressure. Never a diagnosis. */
    private fun vitalsInsights(bowel: List<SessionWithAnswers>): List<InsightItem> {
        val items = mutableListOf<InsightItem>()
        val recent = bowel.take(10)

        val highBp = recent.count { (it.vitals.systolic ?: 0) >= 140 || (it.vitals.diastolic ?: 0) >= 90 }
        if (highBp >= 2) {
            items += InsightItem("bp-elevated", "Higher blood pressure around sessions", "$highBp recent readings near a session were 140/90 or above. Straining can raise it briefly, but if readings stay high, mention it to your doctor.", Severity.Warning)
        }

        val strained = recent.filter { "straining" in it.answers["adaptive_tags"].orEmpty() }.mapNotNull { it.vitals.heartRateAvg }
        val relaxed = recent.filter { "straining" !in it.answers["adaptive_tags"].orEmpty() }.mapNotNull { it.vitals.heartRateAvg }
        if (strained.size >= 2 && relaxed.size >= 2) {
            val diff = strained.average() - relaxed.average()
            if (abs(diff) >= 8) {
                items += InsightItem(
                    "hr-straining",
                    "Straining changes your heart rate",
                    "Your heart rate averages ${diff.roundToInt().let { if (it > 0) "$it bpm higher" else "${-it} bpm lower" }} on sessions where you strained. Taking your time and staying hydrated can ease it.",
                    Severity.Info,
                )
            }
        }

        val peaks = recent.mapNotNull { it.vitals.heartRateMax }
        if (peaks.count { it >= 120 } >= 2) {
            items += InsightItem("hr-peaks", "Heart rate spikes on the toilet", "Your heart rate went past 120 bpm in more than one recent session. If you also feel dizzy or faint, talk to a doctor.", Severity.Warning)
        }
        return items
    }

    /** Stable for the whole week, so the screen and the shared image say the same thing. */
    fun quirkyInsight(w: WeeklyWrappedSummary): String {
        val minutes = (w.totalDurationSeconds / 60.0).roundToInt()
        val quirks = listOf(
            "You spent $minutes minutes this week in mindful reflection. That's ${(minutes / 7.0).roundToInt()} minutes per day of self-care.",
            "Your most productive day had ${w.visitsPerDay.maxOfOrNull { it.count } ?: 0} visits. Consistency is key!",
            "Your rhythm is ${if (w.totalVisits >= 14) "incredibly steady" else if (w.totalVisits >= 7) "building momentum" else "just getting started"}. Keep it up!",
            "If your sessions were songs, you'd have a $minutes-minute wellness playlist.",
        )
        return quirks[(w.weekStart.toEpochDay() % quirks.size).toInt()]
    }

    fun personality(w: WeeklyWrappedSummary): Pair<String, String> {
        val avg = w.averageDurationSeconds / 60.0
        return when {
            avg < 3 -> "The Efficient One" to "Quick and focused"
            avg < 5 -> "The Balanced Soul" to "Perfect harmony"
            avg < 8 -> "The Mindful Meditator" to "Deep reflection"
            else -> "The Zen Master" to "Ultimate patience"
        }
    }
}
