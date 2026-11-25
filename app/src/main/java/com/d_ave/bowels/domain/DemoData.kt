package com.d_ave.bowels.domain

import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionRecord
import com.d_ave.bowels.data.SessionWithAnswers
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Shown until the first real log, so the app doesn't open empty. */
object DemoData {
    private data class Seed(val id: Long, val daysAgo: Long, val hour: Int, val minute: Int, val minutes: Long, val stool: Int, val pain: String, val urgency: Int, val hr: Int?)

    private val seeds = listOf(
        Seed(1, 0, 7, 42, 4, 4, "none", 3, 74), Seed(2, 1, 8, 5, 5, 3, "none", 2, 71),
        Seed(3, 1, 14, 30, 3, 4, "mild", 4, null), Seed(4, 2, 7, 55, 4, 4, "none", 3, 76),
        Seed(5, 3, 8, 18, 3, 4, "none", 2, 70), Seed(6, 3, 16, 45, 6, 5, "mild", 5, 88),
        Seed(7, 4, 7, 51, 4, 3, "none", 3, 73), Seed(8, 5, 8, 11, 5, 4, "none", 2, null),
        Seed(9, 6, 7, 48, 4, 4, "none", 3, 75), Seed(10, 6, 19, 20, 3, 5, "none", 4, 79),
        Seed(11, 7, 8, 8, 3, 4, "none", 2, 72), Seed(12, 8, 9, 15, 7, 2, "moderate", 5, 94),
        Seed(13, 9, 7, 35, 4, 4, "none", 3, 74), Seed(14, 10, 8, 22, 5, 3, "none", 2, null),
        Seed(15, 10, 15, 10, 4, 4, "mild", 4, 77), Seed(16, 11, 7, 58, 4, 4, "none", 3, 73),
        Seed(17, 12, 8, 40, 6, 5, "mild", 5, 86), Seed(18, 13, 7, 25, 3, 4, "none", 2, 71),
        Seed(19, 14, 8, 12, 5, 3, "none", 3, 72), Seed(20, 14, 20, 5, 4, 4, "none", 4, null),
    )

    fun sessions(): List<SessionWithAnswers> {
        val zone = ZoneId.systemDefault()
        return seeds.map { s ->
            val start = LocalDate.now().minusDays(s.daysAgo).atTime(LocalTime.of(s.hour, s.minute)).atZone(zone).toInstant()
            val record = SessionRecord(s.id, SessionKind.Bowel, start, start.plusSeconds(s.minutes * 60), s.minutes * 60, start)
            val answers = buildMap {
                put("stool_type", s.stool.toString())
                put("pain", s.pain)
                put("blood", "false")
            }
            SessionWithAnswers(record, answers)
        }.sortedByDescending { it.session.startTime }
    }
}
