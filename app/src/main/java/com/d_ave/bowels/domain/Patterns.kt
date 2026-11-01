package com.d_ave.bowels.domain

import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class WeekPoint(val start: LocalDate, val visits: Int, val steadyShare: Double?, val medianMinutes: Double?)

data class SymptomRate(val name: String, val recent: Int, val before: Int?)

data class Patterns(
    /** 0-100: how regular the last 30 days were (timing, form, frequency), or null without enough visits. */
    val regularity: Int?,
    val regularityParts: List<Pair<String, Int>>,
    /** Visits per hour of the day, last 30 days. */
    val hourly: List<Int>,
    /** Average visits on each weekday (Monday first), last 8 weeks. */
    val weekday: List<Double>,
    /** The last 8 weeks, oldest first. */
    val weeks: List<WeekPoint>,
    val symptoms: List<SymptomRate>,
    val currentStreak: Int,
    val longestStreak: Int,
    val usualGapHours: Double?,
    val longestGapHours: Double?,
    val visitsPerDay: Double,
)

object PatternAnalysis {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun median(values: List<Double>) = values.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }

    fun build(all: List<SessionWithAnswers>, today: LocalDate = LocalDate.now()): Patterns {
        val visits = Rhythm.distinct(all).filter { it.session.kind == SessionKind.Bowel }.sortedBy { it.session.startTime }
        fun day(s: SessionWithAnswers) = s.session.startTime.atZone(zone).toLocalDate()
        val last30 = visits.filter { day(it) > today.minusDays(30) }
        val before30 = visits.filter { day(it) <= today.minusDays(30) && day(it) > today.minusDays(90) }

        val hourly = IntArray(24).also { h -> last30.forEach { h[it.session.startTime.atZone(zone).hour]++ } }.toList()

        val eightWeeks = visits.filter { day(it) > today.minusDays(56) }
        val firstDay = visits.firstOrNull()?.let(::day)
        val weekday = DayOfWeek.entries.map { dow ->
            val days = (0 until 56).map { today.minusDays(it.toLong()) }.filter { it.dayOfWeek == dow && (firstDay == null || it >= firstDay) }
            if (days.isEmpty()) 0.0 else eightWeeks.count { day(it).dayOfWeek == dow }.toDouble() / days.size
        }

        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeks = (7 downTo 0).map { back ->
            val start = thisMonday.minusWeeks(back.toLong())
            val inWeek = visits.filter { day(it) >= start && day(it) < start.plusDays(7) }
            val typed = inWeek.mapNotNull { it.stoolType }
            WeekPoint(
                start, inWeek.size,
                typed.takeIf { it.isNotEmpty() }?.let { t -> t.count { it in 3..4 }.toDouble() / t.size },
                inWeek.takeIf { it.isNotEmpty() }?.let { w -> median(w.map { it.session.durationSeconds / 60.0 }) },
            )
        }

        fun rate(list: List<SessionWithAnswers>, test: (SessionWithAnswers) -> Boolean) =
            if (list.isEmpty()) null else (100.0 * list.count(test) / list.size).roundToInt()
        fun tag(name: String) = { s: SessionWithAnswers -> name in s.answers["adaptive_tags"].orEmpty() }
        val symptomTests = listOf(
            "Gas" to tag("gas"),
            "Bloating" to tag("bloating"),
            "Straining" to tag("straining"),
            "High urgency" to { s: SessionWithAnswers -> (s.answers["urgency"]?.toIntOrNull() ?: 0) >= 4 },
            "Felt incomplete" to { s: SessionWithAnswers -> s.answers["completion"] == "incomplete" },
            "Pain" to { s: SessionWithAnswers -> s.answers["pain"] in setOf("mild", "moderate", "severe") },
        )
        val symptoms = if (last30.isEmpty()) emptyList() else symptomTests.map { (name, test) ->
            SymptomRate(name, rate(last30, test) ?: 0, rate(before30, test))
        }

        // Logged-day streaks.
        val days = visits.map(::day).toSortedSet()
        var longest = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days) {
            run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
            longest = maxOf(longest, run)
            prev = d
        }
        var current = 0
        var cursor = if (today in days) today else today.minusDays(1)
        while (cursor in days) { current++; cursor = cursor.minusDays(1) }

        val gaps = last30.zipWithNext { a, b -> Duration.between(a.session.startTime, b.session.startTime).toMinutes() / 60.0 }

        // Regularity: timing (how spread your first visit of the day is), form (Types 3-4, half
        // credit for 5), frequency (days within the normal 3 a week to 3 a day, NHANES).
        var regularity: Int? = null
        var parts = emptyList<Pair<String, Int>>()
        if (last30.size >= 10) {
            val firsts = last30.groupBy(::day).values.map { v -> v.first().session.startTime.atZone(zone).let { it.hour * 60 + it.minute } }
            val x = firsts.sumOf { cos(2 * PI * it / 1440) } / firsts.size
            val y = firsts.sumOf { sin(2 * PI * it / 1440) } / firsts.size
            val r = sqrt(x * x + y * y).coerceIn(1e-6, 1.0)
            val spreadMinutes = sqrt(-2 * ln(r)) * 1440 / (2 * PI)
            val timing = (100 * (1 - (spreadMinutes - 30) / 210)).roundToInt().coerceIn(0, 100)
            val typed = last30.mapNotNull { it.stoolType }
            val form = if (typed.isEmpty()) 50 else (100.0 * (typed.count { it in 3..4 } + 0.5 * typed.count { it == 5 }) / typed.size).roundToInt()
            val start = maxOf(today.minusDays(29), days.first())
            val window = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= today }.toList()
            val perDay = last30.groupingBy(::day).eachCount()
            val okDays = window.count { d -> (perDay[d] ?: 0) <= 3 && (0L..2L).any { (perDay[d.minusDays(it)] ?: 0) > 0 } }
            val frequency = (100.0 * okDays / window.size).roundToInt()
            parts = listOf("Timing" to timing, "Form" to form, "Frequency" to frequency)
            regularity = parts.map { it.second }.average().roundToInt()
        }

        val spanDays = (firstDay?.let { maxOf(1, minOf(30, (today.toEpochDay() - it.toEpochDay() + 1).toInt())) } ?: 30)
        return Patterns(
            regularity, parts, hourly, weekday, weeks, symptoms, current, longest,
            gaps.takeIf { it.size >= 3 }?.let { median(it) }, gaps.maxOrNull(),
            last30.size.toDouble() / spanDays,
        )
    }
}
