package com.d_ave.bowels.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Reads both the current backup format and the legacy one (session_answers / question_key). */
object ArchiveNormalizer {
    data class Session(val id: Long, val kind: String, val startTime: String, val endTime: String, val durationSeconds: Long, val createdAt: String)
    data class Answer(val sessionId: Long, val key: String, val value: String)
    data class Setting(val key: String, val value: String, val updatedAt: String)
    data class Archive(val sessions: List<Session>, val answers: List<Answer>, val dailyHealth: List<DailyHealthRecord>, val settings: List<Setting>)

    private val legacyAnswerKeys = mapOf(
        "bowel_stool_type" to "stool_type",
        "bowel_pain" to "pain",
        "bowel_blood" to "blood",
        "bowel_urgency" to "urgency",
        "bowel_completion" to "completion",
        "bowel_adaptive_tags" to "adaptive_tags",
        "urine_urgency" to "urine_urgency",
        "urine_color" to "urine_color",
    )

    private val knownSettingKeys = setOf("themeMode", "accent", "reminderHour", "notificationsEnabled", "hasOnboarded")

    private fun now() = Instant.now().toIsoString()

    private fun JSONObject.text(vararg keys: String, fallback: String = ""): String {
        for (key in keys) if (has(key) && !isNull(key)) return get(key).toString()
        return fallback
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

    private fun isoOrNow(value: String) = value.ifEmpty { now() }

    private fun booleanString(value: String): String = when (value.trim().lowercase()) {
        "yes", "true", "1" -> "true"
        "no", "false", "0" -> "false"
        else -> value
    }

    fun normalize(raw: JSONObject): Archive {
        val current = raw.optJSONArray("answers")
        val sessionsJson = raw.optJSONArray("sessions") ?: throw IllegalArgumentException("Unsupported archive format.")
        return if (current != null) normalizeCurrent(raw, sessionsJson, current) else normalizeLegacy(raw, sessionsJson)
    }

    private fun normalizeCurrent(raw: JSONObject, sessionsJson: JSONArray, answersJson: JSONArray): Archive {
        val sessions = sessionsJson.objects().mapIndexed { index, s ->
            val start = isoOrNow(s.text("startTime"))
            Session(
                id = s.optLong("id", index + 1L),
                kind = if (s.text("kind") == "urine") "urine" else "bowel",
                startTime = start,
                endTime = s.text("endTime", fallback = start),
                durationSeconds = s.optLong("durationSeconds", 0),
                createdAt = s.text("createdAt", fallback = start),
            )
        }
        val answers = answersJson.objects().map { Answer(it.optLong("sessionId"), it.text("key"), it.text("value")) }
        val health = (raw.optJSONArray("dailyHealth") ?: JSONArray()).objects().mapIndexed { i, h -> healthRecord(h, i) }
        val settings = (raw.optJSONArray("settings") ?: JSONArray()).objects()
            .map { Setting(it.text("key"), it.text("value"), isoOrNow(it.text("updatedAt"))) }
            .toMutableList()
        if (settings.none { it.key == "hasRealData" }) settings += Setting("hasRealData", "true", now())
        if (settings.none { it.key == "hasOnboarded" }) settings += Setting("hasOnboarded", "true", now())
        return Archive(sessions, answers, health, settings)
    }

    private fun normalizeLegacy(raw: JSONObject, sessionsJson: JSONArray): Archive {
        val legacyAnswers = raw.optJSONArray("session_answers") ?: throw IllegalArgumentException("Unsupported archive format.")
        val sessions = sessionsJson.objects().mapIndexed { index, s ->
            val start = isoOrNow(s.text("start_time"))
            Session(
                id = s.optLong("id", index + 1L),
                kind = if (s.text("type") == "urine") "urine" else "bowel",
                startTime = start,
                endTime = s.text("end_time", fallback = start),
                durationSeconds = s.optLong("duration_seconds", 0),
                createdAt = s.text("created_at", "updated_at", fallback = start),
            )
        }
        val answers = legacyAnswers.objects().mapNotNull { a ->
            val key = legacyAnswerKeys[a.text("question_key")] ?: return@mapNotNull null
            val value = a.text("value")
            Answer(a.optLong("session_id"), key, if (key == "blood") booleanString(value) else value)
        }
        val health = (raw.optJSONArray("daily_health") ?: JSONArray()).objects().mapIndexed { i, h -> healthRecord(h, i) }

        val settings = mutableListOf<Setting>()
        var sawOnboarding = false
        (raw.optJSONArray("settings") ?: JSONArray()).objects().forEach { setting ->
            val key = setting.text("key")
            val value = setting.text("value")
            val updatedAt = isoOrNow(setting.text("updated_at"))
            if (key == "app_settings_v1") {
                runCatching { JSONObject(value.ifEmpty { "{}" }) }.getOrNull()?.let { parsed ->
                    parsed.text("mode").takeIf { it.isNotEmpty() }?.let { settings += Setting("themeMode", it, updatedAt) }
                    parsed.text("accent").takeIf { it.isNotEmpty() }?.let {
                        settings += Setting("accent", if (it.lowercase() == "orange") "coral" else it, updatedAt)
                    }
                    parsed.text("dailyReminderTime").takeIf { it.isNotEmpty() }?.let {
                        settings += Setting("reminderHour", (it.substringBefore(":").toIntOrNull() ?: 20).toString(), updatedAt)
                    }
                    if (parsed.has("notificationsEnabled")) settings += Setting("notificationsEnabled", parsed.optBoolean("notificationsEnabled").toString(), updatedAt)
                    if (parsed.has("hasOnboarded")) {
                        sawOnboarding = true
                        settings += Setting("hasOnboarded", parsed.optBoolean("hasOnboarded").toString(), updatedAt)
                    }
                }
            } else if (key in knownSettingKeys) {
                if (key == "hasOnboarded") sawOnboarding = true
                settings += Setting(key, value, updatedAt)
            }
        }
        if (!sawOnboarding) settings += Setting("hasOnboarded", "true", now())
        settings += Setting("hasRealData", "true", now())
        return Archive(sessions, answers, health, settings)
    }

    private fun healthRecord(h: JSONObject, index: Int): DailyHealthRecord {
        val created = isoOrNow(h.text("createdAt", "created_at"))
        return DailyHealthRecord(
            id = h.optLong("id", index + 1L),
            day = h.text("day", "date_key", fallback = created.take(10)),
            water = h.text("water"), fiber = h.text("fiber"), meals = h.text("meals"), stress = h.text("stress"),
            sleep = h.text("sleep"), exercise = h.text("exercise"),
            caffeine = h.text("caffeine", fallback = "None"), alcohol = h.text("alcohol", fallback = "None"),
            medication = h.text("medication", fallback = "None"), mood = h.text("mood", fallback = "Neutral"),
            createdAt = created, updatedAt = h.text("updatedAt", "updated_at", fallback = created),
        )
    }
}
