package com.d_ave.bowels

import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionRecord
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
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
        assertNull(w.averageHeartRate)
    }

    @Test
    fun `heart rate from the watch shows up in weekly wrapped and vitals summary`() {
        val hr = { avg: Int -> mapOf(VitalsKeys.HR_AVG to "$avg", VitalsKeys.HR_MAX to "${avg + 10}") }
        val sessions = listOf(session(0, 8, 4, hr(70)), session(1, 8, 4, hr(80)), session(2, 8, 4))
        assertEquals(75, Analytics.weeklyWrapped(sessions).averageHeartRate)
        val v = Analytics.vitalsSummary(sessions)
        assertEquals(2, v.sessionsWithHeartRate)
        assertEquals(90, v.peakHeartRate)
    }

    @Test
    fun `high blood pressure around sessions becomes a calm warning`() {
        val bp = mapOf(VitalsKeys.BP_SYSTOLIC to "146", VitalsKeys.BP_DIASTOLIC to "92")
        val sessions = listOf(session(0, 8, 4, bp), session(1, 8, 4, bp))
        val ids = Analytics.insights(sessions, emptyList()).map { it.id }
        assertTrue("bp-elevated" in ids)
    }

    @Test
    fun `straining sessions with a higher heart rate are noticed`() {
        val strained = { avg: Int -> mapOf("adaptive_tags" to "straining", VitalsKeys.HR_AVG to "$avg") }
        val relaxed = { avg: Int -> mapOf(VitalsKeys.HR_AVG to "$avg") }
        val sessions = listOf(session(0, 8, 4, strained(92)), session(1, 8, 4, strained(96)), session(2, 8, 4, relaxed(72)), session(3, 8, 4, relaxed(74)))
        assertTrue("hr-straining" in Analytics.insights(sessions, emptyList()).map { it.id })
    }

    @Test
    fun `steady when nothing stands out`() {
        val ids = Analytics.insights(listOf(session(0, 8, 4)), emptyList()).map { it.id }
        assertEquals(listOf("steady"), ids)
    }

    @Test
    fun `quirky line is stable for the week`() {
        val w = Analytics.weeklyWrapped(listOf(session(0, 8, 4)))
        assertEquals(Analytics.quirkyInsight(w), Analytics.quirkyInsight(w))
    }

    @Test
    fun `prediction needs three bowel sessions`() {
        assertEquals("Need more data", Analytics.build(listOf(session(0, 8, 4)), emptyList()).predictedNextTimeLabel)
        val label = Analytics.build(listOf(session(0, 8, 4), session(1, 8, 4), session(2, 8, 4)), emptyList()).predictedNextTimeLabel
        assertTrue(label, label.isNotBlank() && label != "Building pattern...")
    }

    @Test
    fun `prediction keeps your usual time of day`() {
        // Morning habit with an evening outlier, like the demo data that used to predict 12:22 AM.
        val sessions = (0L..9L).map { session(it, 7, 4) } + session(1, 19, 4)
        val label = Analytics.build(sessions.sortedByDescending { it.session.startTime }, emptyList()).predictedNextTimeLabel
        assertTrue(label, label.contains("7:00"))
    }

    @Test
    fun `update check compares versions numerically`() {
        assertTrue(UpdateChecker.isNewer("3.0.0", "v3.0.1"))
        assertTrue(UpdateChecker.isNewer("2.9.10", "v3.0.0"))
        assertFalse(UpdateChecker.isNewer("3.0.0", "v3.0.0"))
        assertFalse(UpdateChecker.isNewer("3.1.0", "v3.0.9"))
    }
}
