package com.d_ave.bowels

import com.d_ave.bowels.data.ArchiveNormalizer
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionRecord
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.data.parseInstant
import com.d_ave.bowels.domain.Analytics
import com.d_ave.bowels.domain.Rhythm
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.ZoneId

/**
 * Backtests the prediction on a real export, which stays on your computer and out of the repo:
 *
 *     BOWELS_EXPORT=/path/to/bowels-export.json ./gradlew :app:testDebugUnitTest --tests '*PersonalExportBacktest*' -i
 *
 * Skipped when BOWELS_EXPORT isn't set.
 */
class PersonalExportBacktest {
    @Test
    fun `prediction on a real export`() {
        val path = System.getenv("BOWELS_EXPORT")
        assumeTrue("BOWELS_EXPORT not set", path != null && File(path).exists())
        val archive = ArchiveNormalizer.normalize(JSONObject(File(path!!).readText()))
        val answers = archive.answers.groupBy { it.sessionId }.mapValues { (_, a) -> a.associate { it.key to it.value } }
        val all = archive.sessions.map { s ->
            SessionWithAnswers(
                SessionRecord(s.id, SessionKind.from(s.kind), parseInstant(s.startTime)!!, parseInstant(s.endTime)!!, s.durationSeconds, parseInstant(s.createdAt)!!),
                answers[s.id].orEmpty(),
            )
        }
        val zone = ZoneId.of(System.getenv("BOWELS_ZONE") ?: "Africa/Addis_Ababa")
        val visits = Rhythm.visits(all)
        println("sessions ${all.size}, visits after folding duplicates ${visits.size}")

        // Look at the prediction right after each visit and every 3 hours until the next one.
        val errors = mutableListOf<Long>()
        var inWindow = 0
        val first = visits.first().session.startTime
        for (i in 1 until visits.size) {
            if (Duration.between(first, visits[i - 1].session.startTime).toDays() < 10) continue
            val actual = visits[i].session.startTime
            var now = visits[i - 1].session.endTime
            while (now < actual) {
                val p = Rhythm.predict(visits.subList(0, i), now.atZone(zone))!!
                errors += Duration.between(p.at.toInstant(), actual).abs().toMinutes()
                if (!actual.isBefore(p.from.toInstant()) && !actual.isAfter(p.to.toInstant())) inWindow++
                now = now.plusSeconds(3 * 3600)
            }
        }
        val within = { m: Int -> errors.count { it <= m } * 100 / errors.size }
        println("predictions ${errors.size}: within 30 min ${within(30)}%, within 1 h ${within(60)}%, within 2 h ${within(120)}%, in the window ${inWindow * 100 / errors.size}%, median ${errors.sorted()[errors.size / 2]} min")

        val end = visits.last().session.endTime.plusSeconds(3600)
        val p = Rhythm.predict(visits, end.atZone(zone))!!
        println("as of ${end.atZone(zone)}: ${Analytics.windowLabel(p, end.atZone(zone))}, self-check ${p.hits}/${p.checked}")
        Analytics.insights(all.sortedByDescending { it.session.startTime }, archive.dailyHealth, end).forEach { println("insight ${it.severity} ${it.id}: ${it.title}. ${it.body}") }
        // What the insights would have said along the way, at the end of each month.
        visits.groupBy { it.session.startTime.atZone(zone).let { z -> z.year * 100 + z.monthValue } }.values.forEach { month ->
            val at = month.last().session.endTime.plusSeconds(3600)
            val sofar = all.filter { it.session.startTime <= at }.sortedByDescending { it.session.startTime }
            val health = archive.dailyHealth.filter { it.day <= at.atZone(zone).toLocalDate().toString() }.sortedByDescending { it.day }
            println("${at.atZone(zone).toLocalDate()}: " + Analytics.insights(sofar, health, at).joinToString(" | ") { it.title })
        }
        assertTrue("within 1 h ${within(60)}%", within(60) >= 55)
    }
}
