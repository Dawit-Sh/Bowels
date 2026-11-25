package com.d_ave.bowels.domain

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
    fun forYou(all: List<SessionWithAnswers>, patterns: Patterns?): List<Advice> {
        val visits = Rhythm.distinct(all).filter { it.session.kind == SessionKind.Bowel }
        if (visits.isEmpty()) {
            return listOf(
                Advice(
                    "start", "Start with a few visits",
                    "There's nothing logged yet, so there's nothing to read. A week of visits is enough for the first patterns.",
                    listOf("Log each visit with its stool type, even with Quick Log", "Time a session when you can, so its length counts too"),
                    listOf(Research.bristolReliability, Research.normalRange), "bowel habits normal range",
                ),
            )
        }
        val out = mutableListOf<Advice>()
        val recent7 = visits.take(7)
        val typed = visits.mapNotNull { it.stoolType }
        val recent = typed.take(14)
        val before = typed.drop(14).take(40)
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
            out += Advice(
                "loose", "Looser than ideal",
                "$loose of your last ${recent.size} visits were Type 6-7$was. That's food moving through faster than ideal.",
                listOf(
                    "Soluble fiber thickens stools: oats, bananas, rice, or a spoon of psyllium",
                    "Sip fluids through the day, more if it's watery",
                    "Look at what you ate the day before loose days; triggers often repeat",
                    "Coffee and alcohol speed things up in many people; see if days without them are firmer",
                ),
                listOf(Research.fiberIbs, Research.bristolTransit, Research.coffee, Research.alcohol), "soluble fiber diarrhea",
            )
        }

        val hard = recent.count { it <= 2 }
        if (recent.size >= 5 && hard * 100 >= recent.size * 25) {
            out += Advice(
                "hard", "Harder going",
                "$hard of your last ${recent.size} visits were Type 1-2.",
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

        if (out.none { !it.urgent }) {
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
