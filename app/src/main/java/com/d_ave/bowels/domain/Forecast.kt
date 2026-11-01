package com.d_ave.bowels.domain

import com.d_ave.bowels.data.DailyHealthInput
import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.exp
import kotlin.math.pow

enum class Outlook(val label: String) { Steady("Steady"), Looser("Looser"), Harder("Harder") }

/** One thing pushing tomorrow's forecast, and which way. */
data class Driver(val text: String, val towards: Outlook, val strength: Double)

data class Forecast(
    val day: LocalDate,
    val chance: Map<Outlook, Double>,
    val outlook: Outlook,
    val drivers: List<Driver>,
    /** Walk-forward self-check on your own past days: trained only on the days before each one. */
    val hits: Int,
    val checked: Int,
    /** How many of the same days "it'll be like usual" would have got right. */
    val baselineHits: Int,
    val daysLearned: Int,
    val usedTodaysCheck: Boolean,
)

/**
 * A small model that learns, on the phone, how your days lead into the next one: your daily
 * check (water, fiber, stress, sleep, exercise, caffeine, alcohol, meals) and today's stools
 * against tomorrow's (steady, looser or harder). It's a multinomial logistic regression with
 * 13 inputs, a few hundred numbers in all, so it trains in milliseconds and nothing leaves the phone.
 *
 * It starts from what research says (see [Research]): low water and fiber lean harder, stress,
 * caffeine, alcohol and poor sleep lean looser. Each day you log pulls it toward what's true
 * for you, and with enough days your own pattern outweighs the starting point.
 */
object ForecastModel {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private class Feature(val text: String, val prior: DoubleArray /* [looser, harder] */, val on: (DailyHealthInput?, DayStools) -> Boolean)

    private data class DayStools(val visits: Int, val meanType: Double?)

    private fun DailyHealthInput?.v(field: String) = this?.get(field)?.lowercase().orEmpty()

    private val features = listOf(
        Feature("Low water today", doubleArrayOf(0.0, 0.5)) { h, _ -> h.v("water") == "low" },
        Feature("Plenty of water today", doubleArrayOf(0.0, -0.3)) { h, _ -> h.v("water") == "great" },
        Feature("Low fiber today", doubleArrayOf(0.0, 0.5)) { h, _ -> h.v("fiber") == "low" },
        Feature("High fiber today", doubleArrayOf(0.1, -0.4)) { h, _ -> h.v("fiber") == "high" },
        Feature("High stress today", doubleArrayOf(0.6, 0.1)) { h, _ -> h.v("stress") == "high" },
        Feature("Poor sleep last night", doubleArrayOf(0.3, 0.2)) { h, _ -> h.v("sleep") == "poor" },
        Feature("Good sleep last night", doubleArrayOf(-0.2, -0.1)) { h, _ -> h.v("sleep") == "great" },
        Feature("No exercise today", doubleArrayOf(0.0, 0.25)) { h, _ -> h.v("exercise") == "none" },
        Feature("Caffeine today", doubleArrayOf(0.35, -0.1)) { h, _ -> h.v("caffeine") in setOf("medium", "high") },
        Feature("Alcohol today", doubleArrayOf(0.45, 0.0)) { h, _ -> h.v("alcohol") in setOf("light", "moderate", "heavy") },
        Feature("A heavy meal today", doubleArrayOf(0.3, 0.1)) { h, _ -> h.v("meals") == "heavy" },
        Feature("Today's stools were loose", doubleArrayOf(1.0, -0.5)) { _, s -> (s.meanType ?: 4.0) >= 5.5 },
        Feature("Today's stools were hard", doubleArrayOf(-0.5, 1.0)) { _, s -> (s.meanType ?: 4.0) <= 2.5 },
    )
    private val priorBias = doubleArrayOf(-1.0, -1.5)
    private const val PRIOR_STRENGTH = 2.0
    private const val ITERATIONS = 200
    private const val SELF_CHECKS = 20
    private const val RATE = 0.3

    private fun outcome(meanType: Double) = when {
        meanType >= 5.5 -> Outlook.Looser
        meanType <= 2.5 -> Outlook.Harder
        else -> Outlook.Steady
    }

    private class Example(val x: DoubleArray, val y: Outlook, val weight: Double, val day: LocalDate)

    private class Model(val w: Array<DoubleArray>) {
        /** Chances for steady, looser, harder. */
        fun predict(x: DoubleArray): DoubleArray {
            val z = DoubleArray(3)
            for (c in 1..2) z[c] = w[c - 1].indices.sumOf { w[c - 1][it] * x[it] }
            val m = z.max()
            val e = z.map { exp(it - m) }
            val s = e.sum()
            return DoubleArray(3) { e[it] / s }
        }
    }

