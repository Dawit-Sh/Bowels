package com.d_ave.bowels.dashboard

import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.data.StoredData
import com.d_ave.bowels.data.parseInstant
import com.d_ave.bowels.shared.VitalsKeys
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

/** One record on its way to the receiver: its array name, its uuid and its body. */
data class Outgoing(val type: String, val uuid: String, val json: JSONObject)

/**
 * What Bowels sends to a Life Dashboard receiver: every visit and every daily health check, in the
 * record arrays such a receiver stores by name (hibi-server keeps them as they are, keyed on uuid).
 *
 * The uuids come from the data itself (a visit's kind and start, a check's day), not from row ids,
 * so a reinstall, a backup import or the first send of a long history gives every record the same
 * uuid again and the receiver keeps one copy. Times are UTC; zone_offset is this phone's offset at
 * that moment, so a receiver can cut local days.
 */
object DashboardRecords {
    const val SOURCE = "bowels"
    const val SESSIONS = "bowels_sessions"
    const val DAILY_CHECKS = "bowels_daily_checks"

    fun sessionUuid(kind: SessionKind, start: Instant) = "bowels-${kind.key}-${start.toEpochMilli()}"
    fun dailyUuid(day: String) = "bowels-day-$day"

    /** Every record the app holds now. A visit saved twice at the same moment goes once (the first one saved). */
    fun all(data: StoredData, zone: ZoneId): List<Outgoing> {
        val sessions = data.sessions
            .sortedBy { it.id }
            .distinctBy { sessionUuid(it.session.kind, it.session.startTime) }
            .sortedBy { it.session.startTime }
            .map { Outgoing(SESSIONS, sessionUuid(it.session.kind, it.session.startTime), session(it, zone)) }
        val checks = data.dailyHealth
            .filter { DAY.matches(it.day) }
            .sortedBy { it.day }
            .map { Outgoing(DAILY_CHECKS, dailyUuid(it.day), dailyCheck(it, zone)) }
        return sessions + checks
    }

    fun session(s: SessionWithAnswers, zone: ZoneId): JSONObject {
        val r = s.session
        val a = s.answers
        val json = JSONObject()
        json.put("uuid", sessionUuid(r.kind, r.startTime))
        json.put("time", r.startTime.toString())
        json.put("start_time", r.startTime.toString())
        json.put("end_time", r.endTime.toString())
        json.put("duration_seconds", r.durationSeconds)
        json.put("kind", r.kind.key)
        if (r.kind == SessionKind.Bowel) {
            a["stool_type"]?.toIntOrNull()?.let { json.put("stool_type", it) }
            a["pain"]?.let { json.put("pain", it) }
            a["blood"]?.let { json.put("blood", it == "true") }
            a["urgency"]?.toIntOrNull()?.let { json.put("urgency", it) }
            a["completion"]?.let { json.put("completion", it) }
            // Older visits only: Bowels no longer asks for urgency, completion or tags.
            a["adaptive_tags"]?.let { tags -> json.put("tags", JSONArray(tags.split(',').map { it.trim() }.filter { it.isNotEmpty() })) }
        } else {
            a["urine_urgency"]?.let { json.put("urine_urgency", it) }
            a["urine_color"]?.let { json.put("urine_color", it) }
        }
        val v = s.vitals
        if (v.hasHeartRate) {
            json.put("heart_rate_avg", v.heartRateAvg)
            v.heartRateMin?.let { json.put("heart_rate_min", it) }
            v.heartRateMax?.let { json.put("heart_rate_max", it) }
            v.heartRateSource?.let { json.put("heart_rate_source", it) }
        }
        if (v.hasBloodPressure) {
            json.put("bp_systolic", v.systolic)
            json.put("bp_diastolic", v.diastolic)
            v.bloodPressureTime?.let { json.put("bp_time", it.toString()) }
            v.bloodPressureSource?.let { json.put("bp_source", it) }
        }
        // Anything else a visit carries (a later version's question), so nothing recorded stays behind.
        val other = a.filterKeys { it !in KNOWN_ANSWERS }
        if (other.isNotEmpty()) json.put("other", JSONObject(other.toSortedMap() as Map<*, *>))
        json.put("from", if (a[VitalsKeys.WATCH_ID] != null) "watch" else "phone")
        json.put("logged_at", r.createdAt.toString())
        json.put("zone_offset", offset(r.startTime, zone))
        return json
    }

    fun dailyCheck(h: DailyHealthRecord, zone: ZoneId): JSONObject {
        val changed = parseInstant(h.updatedAt) ?: parseInstant(h.createdAt) ?: noonOf(h.day, zone)
        val json = JSONObject()
        json.put("uuid", dailyUuid(h.day))
        json.put("time", changed.toString())
        json.put("day", h.day)
        json.put("water", h.water.lowercase())
        json.put("fiber", h.fiber.lowercase())
        json.put("meals", h.meals.lowercase())
        json.put("stress", h.stress.lowercase())
        json.put("sleep", h.sleep.lowercase())
        json.put("exercise", h.exercise.lowercase())
        json.put("caffeine", h.caffeine.lowercase())
        json.put("alcohol", h.alcohol.lowercase())
        json.put("medication", h.medication.lowercase())
        json.put("mood", h.mood.lowercase())
        (parseInstant(h.createdAt))?.let { json.put("logged_at", it.toString()) }
        json.put("zone_offset", offset(changed, zone))
        return json
    }

    /** As Health Connect writes it: "+03:00", or "Z" for UTC. */
    fun offset(at: Instant, zone: ZoneId): String = zone.rules.getOffset(at).id

    private fun noonOf(day: String, zone: ZoneId): Instant = java.time.LocalDate.parse(day).atTime(12, 0).atZone(zone).toInstant()

    private val DAY = Regex("""\d{4}-\d{2}-\d{2}""")

    private val KNOWN_ANSWERS = setOf(
        "stool_type", "pain", "blood", "urgency", "completion", "adaptive_tags", "urine_urgency", "urine_color",
        VitalsKeys.HR_AVG, VitalsKeys.HR_MIN, VitalsKeys.HR_MAX, VitalsKeys.HR_SOURCE,
        VitalsKeys.BP_SYSTOLIC, VitalsKeys.BP_DIASTOLIC, VitalsKeys.BP_TIME, VitalsKeys.BP_SOURCE,
        // Bookkeeping, not something recorded about the visit.
        VitalsKeys.WATCH_ID, VitalsKeys.VITALS_CHECKED,
    )
}
