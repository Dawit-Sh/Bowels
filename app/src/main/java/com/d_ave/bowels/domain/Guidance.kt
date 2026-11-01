package com.d_ave.bowels.domain

import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import kotlin.math.roundToInt

/** One piece of advice written from your own logs: what was noticed, what to try, and the research. */
data class Advice(
    val id: String,
    val title: String,
    val noticed: String,
    val tryThis: List<String>,
    val papers: List<Paper>,
    /** Words for "find more studies" on PubMed. */
    val topic: String,
    val urgent: Boolean = false,
)

object Guidance {
    /** Advice for you, most important first. Always at least one card. */
    fun forYou(all: List<SessionWithAnswers>, dailyHealth: List<DailyHealthRecord>, patterns: Patterns?): List<Advice> {
        val visits = Rhythm.distinct(all).filter { it.session.kind == SessionKind.Bowel }
        if (visits.isEmpty()) {
            return listOf(
                Advice(
                    "start", "Start with a few visits",
                    "There's nothing logged yet, so there's nothing to read. A week of visits and health checks is enough for the first patterns.",
                    listOf("Log each visit with its stool type, even with Quick Log", "Fill in the daily health check when you finish a session"),
                    listOf(Research.bristolReliability, Research.normalRange), "bowel habits normal range",
                ),
            )
        }
        val out = mutableListOf<Advice>()
        val recent7 = visits.take(7)
        val typed = visits.mapNotNull { it.stoolType }
        val recent = typed.take(14)
        val before = typed.drop(14).take(40)
        val health = dailyHealth.take(7).map { it.toInput() }
        fun count(field: String, vararg levels: String) = health.count { it[field].lowercase() in levels }
        fun pct(n: Int, d: Int) = if (d == 0) 0 else (100.0 * n / d).roundToInt()

        // Red flags first, in plain words.
        val blood = recent7.count { it.answers["blood"] == "true" }
        if (blood > 0) {
            out += Advice(
                "blood", "Blood showed up",
                "You logged blood on $blood of your last ${recent7.size} visits.",
                listOf(
                    "See a doctor, especially if it happens again or the blood is dark",
                    "Most causes (like hemorrhoids or a small tear) are minor and easy to treat, but it should be checked",
                ),
                emptyList(), "rectal bleeding causes evaluation", urgent = true,
            )
        }
        val pain = visits.take(5).count { it.answers["pain"] in setOf("moderate", "severe") }
        if (pain >= 2) {
            out += Advice(
                "pain", "Pain keeps coming back",
                "$pain of your last 5 visits had moderate or severe pain.",
                listOf("Talk to a doctor if it carries on or gets worse", "Note what came before it (food, stress, straining) to show them"),
                emptyList(), "abdominal pain defecation causes", urgent = true,
            )
        }

        val loose = recent.count { it >= 6 }
        if (recent.size >= 5 && loose * 100 >= recent.size * 30) {
            val was = if (before.size >= 10) " (${pct(before.count { it >= 6 }, before.size)}% before)" else ""
            val tips = mutableListOf(
                "Soluble fiber thickens stools: oats, bananas, rice, or a spoon of psyllium",
                "Sip fluids through the day, more if it's watery",
                "Look at what you ate the day before loose days; triggers often repeat",
            )
            val papers = mutableListOf(Research.fiberIbs, Research.bristolTransit)
            if (count("caffeine", "medium", "high") >= 2) { tips += "Try a few days with less coffee: it speeds up the colon in many people"; papers += Research.coffee }
            if (count("alcohol", "light", "moderate", "heavy") >= 2) { tips += "Alcohol often loosens the next day's stools; see if the dry days are firmer"; papers += Research.alcohol }
            out += Advice(
                "loose", "Looser than ideal",
                "$loose of your last ${recent.size} visits were Type 6-7$was. That's food moving through faster than ideal.",
                tips, papers, "soluble fiber diarrhea",
            )
        }

        val hard = recent.count { it <= 2 }
        val strained = recent7.count { "straining" in it.answers["adaptive_tags"].orEmpty() }
        if ((recent.size >= 5 && hard * 100 >= recent.size * 25) || strained >= 3) {
            val noticed = buildList {
                if (hard > 0) add("$hard of your last ${recent.size} visits were Type 1-2")
                if (strained >= 3) add("you strained on $strained of your last ${recent7.size}")
            }.joinToString(", and ").replaceFirstChar { it.uppercase() } + "."
            out += Advice(
                "hard", "Harder going",
                noticed,
                listOf(
                    "Add fiber slowly (about 5 g more a week) toward 25-30 g a day: whole grains, beans, vegetables",
                    "Drink more water with it; fiber works better with fluids",
                    "Two green kiwifruit or a handful of prunes a day help many people",
                    "A small footstool under your feet straightens the way out",
                ),
                listOf(Research.fiberConstipation, Research.waterFiber, Research.kiwi, Research.prunes, Research.footstool),
                "constipation fiber water treatment",
            )
        }

        val minutes = visits.take(7).map { it.session.durationSeconds / 60.0 }.sorted()
        val usualMinutes = minutes.getOrNull(minutes.size / 2)
        if (minutes.size >= 3 && (usualMinutes ?: 0.0) >= 10) {
            out += Advice(
                "long", "Long sittings",
                "Your recent sessions take about ${usualMinutes!!.roundToInt()} minutes. Past 10 minutes, the pressure on the veins down there adds up.",
                listOf(
                    "Leave the phone outside; it's the main reason sittings run long",
                    "If nothing happens in about 5 minutes, get up and come back when you feel the urge",
                    "Put your feet on a footstool so it goes faster with less straining",
                ),
                listOf(Research.phoneOnToilet, Research.footstool, Research.posture), "toilet time hemorrhoids",
            )
        }

        val timing = patterns?.regularityParts?.firstOrNull { it.first == "Timing" }?.second
        if (timing != null && timing < 50) {
            out += Advice(
                "timing", "Your timing moves around",
                "Your first visit of the day lands at quite different times (timing score $timing of 100).",
                listOf(
                    "Give it the same slot each day, 15-30 minutes after breakfast, when the gut is most active",
                    "A warm drink in the morning helps it along",
                    "Don't hold it when the urge comes; waiting trains it to fade",
                ),
                listOf(Research.normalRange), "bowel habit routine morning",
            )
        }

        val personal = Analytics.insights(all, dailyHealth).filter { it.id.startsWith("effect-") }
        fun personalNote(field: String) = personal.firstOrNull { it.id.startsWith("effect-$field") }?.let { " In your own logs: ${it.body}" }.orEmpty()

        val stress = count("stress", "high")
        if (stress >= 2 || personal.any { it.id.startsWith("effect-stress") }) {
            out += Advice(
                "stress", "Stress and your gut",
                "You logged high stress on $stress of the last ${health.size} days.${personalNote("stress")}",
                listOf("Five minutes of slow breathing before a meal or the bathroom", "A short walk after eating", "Keep meals regular on busy days"),
                listOf(Research.stress), "stress gut brain axis bowel",
            )
        }
        val poorSleep = count("sleep", "poor")
        if (poorSleep >= 3 || personal.any { it.id.startsWith("effect-sleep") }) {
            out += Advice(
                "sleep", "Sleep is showing up",
                "You slept poorly on $poorSleep of the last ${health.size} nights.${personalNote("sleep")}",
                listOf("A fixed wake-up time, even on weekends", "No big meals in the last 2-3 hours before bed"),
                listOf(Research.sleep), "sleep gastrointestinal symptoms",
            )
        }
        val lowWater = count("water", "low")
        if (lowWater >= 3) {
            out += Advice(
                "water", "Water is low",
                "Water was low on $lowWater of the last ${health.size} days.${personalNote("water")}",
                listOf("A glass with each meal and one mid-morning and mid-afternoon", "More on hot days and when you add fiber"),
                listOf(Research.waterFiber), "water intake constipation",
            )
        }
        val noExercise = count("exercise", "none")
        if (health.size >= 5 && noExercise >= 5) {
            out += Advice(
                "move", "Little movement",
                "No exercise on $noExercise of the last ${health.size} days.",
                listOf("A 20-30 minute walk a day is enough to help", "Walk after your biggest meal"),
                listOf(Research.exercise), "exercise constipation",
            )
        }

        // When every check-in looks the same, the habit links can't show anything.
        val checks = dailyHealth.take(60).map { it.toInput() }
        if (checks.size >= 14) {
            val same = checks.groupingBy { it }.eachCount().values.max()
            if (same * 100 >= checks.size * 80) {
                out += Advice(
                    "checkins", "Make your check-ins count",
                    "$same of your last ${checks.size} health checks had exactly the same answers. That's fine if your days really are alike, but if not, the links between habits and stools can't show up.",
                    listOf("Change only what was different that day: stress, sleep, water, a drink, a heavy meal", "The check is pre-filled, so a different day is one or two taps"),
                    listOf(Research.bristolReliability), "food diary symptom tracking",
                )
            }
        }

        if (out.none { !it.urgent && it.id !in setOf("checkins") }) {
            out += Advice(
                "good", "You're in a good rhythm",
                buildString {
                    append("Nothing in your recent logs calls for a change")
                    patterns?.regularity?.let { append(", and your regularity score is $it of 100") }
                    append(".")
                },
                listOf("Keep the same routine; consistency is most of it", "Keep logging so changes show up early"),
                listOf(Research.normalRange, Research.bristolTransit), "healthy bowel habits",
            )
        }
        return out
    }

    /** Research behind each general practice on the Gut Practices screen. */
    val practicePapers: Map<String, List<Paper>> = mapOf(
        "hydration" to listOf(Research.waterFiber),
        "fiber" to listOf(Research.fiberConstipation, Research.fiberIbs),
        "routine" to listOf(Research.normalRange),
        "sleep" to listOf(Research.sleep),
        "stress" to listOf(Research.stress),
        "movement" to listOf(Research.exercise),
        "caffeine" to listOf(Research.coffee),
        "alcohol" to listOf(Research.alcohol),
        "bristol-scale" to listOf(Research.bristolTransit, Research.bristolReliability),
        "posture" to listOf(Research.posture, Research.footstool),
        "phone" to listOf(Research.phoneOnToilet),
        "fruit" to listOf(Research.kiwi, Research.prunes),
    )
}
