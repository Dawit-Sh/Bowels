package com.d_ave.bowels.domain

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
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AnalyticsSummary(
    val visitsPerDay: List<DayCount>,
    val averageDurationSeconds: Long,
    val stoolDistribution: List<Pair<Int, Int>>,
    val totalVisits: Int,
    val totalDurationSeconds: Long,
    val predictedNextTimeLabel: String,
    val predictionNote: String,
    val prediction: Prediction?,
    val milestoneProgressDays: Int,
    val patterns: Patterns? = null,
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
    val weekStart: LocalDate,
)

object Analytics {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun lastDays(days: Int): List<LocalDate> {
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    private fun SessionWithAnswers.day() = session.startTime.localDate(zone)

    fun build(all: List<SessionWithAnswers>): AnalyticsSummary {
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
            patterns = PatternAnalysis.build(sessions),
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

    fun weeklyWrapped(all: List<SessionWithAnswers>): WeeklyWrappedSummary {
        val sessions = Rhythm.distinct(all)
        val days = lastDays(7)
        val window = days.toSet()
        val weekly = sessions.filter { it.session.kind == SessionKind.Bowel && it.day() in window }
        val byDay = weekly.groupingBy { it.day() }.eachCount()
        val perDay = days.map { DayCount(it, byDay[it] ?: 0) }
        val distribution = (1..7).map { type -> type to weekly.count { it.stoolType == type } }
        val total = weekly.sumOf { it.session.durationSeconds }
        return WeeklyWrappedSummary(
            totalVisits = weekly.size,
            averageDurationSeconds = if (weekly.isEmpty()) 0 else (total.toDouble() / weekly.size).roundToInt().toLong(),
            totalDurationSeconds = total,
            visitsPerDay = perDay,
            stoolDistribution = distribution,
            mostCommonStoolType = distribution.maxByOrNull { it.second }?.takeIf { it.second > 0 }?.first,
            bestDay = perDay.maxByOrNull { it.count },
            worstDay = perDay.minByOrNull { it.count },
            weekStart = days.first(),
        )
    }

    /**
     * What stands out in your logs. Safety notes (blood, pain, a three-day gap) use fixed lines;
     * everything else is measured against your own usual, so a pattern that's normal for you
     * doesn't nag.
     */
    fun insights(all: List<SessionWithAnswers>, now: Instant = Instant.now()): List<InsightItem> {
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

        items += rhythmEffects(bowel, now).take(1).map { it.insight }

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

    /**
     * Going again soon after a visit, against all your other visits: shown only with enough of
     * both, a clear difference, and a quick return in the last 30 days.
     */
    private fun rhythmEffects(bowel: List<SessionWithAnswers>, now: Instant = Instant.now()): List<Effect> {
        val visits = bowel.filter { it.stoolType != null && Duration.between(it.session.startTime, now).toDays() <= 120 }
        if (visits.size < 15) return emptyList()
        val monthAgo = now.atZone(zone).toLocalDate().minusDays(30)
        val quick = bowel.zipWithNext { newer, older -> newer.id to (Duration.between(older.session.startTime, newer.session.startTime).toHours() < 12) }
            .filter { it.second }.map { it.first }.toSet()
        val (inside, outside) = visits.partition { it.id in quick }
        if (inside.none { it.day() >= monthAgo } || inside.size < 5 || outside.size < 10) return emptyList()
        return Consistency.entries
            .filter { it != Consistency.Steady }
            .mapNotNull { c ->
                val hits = inside.count { it.stoolType!! in c.types }
                val base = outside.count { it.stoolType!! in c.types }
                val z = zScore(hits, inside.size, base, outside.size)
                if (!clearlyMore(hits, inside.size, base, outside.size) || z < 2.0) return@mapNotNull null
                Effect(
                    InsightItem(
                        "effect-quick-return-${c.name.lowercase()}", "${c.word} when you go again soon",
                        "When you went again within 12 hours, $hits of ${inside.size} of those visits were ${c.label}, against ${share(base, outside.size)} for the rest.",
                        Severity.Info,
                    ),
                    z,
                )
            }
            .sortedByDescending { it.strength }
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
