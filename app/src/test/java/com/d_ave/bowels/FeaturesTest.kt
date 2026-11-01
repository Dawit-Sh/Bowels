package com.d_ave.bowels

import com.d_ave.bowels.data.AppSettings
import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionRecord
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.domain.Achievements
import com.d_ave.bowels.domain.ForecastModel
import com.d_ave.bowels.domain.Guidance
import com.d_ave.bowels.domain.Outlook
import com.d_ave.bowels.domain.PatternAnalysis
import com.d_ave.bowels.domain.Research
import com.d_ave.bowels.domain.SessionDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class FeaturesTest {
    private var nextId = 1L
    private val zone = ZoneId.systemDefault()

    private fun at(day: LocalDate, hour: Int, stool: Int = 4, minutes: Long = 6, extra: Map<String, String> = emptyMap()): SessionWithAnswers {
        val start = day.atTime(hour, 0).atZone(zone).toInstant()
        return SessionWithAnswers(SessionRecord(nextId++, SessionKind.Bowel, start, start.plusSeconds(minutes * 60), minutes * 60, start), mapOf("stool_type" to "$stool") + extra)
    }

    private fun health(day: LocalDate, stress: String = "Medium", water: String = "Okay", fiber: String = "Medium") =
        DailyHealthRecord(0, day.toString(), water, fiber, "Balanced", stress, "Fair", "Light", "None", "None", "None", "Neutral", "", "")

    @Test
    fun `reminder times round trip, and old single hours carry over`() {
        val s = AppSettings(reminderTimes = listOf(LocalTime.of(8, 30), LocalTime.of(21, 0)))
        assertEquals(listOf(LocalTime.of(8, 30), LocalTime.of(21, 0)), AppSettings.fromMap(s.toMap()).reminderTimes)
        assertEquals(listOf(LocalTime.of(19, 0)), AppSettings.fromMap(mapOf("reminderHour" to "19")).reminderTimes)
        // Removing every reminder is allowed and stays empty.
        assertTrue(AppSettings.fromMap(AppSettings(reminderTimes = emptyList()).toMap()).reminderTimes.isEmpty())
    }

    @Test
    fun `the forecast learns that stress leads to looser days`() {
        val today = LocalDate.now()
        val days = (90L downTo 1L).map { today.minusDays(it) }
        val stressed = days.filterIndexed { i, _ -> i % 4 == 0 }.toSet()
        val sessions = days.map { d -> at(d, 8, stool = if (d.minusDays(1) in stressed) 7 else 4) }
        val health = (days + today).map { health(it, stress = if (it in stressed || it == today) "High" else "Medium") }.sortedByDescending { it.day }
        val f = ForecastModel.forecast(sessions, health, today.atTime(20, 0).atZone(zone))
        assertNotNull(f)
        assertEquals(Outlook.Looser, f!!.outlook)
        assertTrue(f.drivers.toString(), f.drivers.any { it.text.contains("stress") && it.towards == Outlook.Looser })
        assertTrue("${f.hits}/${f.checked} vs ${f.baselineHits}", f.hits > f.baselineHits)
    }

    @Test
    fun `guidance reads hard stools and cites the research`() {
        val today = LocalDate.now()
        val sessions = (20L downTo 1L).map { at(today.minusDays(it), 8, stool = if (it % 2 == 0L) 1 else 4, extra = mapOf("adaptive_tags" to "straining")) }
            .sortedByDescending { it.session.startTime }
        val advice = Guidance.forYou(sessions, emptyList(), PatternAnalysis.build(sessions))
        val hard = advice.first { it.id == "hard" }
        assertTrue(Research.fiberConstipation in hard.papers)
        assertTrue(hard.noticed, hard.noticed.contains("Type 1-2"))
        assertEquals("start", Guidance.forYou(emptyList(), emptyList(), null).single().id)
    }

    @Test
    fun `achievements count streaks and steady runs`() {
        val today = LocalDate.now()
        val sessions = (4L downTo 0L).map { at(today.minusDays(it), 7) }
        val progress = Achievements.progress(sessions, emptyList()).associateBy { it.achievement.id }
        assertTrue(progress.getValue("streak-3").earned)
        assertTrue(progress.getValue("smooth-3").earned)
        assertTrue(progress.getValue("early-bird").current == 5)
        assertFalse(progress.getValue("visits-10").earned)
    }

    @Test
    fun `a session's detail compares it with the visits before it`() {
        val today = LocalDate.now()
        val usual = (20L downTo 2L).map { at(today.minusDays(it), 8, minutes = 6) }
        val long = at(today.minusDays(1), 8, minutes = 20)
        val detail = SessionDetails.build(long, (usual + long).sortedByDescending { it.session.startTime }, emptyList())
        val length = detail.comparisons.first { it.label == "Length" }
        assertTrue(length.note, length.note.contains("longer than your usual 6"))
        assertTrue(detail.comparisons.first { it.label == "Time" }.note.startsWith("Right on your usual time"))
    }

    @Test
    fun `a regular month scores high on regularity`() {
        val today = LocalDate.now()
        val sessions = (29L downTo 0L).map { at(today.minusDays(it), 7, stool = 4) }
        val p = PatternAnalysis.build(sessions, today)
        assertTrue("${p.regularity}", (p.regularity ?: 0) >= 90)
        assertEquals(30, p.hourly.sum())
        assertEquals(30, p.currentStreak)
    }
}
