package com.d_ave.bowels.domain

import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionRecord
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.data.toIsoString
import com.d_ave.bowels.shared.VitalsKeys
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
                put("urgency", s.urgency.toString())
                put("blood", "false")
                if (s.hr != null) {
                    put(VitalsKeys.HR_AVG, s.hr.toString())
                    put(VitalsKeys.HR_MIN, (s.hr - 6).toString())
                    put(VitalsKeys.HR_MAX, (s.hr + 11).toString())
                    put(VitalsKeys.HR_SOURCE, VitalsKeys.SOURCE_WATCH)
                }
                if (s.id % 4 == 1L) {
                    put(VitalsKeys.BP_SYSTOLIC, (118 + s.id % 7).toString())
                    put(VitalsKeys.BP_DIASTOLIC, (76 + s.id % 5).toString())
                    put(VitalsKeys.BP_SOURCE, VitalsKeys.SOURCE_HEALTH_CONNECT)
                }
            }
            SessionWithAnswers(record, answers)
        }.sortedByDescending { it.session.startTime }
    }

    fun dailyHealth(): List<DailyHealthRecord> = (0 until 15).map { i ->
        val day = LocalDate.now().minusDays(i.toLong()).toString()
        val stamp = LocalDate.now().minusDays(i.toLong()).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toIsoString()
        DailyHealthRecord(
            id = i + 1L,
            day = day,
            water = if (i % 6 == 0) "Low" else if (i % 3 == 0) "Okay" else "Great",
            fiber = if (i % 4 == 0) "Low" else if (i % 2 == 0) "High" else "Medium",
            meals = "Balanced",
            stress = if (i % 7 == 0) "High" else if (i % 4 == 0) "Medium" else "Low",
            sleep = if (i % 5 == 0) "Poor" else if (i % 3 == 0) "Fair" else "Great",
            exercise = if (i % 3 == 0) "None" else if (i % 2 == 0) "Light" else "Active",
            caffeine = if (i % 5 == 0) "High" else if (i % 3 == 0) "Medium" else if (i % 2 == 0) "Low" else "None",
            alcohol = if (i % 8 == 0) "Moderate" else if (i % 10 == 0) "Light" else "None",
            medication = if (i % 9 == 0) "OTC" else "None",
            mood = if (i % 7 == 0) "Stressed" else if (i % 5 == 0) "Anxious" else if (i % 4 == 0) "Neutral" else if (i % 3 == 0) "Sad" else "Happy",
            createdAt = stamp,
            updatedAt = stamp,
        )
    }
}
