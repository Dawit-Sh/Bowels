package com.d_ave.bowels

import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionRecord
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.Rhythm
import com.d_ave.bowels.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class AnalyticsTest {
    private var nextId = 1L

    private fun session(daysAgo: Long, hour: Int, stool: Int, extra: Map<String, String> = emptyMap(), minutes: Long = 4): SessionWithAnswers {
        val start = LocalDate.now().minusDays(daysAgo).atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant()
        return SessionWithAnswers(
            SessionRecord(nextId++, SessionKind.Bowel, start, start.plusSeconds(minutes * 60), minutes * 60, start),
            mapOf("stool_type" to stool.toString()) + extra,
        )
    }

    @Test
    fun `weekly wrapped counts only the last seven local days`() {
        val sessions = listOf(session(0, 8, 4), session(0, 20, 4), session(3, 7, 3), session(7, 7, 1)).sortedByDescending { it.session.startTime }
        val w = Analytics.weeklyWrapped(sessions)
        assertEquals(3, w.totalVisits)
        assertEquals(4, w.mostCommonStoolType)
        assertEquals(LocalDate.now(), w.bestDay!!.day)
        assertEquals(2, w.bestDay!!.count)
    }

    @Test
    fun `steady when nothing stands out`() {
        val ids = Analytics.insights(listOf(session(0, 8, 4))).map { it.id }
        assertEquals(listOf("steady"), ids)
    }

    @Test
    fun `quirky line is stable for the week`() {
        val w = Analytics.weeklyWrapped(listOf(session(0, 8, 4)))
        assertEquals(Analytics.quirkyInsight(w), Analytics.quirkyInsight(w))
    }

    @Test
    fun `prediction needs three bowel sessions`() {
        assertEquals("Need more data", Analytics.build(listOf(session(0, 8, 4))).predictedNextTimeLabel)
        val label = Analytics.build(listOf(session(0, 8, 4), session(1, 8, 4), session(2, 8, 4))).predictedNextTimeLabel
        assertTrue(label, label.isNotBlank() && label != "Building pattern...")
    }

    @Test
    fun `prediction keeps your usual time of day`() {
        // Morning habit with an evening outlier, like the demo data that used to predict 12:22 AM.
        val sessions = (1L..10L).map { session(it, 7, 4) } + session(2, 19, 4)
        val p = Analytics.build(sessions.sortedByDescending { it.session.startTime }).prediction!!
        val seven = LocalTime.of(7, 0)
        assertTrue("${p.from}..${p.to}", !p.from.toLocalTime().isAfter(seven) && !p.to.toLocalTime().isBefore(seven))
    }

    private fun at(day: LocalDate, hour: Int, minute: Int = 0, stool: Int = 4, minutes: Long = 6, extra: Map<String, String> = emptyMap()): SessionWithAnswers {
        val start = day.atTime(hour, minute).atZone(zone).toInstant()
        return SessionWithAnswers(SessionRecord(nextId++, SessionKind.Bowel, start, start.plusSeconds(minutes * 60), minutes * 60, start), mapOf("stool_type" to "$stool") + extra)
    }

    private val zone = ZoneId.systemDefault()

    @Test
    fun `twice a day, the evening comes next after a morning visit`() {
        val today = LocalDate.of(2026, 6, 15)
        val visits = (14L downTo 1L).flatMap { listOf(at(today.minusDays(it), 7), at(today.minusDays(it), 19)) } + at(today, 7)
        val p = Rhythm.predict(visits, today.atTime(8, 0).atZone(zone))!!
        assertEquals(today, p.at.toLocalDate())
        assertTrue("${p.at}", p.at.hour in 18..19)
        // It checks itself against your own history, and a steady rhythm passes.
        assertTrue("${p.hits}/${p.checked}", p.checked >= 10 && p.hits >= p.checked * 0.8)
    }

    @Test
    fun `a visit logged twice counts once`() {
        val today = LocalDate.now()
        val once = at(today.minusDays(1), 8)
        val twice = SessionWithAnswers(once.session.copy(id = nextId++), once.answers)
        val s = Analytics.build(listOf(once, twice, at(today.minusDays(2), 8)))
        assertEquals(2, s.totalVisits)
        assertEquals(2, s.stoolDistribution.first { it.first == 4 }.second)
    }

    @Test
    fun `looser than usual is measured against your own baseline`() {
        val today = LocalDate.now()
        val usual = (50L downTo 11L).map { at(today.minusDays(it), 8, stool = 4) }
        val lately = (10L downTo 1L).map { at(today.minusDays(it), 8, stool = if (it % 2 == 0L) 7 else 4) }
        val ids = Analytics.insights((usual + lately).sortedByDescending { it.session.startTime }).map { it.id }
        assertTrue(ids.toString(), "looser-than-usual" in ids)

        // Someone who is always Type 5-6 isn't nagged about it.
        val steady = (50L downTo 1L).map { at(today.minusDays(it), 8, stool = if (it % 3 == 0L) 6 else 5) }
        val steadyIds = Analytics.insights(steady.sortedByDescending { it.session.startTime }).map { it.id }
        assertFalse(steadyIds.toString(), steadyIds.any { it in setOf("looser-than-usual", "hydration-issue") })
    }

    @Test
    fun `sessions are long only against your own usual`() {
        // Twelve minutes every time used to trip "Long sessions recurring" on every visit.
        val today = LocalDate.now()
        val usual = (40L downTo 1L).map { at(today.minusDays(it), 8, minutes = 12) }
        val ids = Analytics.insights(usual.sortedByDescending { it.session.startTime }).map { it.id }
        assertFalse(ids.toString(), ids.any { it in setOf("long-session", "longer-than-usual") })

        val longer = (usual.dropLast(5) + (5L downTo 1L).map { at(today.minusDays(it), 20, minutes = 18) })
        val longerIds = Analytics.insights(longer.sortedByDescending { it.session.startTime }).map { it.id }
        assertTrue(longerIds.toString(), "longer-than-usual" in longerIds)
    }

    @Test
    fun `update check compares versions numerically`() {
        assertTrue(UpdateChecker.isNewer("3.0.0", "v3.0.1"))
        assertTrue(UpdateChecker.isNewer("2.9.10", "v3.0.0"))
        assertFalse(UpdateChecker.isNewer("3.0.0", "v3.0.0"))
        assertFalse(UpdateChecker.isNewer("3.1.0", "v3.0.9"))
    }
}
