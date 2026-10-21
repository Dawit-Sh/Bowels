package com.d_ave.bowels.shared

import org.json.JSONObject

/**
 * The contract between the phone and the watch. The watch writes one Data Layer item per
 * session under [SESSION_PATH_PREFIX]; Google Play services keeps it on the watch and delivers
 * it whenever the phone is back in range. The phone saves it, then deletes the item, which is
 * how the watch knows it has synced.
 */
object WatchProtocol {
    const val SESSION_PATH_PREFIX = "/bowels/session/"
    const val PAYLOAD_KEY = "payload"

    /** Declared by each app in res/values/wear.xml so the other side can find it. */
    const val PHONE_CAPABILITY = "bowels_phone_app"
    const val WATCH_CAPABILITY = "bowels_watch_app"

    const val PACKAGE_NAME = "com.d_ave.bowels"
    const val PLAY_STORE_URI = "market://details?id=$PACKAGE_NAME"

    fun sessionPath(id: String) = SESSION_PATH_PREFIX + id
}

/** Answer keys for vitals, stored in session_answers next to stool type, pain and so on. */
object VitalsKeys {
    const val HR_AVG = "hr_avg"
    const val HR_MIN = "hr_min"
    const val HR_MAX = "hr_max"
    const val BP_SYSTOLIC = "bp_systolic"
    const val BP_DIASTOLIC = "bp_diastolic"
    const val BP_TIME = "bp_time"
    const val HR_SOURCE = "hr_source"
    const val BP_SOURCE = "bp_source"
    const val WATCH_ID = "watch_id"
    const val VITALS_CHECKED = "vitals_checked"

    const val SOURCE_WATCH = "watch"
    const val SOURCE_HEALTH_CONNECT = "health_connect"
    const val SOURCE_MANUAL = "manual"
}

/** A session logged on the watch, as it travels to the phone. */
data class WatchSessionPayload(
    val id: String,
    val kind: String,
    val startTime: String,
    val endTime: String,
    val durationSeconds: Long,
    val answers: Map<String, String>,
    val heartRateAvg: Int? = null,
    val heartRateMin: Int? = null,
    val heartRateMax: Int? = null,
) {
    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("kind", kind)
        put("startTime", startTime)
        put("endTime", endTime)
        put("durationSeconds", durationSeconds)
        put("answers", JSONObject(answers))
        heartRateAvg?.let { put("hrAvg", it) }
        heartRateMin?.let { put("hrMin", it) }
        heartRateMax?.let { put("hrMax", it) }
    }.toString()

    companion object {
        fun fromJson(raw: String): WatchSessionPayload {
            val json = JSONObject(raw)
            val answersJson = json.optJSONObject("answers") ?: JSONObject()
            val answers = answersJson.keys().asSequence().associateWith { answersJson.getString(it) }
            return WatchSessionPayload(
                id = json.getString("id"),
                kind = json.optString("kind", "bowel"),
                startTime = json.getString("startTime"),
                endTime = json.getString("endTime"),
                durationSeconds = json.optLong("durationSeconds", 0),
                answers = answers,
                heartRateAvg = json.optIntOrNull("hrAvg"),
                heartRateMin = json.optIntOrNull("hrMin"),
                heartRateMax = json.optIntOrNull("hrMax"),
            )
        }

        private fun JSONObject.optIntOrNull(key: String) = if (has(key)) getInt(key) else null
    }
}
