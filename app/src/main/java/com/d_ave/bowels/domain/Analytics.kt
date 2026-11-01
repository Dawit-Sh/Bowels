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
    val predictionNote: String,
    val prediction: Prediction?,
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

    fun build(all: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>): AnalyticsSummary {
        val sessions = Rhythm.distinct(all)
        val days = lastDays(14)
        val byDay = sessions.groupingBy { it.day() }.eachCount()
        val totalDuration = sessions.sumOf { it.session.durationSeconds }
        val now = ZonedDateTime.now(zone)
        val prediction = predictCached(sessions, now)
        val bowelCount = sessions.count { it.session.kind == SessionKind.Bowel }
        return AnalyticsSummary(
            visitsPerDay = days.map { DayCount(it, byDay[it] ?: 0) },
            averageDurationSeconds = if (sessions.isEmpty()) 0 else (totalDuration.toDouble() / sessions.size).roundToInt().toLong(),
            stoolDistribution = (1..7).map { type -> type to sessions.count { it.stoolType == type } },
            fiberCorrelationLabel = fiberLabel(sessions, dailyHealth),
            totalVisits = sessions.size,
            totalDurationSeconds = totalDuration,
            predictedNextTimeLabel = when {
                prediction != null -> windowLabel(prediction, now)
                bowelCount < 2 -> "Need more data"
                else -> "Building pattern..."
            },
            predictionNote = prediction?.let { predictionNote(it) } ?: "Log a few visits and this learns your usual times.",
            prediction = prediction,
            milestoneProgressDays = sessions.map { it.day() }.toSet().size,
            vitals = vitalsSummary(sessions.filter { it.day() >= days.first() }),
        )
    }

    // The screen state is rebuilt often (every screen change); the sessions list only changes when data does.
    private var cached: Triple<List<SessionWithAnswers>, Long, Prediction?>? = null

    private fun predictCached(sessions: List<SessionWithAnswers>, now: ZonedDateTime): Prediction? {
        val bucket = now.toEpochSecond() / 300
        cached?.let { (list, at, p) -> if (list == sessions && at == bucket) return p }
        val p = Rhythm.predict(sessions.filter { it.session.kind == SessionKind.Bowel }.asReversed(), now)
        cached = Triple(sessions, bucket, p)
        return p
    }

    private fun clock(t: ZonedDateTime) = t.format(DateTimeFormatter.ofPattern("h:mm", Locale.getDefault()))
    private fun amPm(t: ZonedDateTime) = t.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))

    /** "Tomorrow 8:10–9:40 AM", "Today 6:50 PM–8:10 PM". */
    fun windowLabel(p: Prediction, now: ZonedDateTime): String {
        val day = when (p.at.toLocalDate()) {
            now.toLocalDate() -> "Today"
            now.toLocalDate().plusDays(1) -> "Tomorrow"
            else -> p.at.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }
        val range = if (amPm(p.from) == amPm(p.to)) "${clock(p.from)}–${clock(p.to)} ${amPm(p.to)}"
        else "${clock(p.from)} ${amPm(p.from)}–${clock(p.to)} ${amPm(p.to)}"
        return "$day $range"
    }

    private fun predictionNote(p: Prediction): String {
        val around = "Most likely around ${clock(p.at)} ${amPm(p.at)}."
        return if (p.checked >= 5) "$around Checked against your own history, this window held the real time on ${p.hits} of your last ${p.checked} visits."
        else "$around It learns your rhythm from every visit you log, newer ones counting more."
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

    fun weeklyWrapped(all: List<SessionWithAnswers>): WeeklyWrappedSummary {
        val sessions = Rhythm.distinct(all)
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

    /**
     * What stands out in your logs. Safety notes (blood, pain, a three-day gap) use fixed lines;
     * everything else is measured against your own usual, so a pattern that's normal for you
     * doesn't nag, and a link to a habit is only shown when your own history backs it up.
     */
    fun insights(all: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>, now: Instant = Instant.now()): List<InsightItem> {
        val items = mutableListOf<InsightItem>()
        val bowel = Rhythm.distinct(all).filter { it.session.kind == SessionKind.Bowel }
        val latest = bowel.firstOrNull()

        val hoursSince = latest?.let { Duration.between(it.session.endTime, now).toMinutes() / 60.0 }
        if (hoursSince == null || hoursSince >= 72) {
            items += InsightItem("no-bowel", "No bowel log in 3 days", "Your recent history suggests a gap in bowel activity. Consider hydration, movement, and fiber intake.", Severity.High)
        } else {
            val gaps = bowel.filter { Duration.between(it.session.startTime, now).toDays() <= 60 }
                .zipWithNext { newer, older -> Duration.between(older.session.startTime, newer.session.startTime).toMinutes() / 60.0 }
            if (gaps.size >= 10) {
                val usual = gaps.sorted()[(gaps.size * 0.9).toInt().coerceAtMost(gaps.size - 1)]
                if (hoursSince >= max(30.0, usual * 1.2)) {
                    items += InsightItem("longer-gap", "Longer gap than usual", "It's been ${hoursSince.roundToInt()} hours since your last visit, and your gaps are usually under ${usual.roundToInt()} hours. Water, a walk and a fiber-rich meal can help things along.", Severity.Info)
                }
            }
        }

        if (bowel.take(5).count { it.answers["pain"] in setOf("moderate", "severe") } >= 2) {
            items += InsightItem("pain-pattern", "Pain pattern detected", "Multiple recent sessions with moderate to severe pain. Consider consulting a healthcare provider if this persists.", Severity.High)
        }
        val recent7 = bowel.take(7)
        if (recent7.any { it.answers["blood"] == "true" }) {
            items += InsightItem("blood-detected", "Blood detected in recent sessions", "Blood in stool should be evaluated by a healthcare provider, especially if recurring.", Severity.High)
        }

        items += consistencyInsights(bowel)
        items += durationInsights(bowel)

        val urgent = recent7.count { (it.answers["urgency"]?.toIntOrNull() ?: 0) >= 4 }
        if (urgent >= 3) {
            items += InsightItem("high-urgency", "Frequent high urgency", "$urgent of your last ${recent7.size} visits had high urgency (4-5). This may point to digestive sensitivity or stress.", Severity.Warning)
        }
        val tags = recent7.flatMap { it.answers["adaptive_tags"].orEmpty().split(",").filter(String::isNotEmpty) }.groupingBy { it }.eachCount()
        tags["straining"]?.takeIf { it >= 3 }?.let {
            items += InsightItem("straining-pattern", "Frequent straining", "You strained on $it of your last ${recent7.size} visits. Low fiber, too little water or rushing are the usual reasons.", Severity.Warning)
        }
        tags["bloating"]?.takeIf { it >= 3 }?.let {
            items += InsightItem("bloating-pattern", "Recurring bloating", "Bloating came up on $it of your last ${recent7.size} visits. Diet, food sensitivities or eating fast can cause it, and noting trigger foods helps.", Severity.Info)
        }

        items += vitalsInsights(bowel)
        items += habitEffects(bowel, dailyHealth, now).take(2).map { it.insight }

        val health = dailyHealth.take(7)
        if (health.count { it.sleep.lowercase() in setOf("poor", "very poor", "none") } >= 4) {
            items += InsightItem("sleep-pattern", "Poor sleep pattern detected", "Consistent poor sleep can affect digestive health. Aim for 7-9 hours of quality sleep.", Severity.Warning)
        }
        if (health.count { it.mood.lowercase() in setOf("sad", "anxious", "stressed", "angry") } >= 4) {
            items += InsightItem("mood-pattern", "Mood affecting wellness", "Negative mood patterns can impact digestive health. Consider stress management or speaking with a professional.", Severity.Info)
        }

        if (items.isEmpty()) {
            items += InsightItem("steady", "Rhythm looks steady", "Nothing in your recent logs stands out from your usual. Keep your routine going.", Severity.Info)
        }
        // Most important first: the home screen shows the top one.
        return items.sortedByDescending { it.severity.ordinal }
    }

    private fun share(count: Int, total: Int) = "${(100.0 * count / total).roundToInt()}%"

    private fun zScore(hits: Int, n: Int, baseHits: Int, baseN: Int): Double {
        if (n == 0 || baseN == 0) return 0.0
        val pooled = (hits + baseHits).toDouble() / (n + baseN)
        val se = sqrt(pooled * (1 - pooled) * (1.0 / n + 1.0 / baseN))
        return if (se > 0) (hits.toDouble() / n - baseHits.toDouble() / baseN) / se else 0.0
    }

    /** True when [hits] of [n] is clearly more than [baseHits] of [baseN]: 20 points or more, and unlikely to be chance. */
    private fun clearlyMore(hits: Int, n: Int, baseHits: Int, baseN: Int): Boolean =
        n > 0 && baseN > 0 && hits.toDouble() / n - baseHits.toDouble() / baseN >= 0.2 && zScore(hits, n, baseHits, baseN) >= 1.65

    private enum class Consistency(val label: String, val types: IntRange, val word: String) {
        Loose("Type 6-7", 6..7, "Looser"), Hard("Type 1-2", 1..2, "Harder"), Steady("Type 3-4", 3..4, "Steadier")
    }

    /** Your last 10 visits against the 40 before them. Until there's a baseline, the last three. */
    private fun consistencyInsights(bowel: List<SessionWithAnswers>): List<InsightItem> {
        val typed = bowel.mapNotNull { it.stoolType }
        val recent = typed.take(10)
        val before = typed.drop(10).take(40)
        if (recent.size < 6 || before.size < 10) {
            val last3 = typed.take(3)
            return listOfNotNull(
                InsightItem("low-fiber", "Pattern suggests low fiber", "Your recent visits lean toward Type 1-2, which can go with constipation or low fiber intake.", Severity.Warning)
                    .takeIf { last3.count { it in 1..2 } >= 2 },
                InsightItem("hydration-issue", "Looser stools lately", "Your recent visits lean toward Type 6-7, which can go with a hydration imbalance or an irritated gut.", Severity.Warning)
                    .takeIf { last3.count { it >= 6 } >= 2 },
            )
        }
        return Consistency.entries.mapNotNull { c ->
            val hits = recent.count { it in c.types }
            val base = before.count { it in c.types }
            if (!clearlyMore(hits, recent.size, base, before.size)) return@mapNotNull null
            val compare = "$hits of your last ${recent.size} visits were ${c.label}, against ${share(base, before.size)} before."
            when (c) {
                Consistency.Loose -> InsightItem("looser-than-usual", "Looser than usual", "$compare Fluids, stress, new foods or a stomach bug are common reasons.", Severity.Warning)
                Consistency.Hard -> InsightItem("harder-than-usual", "Harder than usual", "$compare Water, fiber and moving more usually help.", Severity.Warning)
                Consistency.Steady -> InsightItem("steadier", "Getting steadier", "$compare Whatever you've changed seems to be working.", Severity.Info)
            }
        }
    }

    private fun median(values: List<Double>) = values.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }

    /** Session length against your own usual, plus a gentle note when sittings run long. */
    private fun durationInsights(bowel: List<SessionWithAnswers>): List<InsightItem> {
        val minutes = bowel.map { it.session.durationSeconds / 60.0 }
        val recent = minutes.take(7)
        if (recent.size < 3) return emptyList()
        val lately = median(recent)
        val before = minutes.drop(7).take(30)
        val items = mutableListOf<InsightItem>()
        if (before.size >= 10) {
            val usual = median(before)
            if (lately >= usual * 1.25 && lately - usual >= 2) {
                items += InsightItem("longer-than-usual", "Sessions running longer", "Your recent sessions take about ${lately.roundToInt()} minutes, against ${usual.roundToInt()} before. If it's straining or waiting, water, fiber and not forcing it can help.", Severity.Info)
            }
        }
        if (lately >= 15) {
            items += InsightItem("long-session", "Long sittings", "Your recent sessions take about ${lately.roundToInt()} minutes. Sitting much past 10 to 15 minutes puts pressure on the veins down there, so it can help to get up and come back when you feel the urge.", Severity.Info)
        }
        return items
    }

    private class Effect(val insight: InsightItem, val strength: Double)

    /** [good]: a habit that can make things steadier. "Steadier after poor sleep" is almost always chance. */
    private class Habit(val field: String, val levels: Set<String>, val phrase: String, val good: Boolean = false)

    private val habits = listOf(
        Habit("stress", setOf("high"), "high-stress days"),
        Habit("sleep", setOf("poor"), "nights of poor sleep"),
        Habit("water", setOf("low"), "low-water days"),
        Habit("fiber", setOf("low"), "low-fiber days"),
        Habit("fiber", setOf("high"), "high-fiber days", good = true),
        Habit("caffeine", setOf("medium", "high"), "days with more caffeine"),
        Habit("alcohol", setOf("moderate", "heavy"), "drinking days"),
        Habit("meals", setOf("heavy"), "heavy-meal days"),
        Habit("exercise", setOf("light", "active"), "days you moved", good = true),
    )

    /**
     * Links between your daily check-ins and your stools: visits on a habit's day or the day after,
     * against all your other visits. Shown only with enough of both, a clear difference, and the
     * habit seen in the last 30 days (an old link that no longer happens isn't news).
     */
    private fun habitEffects(bowel: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>, now: Instant = Instant.now()): List<Effect> {
        val visits = bowel.filter { it.stoolType != null && Duration.between(it.session.startTime, now).toDays() <= 120 }
        if (visits.size < 15) return emptyList()
        val effects = mutableListOf<Effect>()

        val monthAgo = now.atZone(zone).toLocalDate().minusDays(30)
        // Many habits are checked at once, so a link needs more than the usual evidence.
        fun compare(id: String, title: (Consistency) -> String, body: (Consistency, Int, Int, String) -> String, days: Int, lately: Boolean, good: Boolean, exposed: (SessionWithAnswers) -> Boolean) {
            val (inside, outside) = visits.partition(exposed)
            if (!lately || days < 3 || inside.size < 5 || outside.size < 10) return
            val (c, hits) = Consistency.entries
                .filter { good || it != Consistency.Steady }
                .map { c -> c to inside.count { it.stoolType!! in c.types } }
                .filter { (c, hits) ->
                    val base = outside.count { it.stoolType!! in c.types }
                    clearlyMore(hits, inside.size, base, outside.size) && zScore(hits, inside.size, base, outside.size) >= 2.0
                }
                .maxByOrNull { (c, hits) -> zScore(hits, inside.size, outside.count { it.stoolType!! in c.types }, outside.size) }
                ?: return
            val base = outside.count { it.stoolType!! in c.types }
            effects += Effect(
                InsightItem("$id-${c.name.lowercase()}", title(c), body(c, hits, inside.size, share(base, outside.size)), Severity.Info),
                zScore(hits, inside.size, base, outside.size),
            )
        }

        for (h in habits) {
            val habitDays = dailyHealth.filter { it.toInput()[h.field].lowercase() in h.levels }.map { it.day }.toSet()
            compare(
                "effect-${h.field}-${h.levels.first()}",
                { c -> "${c.word} after ${h.phrase}" },
                { c, hits, n, other -> "On ${h.phrase} and the day after, $hits of $n visits were ${c.label}, against $other otherwise." },
                habitDays.size,
                habitDays.any { day -> runCatching { LocalDate.parse(day) >= monthAgo }.getOrDefault(false) },
                h.good,
            ) { v -> v.day().toString() in habitDays || v.day().minusDays(1).toString() in habitDays }
        }

        // Going again soon after a visit, against the rest.
        val quick = bowel.zipWithNext { newer, older -> newer.id to (Duration.between(older.session.startTime, newer.session.startTime).toHours() < 12) }
            .filter { it.second }.map { it.first }.toSet()
        compare(
            "effect-quick-return",
            { c -> "${c.word} when you go again soon" },
            { c, hits, n, other -> "When you went again within 12 hours, $hits of $n of those visits were ${c.label}, against $other for the rest." },
            visits.count { it.id in quick },
            visits.any { it.id in quick && it.day() >= monthAgo },
            good = false,
        ) { it.id in quick }
        return effects.sortedByDescending { it.strength }
    }

    /** The Fiber Effect card: a real comparison from your check-ins, or what's missing for one. */
    private fun fiberLabel(sessions: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>): String {
        val bowel = sessions.filter { it.session.kind == SessionKind.Bowel }
        habitEffects(bowel, dailyHealth).firstOrNull { it.insight.id.startsWith("effect-fiber") }?.let { return it.insight.body }
        val fiber = dailyHealth.take(120).map { it.fiber.lowercase() }
        if (fiber.size < 7) return "Fill in the daily health check for a week or two and this shows what fiber does for your stools."
        val (common, count) = fiber.groupingBy { it }.eachCount().maxBy { it.value }
        if (count >= fiber.size * 0.85) {
            return "You've logged ${common.replaceFirstChar { it.uppercase() }} fiber on $count of ${fiber.size} days, so there's nothing to compare yet. Log the days you eat more or less fiber and this will show what it does for you."
        }
        return "No clear link yet between fiber and your stools. It needs a few more days of higher and lower fiber to tell."
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
