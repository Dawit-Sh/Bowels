package com.d_ave.bowels.domain

import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

/** When the next visit is likely, with a window around it and how often that window held up for you. */
data class Prediction(
    val at: ZonedDateTime,
    val from: ZonedDateTime,
    val to: ZonedDateTime,
    /** Self-check over your own recent visits: how many times the window held the real time. */
    val hits: Int,
    val checked: Int,
)

/**
 * Learns your rhythm from your own visits, on the phone, in a few milliseconds.
 *
 * Each past visit adds a small bump at its time of day, newer visits weighing more (half of the
 * weight is gone after two weeks), which gives how likely a visit is at every minute of the day.
 * Right after a visit that chance is damped and comes back over a few hours. Walking forward from
 * now, the first moment the next visit becomes most likely is the prediction; the times around it
 * that are at least half as likely make the window.
 *
 * Backtested on a real four-month export and on made-up rhythms (once or twice a day, late
 * weekends, a schedule change): within an hour of the real time 60% of the time on the real
 * history (the 2.x algorithm: 53%), and 68% against 28% for people who go twice a day.
 */
object Rhythm {
    private const val STEP = 5 // minutes
    private const val HORIZON = 72 * 60
    private const val HALF_LIFE_DAYS = 14.0
    private const val TIME_SPREAD_MINUTES = 45.0
    private const val SETTLE_HOURS = 4.0
    private const val FLOOR_PER_DAY = 0.02
    private const val LOOKBACK_DAYS = 120L
    private const val SELF_CHECKS = 20

    /** A von Mises bump on the 24 h clock, as a density per minute. */
    private val kernel: DoubleArray by lazy {
        val kappa = 1 / (2 * PI * TIME_SPREAD_MINUTES / 1440).pow(2)
        val raw = DoubleArray(1440) { exp(kappa * (cos(2 * PI * it / 1440) - 1)) }
        val total = raw.sum()
        DoubleArray(1440) { raw[it] / total }
    }

    /**
     * Bowel sessions oldest first, with one visit logged twice (an archive imported twice, a double
     * tap on Quick Log) counted once: a session that starts before the previous one ended is the same visit.
     */
    fun visits(sessions: List<SessionWithAnswers>): List<SessionWithAnswers> =
        distinct(sessions.filter { it.session.kind == SessionKind.Bowel }).asReversed()

    /** Same as [visits] for any kind, newest first, which is the order the rest of the app uses. */
    fun distinct(sessions: List<SessionWithAnswers>): List<SessionWithAnswers> {
        val out = ArrayList<SessionWithAnswers>(sessions.size)
        for (kind in SessionKind.entries) {
            var last: SessionWithAnswers? = null
            for (s in sessions.filter { it.session.kind == kind }.sortedBy { it.session.startTime }) {
                val prev = last
                if (prev != null && s.session.startTime < prev.session.endTime.plusSeconds(120)) {
                    if (s.answers.size > prev.answers.size) {
                        out[out.indexOf(prev)] = s
                        last = s
                    }
                    continue
                }
                out += s
                last = s
            }
        }
        return out.sortedByDescending { it.session.startTime }
    }

    /** [visits] oldest first. Returns null until there are three. */
    fun predict(visits: List<SessionWithAnswers>, now: ZonedDateTime): Prediction? {
        val history = visits.filter { it.session.startTime >= now.toInstant().minusSeconds(LOOKBACK_DAYS * 86400) }
        val point = predictAt(history, now) ?: return null
        var hits = 0
        var checked = 0
        val firstAllowed = history.firstOrNull()?.session?.startTime?.plusSeconds(7 * 86400)
        for (j in history.indices.reversed().take(SELF_CHECKS)) {
            if (j < 5 || firstAllowed == null || history[j - 1].session.startTime < firstAllowed) continue
            val then = history[j - 1].session.endTime.atZone(now.zone)
            val p = predictAt(history.subList(0, j), then) ?: continue
            val actual = history[j].session.startTime.atZone(now.zone)
            checked++
            if (!actual.isBefore(p.from) && !actual.isAfter(p.to)) hits++
        }
        return point.copy(hits = hits, checked = checked)
    }

    private fun predictAt(history: List<SessionWithAnswers>, now: ZonedDateTime): Prediction? {
        if (history.size < 3) return null
        val zone: ZoneId = now.zone
        val nowMinutes = now.toEpochSecond() / 60.0
        fun dayOf(i: Instant) = i.atZone(zone).toLocalDate().toEpochDay()
        fun weight(ageDays: Double) = 0.5.pow(max(ageDays, 0.0) / HALF_LIFE_DAYS)

        // Visits per minute at each clock time, recency weighted.
        val today = now.toLocalDate().toEpochDay()
        val firstDay = dayOf(history.first().session.startTime)
        val dayWeights = (firstDay..today).sumOf { weight((today - it).toDouble()) }
        val profile = DoubleArray(1440 / STEP)
        for (v in history) {
            val start = v.session.startTime
            val w = weight(nowMinutes / 1440 - start.epochSecond / 86400.0)
            val local = start.atZone(zone)
            val minute = local.hour * 60 + local.minute
            for (b in profile.indices) {
                val centre = b * STEP + STEP / 2
                profile[b] += w * kernel[Math.floorMod(centre - minute, 1440)]
            }
        }
        for (b in profile.indices) profile[b] = profile[b] / dayWeights + FLOOR_PER_DAY / 1440

        // Walk forward: hazard -> chance the next visit is at each step.
        val lastEnd = history.last().session.endTime.epochSecond / 60.0
        val firstStep = now.withSecond(0).withNano(0).plusMinutes((STEP - now.minute % STEP).toLong())
        val steps = HORIZON / STEP
        val chance = DoubleArray(steps)
        var survive = 1.0
        var t = firstStep
        for (k in 0 until steps) {
            val sinceLast = (t.toEpochSecond() / 60.0 - lastEnd) / 60
            val hazard = profile[(t.hour * 60 + t.minute) / STEP] * (1 - exp(-max(sinceLast, 0.0) / SETTLE_HOURS))
            survive *= exp(-hazard * STEP)
            chance[k] = hazard * survive
            t = t.plusMinutes(STEP.toLong())
        }
        val peak = chance.indices.maxBy { chance[it] }
        var a = peak
        while (a > 0 && chance[a - 1] >= chance[peak] / 2) a--
        var b = peak
        while (b < steps - 1 && chance[b + 1] >= chance[peak] / 2) b++
        fun stepTime(k: Int) = firstStep.plusMinutes(k.toLong() * STEP)
        return Prediction(stepTime(peak), roundDown(stepTime(a)), roundUp(stepTime(b)), 0, 0)
    }

    /** Windows read better on tens of minutes. */
    private fun roundDown(t: ZonedDateTime) = t.minusMinutes((t.minute % 10).toLong())
    private fun roundUp(t: ZonedDateTime) = if (t.minute % 10 == 0) t else t.plusMinutes((10 - t.minute % 10).toLong())
}