    private fun priorWeights(): Array<DoubleArray> = Array(2) { c -> DoubleArray(features.size + 1) { k -> if (k == 0) priorBias[c] else features[k - 1].prior[c] } }

    /** Maximum a posteriori fit: the data's likelihood, pulled toward the research prior. */
    private fun train(examples: List<Example>): Model {
        val prior = priorWeights()
        val w = Array(2) { prior[it].copyOf() }
        if (examples.isEmpty()) return Model(w)
        val total = examples.sumOf { it.weight }
        repeat(ITERATIONS) {
            val grad = Array(2) { DoubleArray(w[0].size) }
            val model = Model(w)
            for (ex in examples) {
                val p = model.predict(ex.x)
                for (c in 1..2) {
                    val err = p[c] - if (ex.y.ordinal == c) 1.0 else 0.0
                    for (k in ex.x.indices) grad[c - 1][k] += ex.weight * err * ex.x[k]
                }
            }
            for (c in 0..1) for (k in w[c].indices) {
                val g = grad[c][k] / total + PRIOR_STRENGTH / total * (w[c][k] - prior[c][k])
                w[c][k] -= RATE * g
            }
        }
        return Model(w)
    }

    private fun vector(health: DailyHealthInput?, stools: DayStools) =
        DoubleArray(features.size + 1) { k -> if (k == 0) 1.0 else if (features[k - 1].on(health, stools)) 1.0 else 0.0 }

    private fun stoolsByDay(sessions: List<SessionWithAnswers>): Map<LocalDate, DayStools> =
        Rhythm.distinct(sessions).filter { it.session.kind == SessionKind.Bowel }
            .groupBy { it.session.startTime.atZone(zone).toLocalDate() }
            .mapValues { (_, v) -> DayStools(v.size, v.mapNotNull { it.stoolType }.takeIf { it.isNotEmpty() }?.average()) }

    /** Every finished day with stools, described by the day before it. */
    private fun examples(stools: Map<LocalDate, DayStools>, health: Map<String, DailyHealthInput>, today: LocalDate): List<Example> =
        stools.entries.filter { (day, s) -> s.meanType != null && day < today }
            .map { (day, s) ->
                val before = day.minusDays(1)
                val x = vector(health[before.toString()], stools[before] ?: DayStools(0, null))
                val age = (today.toEpochDay() - day.toEpochDay()).toDouble()
                Example(x, outcome(s.meanType!!), 0.5.pow(age / 90), day)
            }
            .sortedBy { it.day }

    /** Tomorrow's outlook, or null until there are a couple of weeks of visits. */
    fun forecast(sessions: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>, now: ZonedDateTime = ZonedDateTime.now(zone)): Forecast? {
        val today = now.toLocalDate()
        val stools = stoolsByDay(sessions).filterKeys { it >= today.minusDays(365) }
        val health = dailyHealth.associate { it.day to it.toInput() }
        val all = examples(stools, health, today)
        if (stools.size < 10) return null

        // Self-check: for each of the last 20 learned days, train on the days before it only.
        var hits = 0
        var checked = 0
        var baselineHits = 0
        for (i in all.indices.reversed().take(SELF_CHECKS)) {
            val past = all.subList(0, i)
            if (past.size < 7) continue
            val model = train(past)
            val p = model.predict(all[i].x)
            val guess = Outlook.entries[p.indices.maxBy { p[it] }]
            val usual = past.groupingBy { it.y }.eachCount().maxBy { it.value }.key
            checked++
            if (guess == all[i].y) hits++
            if (usual == all[i].y) baselineHits++
        }

        val model = train(all)
        val todayHealth = health[today.toString()]
        val todayStools = stools[today] ?: DayStools(0, null)
        val x = vector(todayHealth, todayStools)
        val p = model.predict(x)
        val chance = mapOf(Outlook.Steady to p[0], Outlook.Looser to p[1], Outlook.Harder to p[2])
        val outlook = Outlook.entries[p.indices.maxBy { p[it] }]

        // What pushes it: each active input's pull toward looser or harder (steady is the reference).
        val drivers = features.indices.filter { x[it + 1] > 0 }.flatMap { k ->
            listOf(Outlook.Looser, Outlook.Harder).map { o -> Driver(features[k].text, o, model.w[o.ordinal - 1][k + 1]) }
        }.filter { it.strength > 0.15 }.sortedByDescending { it.strength }
            .distinctBy { it.text }.take(4)

        return Forecast(today.plusDays(1), chance, outlook, drivers, hits, checked, baselineHits, all.size, todayHealth != null)
    }
}
