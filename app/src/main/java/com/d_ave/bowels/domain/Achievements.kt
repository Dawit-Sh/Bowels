package com.d_ave.bowels.domain

import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.shared.VitalsKeys
import java.time.LocalDate
import java.time.ZoneId

enum class AchievementGroup(val label: String) { Logging("Logging"), Streaks("Streaks"), Body("Body"), Watch("Watch") }

data class Achievement(
    val id: String,
    val title: String,
    val body: String,
    val emoji: String,
    val group: AchievementGroup,
    val target: Int,
)

data class AchievementProgress(val achievement: Achievement, val current: Int) {
    val earned get() = current >= achievement.target
    val fraction get() = (current.toFloat() / achievement.target).coerceIn(0f, 1f)
}

/** Everything you can earn besides the logged-day milestones, and how far along you are. */
object Achievements {
    val all = listOf(
        Achievement("first-log", "First Log", "Log your first visit.", "🌱", AchievementGroup.Logging, 1),
        Achievement("visits-10", "Getting Started", "Log 10 visits.", "📝", AchievementGroup.Logging, 10),
        Achievement("visits-50", "Regular", "Log 50 visits.", "📚", AchievementGroup.Logging, 50),
        Achievement("visits-100", "Centurion", "Log 100 visits.", "💯", AchievementGroup.Logging, 100),
        Achievement("visits-250", "Archivist", "Log 250 visits.", "🗂️", AchievementGroup.Logging, 250),
        Achievement("visits-500", "Lifelong Logger", "Log 500 visits.", "🏛️", AchievementGroup.Logging, 500),
        Achievement("streak-3", "Three in a Row", "Log on 3 days in a row.", "🔥", AchievementGroup.Streaks, 3),
        Achievement("streak-14", "Two Steady Weeks", "Log on 14 days in a row.", "📆", AchievementGroup.Streaks, 14),
        Achievement("streak-60", "Unbroken", "Log on 60 days in a row.", "⛓️", AchievementGroup.Streaks, 60),
        Achievement("smooth-3", "Smooth Operator", "Three Type 3-4 visits in a row.", "🐍", AchievementGroup.Body, 3),
        Achievement("smooth-10", "Steady State", "Ten Type 3-4 visits in a row.", "🎯", AchievementGroup.Body, 10),
        Achievement("quick-10", "In and Out", "Ten visits under 5 minutes.", "⚡", AchievementGroup.Body, 10),
        Achievement("early-bird", "Early Bird", "Ten visits before 8 AM.", "🌅", AchievementGroup.Body, 10),
        Achievement("clockwork", "Clockwork", "Five days in a row with your first visit within an hour of the day before's.", "⏰", AchievementGroup.Body, 5),
        Achievement("watch-1", "On Your Wrist", "Log a session from your watch.", "⌚", AchievementGroup.Watch, 1),
        Achievement("watch-25", "Hands Free", "Log 25 sessions from your watch.", "🙌", AchievementGroup.Watch, 25),
    )

    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun progress(sessions: List<SessionWithAnswers>): List<AchievementProgress> {
        val visits = Rhythm.distinct(sessions).filter { it.session.kind == SessionKind.Bowel }.sortedBy { it.session.startTime }
        fun day(s: SessionWithAnswers): LocalDate = s.session.startTime.atZone(zone).toLocalDate()
        val days = visits.map(::day).toSortedSet()

        fun longestRun(dates: Collection<LocalDate>): Int {
            var best = 0
            var run = 0
            var prev: LocalDate? = null
            for (d in dates.sorted()) {
                run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
                best = maxOf(best, run)
                prev = d
            }
            return best
        }
        fun longestTrue(flags: List<Boolean>): Int {
            var best = 0
            var run = 0
            for (f in flags) { run = if (f) run + 1 else 0; best = maxOf(best, run) }
            return best
        }

        val streak = longestRun(days)
        val smooth = longestTrue(visits.mapNotNull { it.stoolType }.map { it in 3..4 })
        val firstOfDay = visits.groupBy(::day).toSortedMap().mapValues { (_, v) -> v.first().session.startTime.atZone(zone).let { it.hour * 60 + it.minute } }
        var clockwork = 0
        var run = 0
        var prevDay: LocalDate? = null
        var prevMinute = 0
        for ((d, m) in firstOfDay) {
            val diff = Math.floorMod(m - prevMinute + 720, 1440) - 720
            run = if (prevDay != null && d == prevDay.plusDays(1) && kotlin.math.abs(diff) <= 60) run + 1 else 1
            clockwork = maxOf(clockwork, run)
            prevDay = d
            prevMinute = m
        }
        val watch = visits.count { it.answers[VitalsKeys.WATCH_ID] != null }

        val value = mapOf(
            "first-log" to visits.size, "visits-10" to visits.size, "visits-50" to visits.size, "visits-100" to visits.size,
            "visits-250" to visits.size, "visits-500" to visits.size,
            "streak-3" to streak, "streak-14" to streak, "streak-60" to streak,
            "smooth-3" to smooth, "smooth-10" to smooth,
            "quick-10" to visits.count { it.session.durationSeconds in 61 until 300 },
            "early-bird" to visits.count { it.session.startTime.atZone(zone).hour < 8 },
            "clockwork" to clockwork,
            "watch-1" to watch, "watch-25" to watch,
        )
        return all.map { AchievementProgress(it, value[it.id] ?: 0) }
    }
}
