package com.d_ave.bowels.domain

import com.d_ave.bowels.data.DailyHealthInput
import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import java.time.Duration
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** One line of "how this visit compares with your usual". */
data class Comparison(val label: String, val value: String, val note: String)

data class SessionDetail(
    val session: SessionWithAnswers,
    val comparisons: List<Comparison>,
    /** The health check of that day and of the day before, when filled in. */
    val dayHealth: DailyHealthInput?,
    val dayBeforeHealth: DailyHealthInput?,
    /** Links from your own history that fit this day ("after high-stress days you run looser"). */
    val links: List<String>,
    /** Where this visit sits among your visits: "1 of 3 that day", "visit 87". */
    val ordinal: String,
    val stoolMeaning: String?,
)

object SessionDetails {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun minuteOfDay(s: SessionWithAnswers) = s.session.startTime.atZone(zone).let { it.hour * 60 + it.minute }

    private fun clock(minute: Int): String {
        val h = minute / 60 % 24
        val m = minute % 60
        return "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")
    }

    private fun circularMean(minutes: List<Int>): Int {
        val x = minutes.sumOf { cos(2 * PI * it / 1440) }
        val y = minutes.sumOf { sin(2 * PI * it / 1440) }
        return Math.floorMod((atan2(y, x) / (2 * PI) * 1440).roundToInt(), 1440)
    }

    private fun median(values: List<Double>) = values.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }

    fun build(target: SessionWithAnswers, all: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>): SessionDetail {
        val visits = Rhythm.distinct(all).filter { it.session.kind == target.session.kind }.sortedBy { it.session.startTime }
        val index = visits.indexOfFirst { it.id == target.id }.takeIf { it >= 0 }
            ?: visits.indexOfLast { it.session.startTime <= target.session.startTime }.coerceAtLeast(0)
        // "Your usual" is the 60 days before this visit, so old visits compare with their own time.
        val from = target.session.startTime.minus(Duration.ofDays(60))
        val before = visits.subList(0, index).filter { it.session.startTime >= from }
        val comparisons = mutableListOf<Comparison>()

        val minutes = target.session.durationSeconds / 60.0
        if (before.size >= 5) {
            val usual = median(before.map { it.session.durationSeconds / 60.0 })
            val diff = minutes - usual
            comparisons += Comparison(
                "Length", "${minutes.roundToInt()} min",
                when {
                    abs(diff) < 1.5 -> "About your usual ${usual.roundToInt()} min."
                    diff > 0 -> "${diff.roundToInt()} min longer than your usual ${usual.roundToInt()}."
                    else -> "${(-diff).roundToInt()} min shorter than your usual ${usual.roundToInt()}."
                },
            )
            val usualMinute = circularMean(before.map(::minuteOfDay))
            val off = Math.floorMod(minuteOfDay(target) - usualMinute + 720, 1440) - 720
            comparisons += Comparison(
                "Time", clock(minuteOfDay(target)),
                when {
                    abs(off) <= 45 -> "Right on your usual time (around ${clock(usualMinute)})."
                    off > 0 -> "${hours(off)} later than your usual ${clock(usualMinute)}."
                    else -> "${hours(-off)} earlier than your usual ${clock(usualMinute)}."
                },
            )
        } else {
            comparisons += Comparison("Length", "${minutes.roundToInt()} min", "Comparisons start after a few more visits.")
        }

        visits.getOrNull(index - 1)?.let { prev ->
            val gap = Duration.between(prev.session.startTime, target.session.startTime).toMinutes() / 60.0
            val gaps = before.zipWithNext { a, b -> Duration.between(a.session.startTime, b.session.startTime).toMinutes() / 60.0 }
            comparisons += Comparison(
                "Since the last visit", if (gap >= 48) "${(gap / 24).roundToInt()} days" else "${gap.roundToInt()} h",
                if (gaps.size >= 5) {
                    val usual = median(gaps)
                    when {
                        gap < usual * 0.6 -> "Sooner than your usual ${usual.roundToInt()} h."
                        gap > usual * 1.5 -> "Longer than your usual ${usual.roundToInt()} h."
                        else -> "About your usual ${usual.roundToInt()} h."
                    }
                } else "",
            )
        }

        val type = target.stoolType
        if (type != null && target.session.kind == SessionKind.Bowel) {
            val typed = before.mapNotNull { it.stoolType }
            if (typed.size >= 5) {
                val same = typed.count { it == type }
                comparisons += Comparison(
                    "Stool", "Type $type",
                    "${(100.0 * same / typed.size).roundToInt()}% of your visits in the 60 days before were Type $type.",
                )
            }
        }
        target.vitals.heartRateAvg?.let { hr ->
            val usual = before.mapNotNull { it.vitals.heartRateAvg }
            comparisons += Comparison(
                "Heart rate", "$hr bpm" + (target.vitals.heartRateMax?.let { " (peak $it)" } ?: ""),
                if (usual.size >= 3) {
                    val avg = usual.average().roundToInt()
                    when {
                        hr - avg >= 8 -> "${hr - avg} bpm above your usual $avg. Straining often does this."
                        avg - hr >= 8 -> "${avg - hr} bpm below your usual $avg."
                        else -> "About your usual $avg bpm."
                    }
                } else "",
            )
        }

        val day = target.session.startTime.atZone(zone).toLocalDate()
        val byDay = dailyHealth.associateBy { it.day }
        val dayHealth = byDay[day.toString()]?.toInput()
        val dayBefore = byDay[day.minusDays(1).toString()]?.toInput()

        val sameDay = visits.filter { it.session.startTime.atZone(zone).toLocalDate() == day }
        val ordinal = buildString {
            append("Visit ${index + 1}")
            if (sameDay.size > 1) append(" · ${sameDay.indexOfFirst { it.id == target.id } + 1} of ${sameDay.size} that day")
        }

        val links = Analytics.linksFor(day, all, dailyHealth)

        val meaning = when (type) {
            1, 2 -> "Types 1-2 mean food moved slowly through the gut, the pattern of constipation."
            3, 4 -> "Types 3-4 are the usual, healthy form."
            5 -> "Type 5 is on the soft side: a little faster than ideal, usually nothing to worry about."
            6, 7 -> "Types 6-7 mean food moved through fast, the pattern of diarrhoea."
            else -> null
        }
        return SessionDetail(target, comparisons, dayHealth, dayBefore, links, ordinal, meaning)
    }

    private fun hours(minutes: Int) = if (minutes < 90) "$minutes min" else "${(minutes / 60.0).roundToInt()} h"
}
