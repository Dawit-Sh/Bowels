package com.d_ave.bowels.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.shared.WatchSessionPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Everything the app has stored, read in one go. */
data class StoredData(
    val sessions: List<SessionWithAnswers> = emptyList(),
    val dailyHealth: List<DailyHealthRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val hasSettingsRows: Boolean = false,
)

data class ImportSummary(val importedSessions: Int, val skippedSessions: Int, val importedAnswers: Int, val importedHealthDays: Int)

class Repository(private val helper: BowelsDatabase) {
    private val writeLock = Mutex()
    private val _data = MutableStateFlow<StoredData?>(null)

    /** Null until the first load finishes. Every write reloads it, so screens just collect this. */
    val data: StateFlow<StoredData?> = _data.asStateFlow()

    private val db: SQLiteDatabase get() = helper.writableDatabase

    suspend fun reload(): StoredData = withContext(Dispatchers.IO) {
        val answers = readAnswerMap()
        val sessions = db.rawQuery(
            "SELECT id, kind, start_time, end_time, duration_seconds, created_at FROM sessions ORDER BY start_time DESC",
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val start = parseInstant(c.getString(2)) ?: continue
                    val record = SessionRecord(
                        id = c.getLong(0),
                        kind = SessionKind.from(c.getString(1)),
                        startTime = start,
                        endTime = parseInstant(c.getString(3)) ?: start,
                        durationSeconds = c.getLong(4),
                        createdAt = parseInstant(c.getString(5)) ?: start,
                    )
                    add(SessionWithAnswers(record, answers[record.id].orEmpty()))
                }
            }
        }
        val settingRows = readSettingRows()
        val stored = StoredData(
            sessions = sessions,
            dailyHealth = readDailyHealth(),
            settings = AppSettings.fromMap(settingRows),
            hasSettingsRows = settingRows.isNotEmpty(),
        )
        _data.value = stored
        stored
    }

    private fun readAnswerMap(): Map<Long, Map<String, String>> {
        val map = mutableMapOf<Long, MutableMap<String, String>>()
        db.rawQuery("SELECT session_id, answer_key, answer_value FROM session_answers ORDER BY id", null).use { c ->
            while (c.moveToNext()) map.getOrPut(c.getLong(0)) { mutableMapOf() }[c.getString(1)] = c.getString(2)
        }
        return map
    }

    private fun readDailyHealth(): List<DailyHealthRecord> =
        db.rawQuery(
            "SELECT id, day, water, fiber, meals, stress, sleep, exercise, caffeine, alcohol, medication, mood, created_at, updated_at FROM daily_health ORDER BY day DESC",
            null,
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        DailyHealthRecord(
                            id = c.getLong(0), day = c.str(1), water = c.str(2), fiber = c.str(3), meals = c.str(4),
                            stress = c.str(5), sleep = c.str(6), exercise = c.str(7),
                            caffeine = c.str(8, "None"), alcohol = c.str(9, "None"), medication = c.str(10, "None"),
                            mood = c.str(11, "Neutral"), createdAt = c.str(12), updatedAt = c.str(13),
                        ),
                    )
                }
            }
        }

    private fun readSettingRows(): Map<String, String> =
        db.rawQuery("SELECT setting_key, setting_value FROM settings", null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
        }

    private fun Cursor.str(index: Int, fallback: String = "") = if (isNull(index)) fallback else getString(index).ifEmpty { fallback }

    suspend fun saveSession(draft: SessionDraft): Long = write {
        val start = requireNotNull(draft.startTime) { "Session draft is incomplete." }
        val end = requireNotNull(draft.endTime) { "Session draft is incomplete." }
        insertSession(draft.kind.key, start, end, draft.durationSeconds, draft.answers())
    }

    /** Saves a session sent by the watch. Returns false if it was already saved (the watch may resend). */
    suspend fun saveWatchSession(payload: WatchSessionPayload): Boolean = write {
        val exists = db.rawQuery(
            "SELECT 1 FROM session_answers WHERE answer_key = ? AND answer_value = ? LIMIT 1",
            arrayOf(VitalsKeys.WATCH_ID, payload.id),
        ).use { it.moveToFirst() }
        if (exists) return@write false

        val start = parseInstant(payload.startTime) ?: Instant.now()
        val end = parseInstant(payload.endTime) ?: start
        val vitals = Vitals(
            heartRateAvg = payload.heartRateAvg,
            heartRateMin = payload.heartRateMin,
            heartRateMax = payload.heartRateMax,
            heartRateSource = VitalsKeys.SOURCE_WATCH,
        )
        insertSession(payload.kind, start, end, payload.durationSeconds, payload.answers + vitals.toAnswers() + (VitalsKeys.WATCH_ID to payload.id))
        true
    }

    private fun insertSession(kind: String, start: Instant, end: Instant, durationSeconds: Long, answers: Map<String, String>): Long {
        val now = Instant.now().toIsoString()
        var id = 0L
        db.transaction {
            id = insert("sessions", null, ContentValues().apply {
                put("kind", kind)
                put("start_time", start.toIsoString())
                put("end_time", end.toIsoString())
                put("duration_seconds", durationSeconds)
                put("created_at", now)
            })
            answers.forEach { (key, value) -> insertAnswer(id, key, value, now) }
        }
        return id
    }

    /** Adds or replaces answers on a saved session (used to attach vitals after the fact). */
    suspend fun putAnswers(sessionId: Long, answers: Map<String, String>) = write {
        val now = Instant.now().toIsoString()
        db.transaction {
            answers.forEach { (key, value) ->
                delete("session_answers", "session_id = ? AND answer_key = ?", arrayOf(sessionId.toString(), key))
                insertAnswer(sessionId, key, value, now)
            }
        }
    }

    private fun SQLiteDatabase.insertAnswer(sessionId: Long, key: String, value: String, createdAt: String) {
        insert("session_answers", null, ContentValues().apply {
            put("session_id", sessionId)
            put("answer_key", key)
            put("answer_value", value)
            put("created_at", createdAt)
        })
    }

    suspend fun upsertDailyHealth(day: String, input: DailyHealthInput) = write {
        val now = Instant.now().toIsoString()
        val values = ContentValues().apply {
            put("water", input.water); put("fiber", input.fiber); put("meals", input.meals)
            put("stress", input.stress); put("sleep", input.sleep); put("exercise", input.exercise)
            put("caffeine", input.caffeine.ifEmpty { "None" }); put("alcohol", input.alcohol.ifEmpty { "None" })
            put("medication", input.medication.ifEmpty { "None" }); put("mood", input.mood.ifEmpty { "Neutral" })
            put("updated_at", now)
        }
        val updated = db.update("daily_health", values, "day = ?", arrayOf(day))
        if (updated == 0) {
            values.put("day", day)
            values.put("created_at", now)
            db.insert("daily_health", null, values)
        }
    }

    suspend fun saveSettings(settings: AppSettings) = write {
        val now = Instant.now().toIsoString()
        db.transaction {
            settings.toMap().forEach { (key, value) -> putSetting(key, value, now) }
        }
    }

    private fun SQLiteDatabase.putSetting(key: String, value: String, updatedAt: String) {
        insertWithOnConflict("settings", null, ContentValues().apply {
            put("setting_key", key)
            put("setting_value", value)
            put("updated_at", updatedAt)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun clearAllData() = write {
        db.transaction {
            delete("session_answers", null, null)
            delete("sessions", null, null)
            delete("daily_health", null, null)
        }
    }

    /** The archive format the Expo app exported, so backups move freely between versions. */
    suspend fun exportArchive(): JSONObject = withContext(Dispatchers.IO) {
        val sessions = JSONArray()
        db.rawQuery("SELECT id, kind, start_time, end_time, duration_seconds, created_at FROM sessions ORDER BY start_time DESC", null).use { c ->
            while (c.moveToNext()) sessions.put(JSONObject().apply {
                put("id", c.getLong(0)); put("kind", c.getString(1)); put("startTime", c.getString(2))
                put("endTime", c.getString(3)); put("durationSeconds", c.getLong(4)); put("createdAt", c.getString(5))
            })
        }
        val answers = JSONArray()
        db.rawQuery("SELECT session_id, answer_key, answer_value FROM session_answers", null).use { c ->
            while (c.moveToNext()) answers.put(JSONObject().apply {
                put("sessionId", c.getLong(0)); put("key", c.getString(1)); put("value", c.getString(2))
            })
        }
        val health = JSONArray()
        readDailyHealth().forEach { h ->
            health.put(JSONObject().apply {
                put("id", h.id); put("day", h.day); put("water", h.water); put("fiber", h.fiber); put("meals", h.meals)
                put("stress", h.stress); put("sleep", h.sleep); put("exercise", h.exercise); put("caffeine", h.caffeine)
                put("alcohol", h.alcohol); put("medication", h.medication); put("mood", h.mood)
                put("createdAt", h.createdAt); put("updatedAt", h.updatedAt)
            })
        }
        val settings = JSONArray()
        db.rawQuery("SELECT setting_key, setting_value, updated_at FROM settings", null).use { c ->
            while (c.moveToNext()) settings.put(JSONObject().apply {
                put("key", c.getString(0)); put("value", c.getString(1)); put("updatedAt", c.getString(2))
            })
        }
        JSONObject().apply {
            put("sessions", sessions); put("answers", answers); put("dailyHealth", health); put("settings", settings)
        }
    }

    suspend fun importArchive(raw: JSONObject): ImportSummary = write {
        val archive = ArchiveNormalizer.normalize(raw)
        val idMap = mutableMapOf<Long, Long>()
        var importedSessions = 0
        var skippedSessions = 0
        var importedAnswers = 0
        var importedHealthDays = 0
        val now = Instant.now().toIsoString()

        db.transaction {
            archive.sessions.forEach { s ->
                val existing = rawQuery(
                    "SELECT id FROM sessions WHERE kind = ? AND start_time = ? AND end_time = ? AND duration_seconds = ? LIMIT 1",
                    arrayOf(s.kind, s.startTime, s.endTime, s.durationSeconds.toString()),
                ).use { if (it.moveToFirst()) it.getLong(0) else null }
                if (existing != null) {
                    idMap[s.id] = existing
                    skippedSessions++
                } else {
                    idMap[s.id] = insert("sessions", null, ContentValues().apply {
                        put("kind", s.kind); put("start_time", s.startTime); put("end_time", s.endTime)
                        put("duration_seconds", s.durationSeconds); put("created_at", s.createdAt)
                    })
                    importedSessions++
                }
            }
            archive.answers.forEach { a ->
                val sessionId = idMap[a.sessionId] ?: return@forEach
                val exists = rawQuery(
                    "SELECT 1 FROM session_answers WHERE session_id = ? AND answer_key = ? LIMIT 1",
                    arrayOf(sessionId.toString(), a.key),
                ).use { it.moveToFirst() }
                if (!exists) {
                    insertAnswer(sessionId, a.key, a.value, now)
                    importedAnswers++
                }
            }
            archive.dailyHealth.forEach { h ->
                val exists = rawQuery("SELECT 1 FROM daily_health WHERE day = ? LIMIT 1", arrayOf(h.day)).use { it.moveToFirst() }
                insertWithOnConflict("daily_health", null, ContentValues().apply {
                    put("day", h.day); put("water", h.water); put("fiber", h.fiber); put("meals", h.meals)
                    put("stress", h.stress); put("sleep", h.sleep); put("exercise", h.exercise)
                    put("caffeine", h.caffeine.ifEmpty { "None" }); put("alcohol", h.alcohol.ifEmpty { "None" })
                    put("medication", h.medication.ifEmpty { "None" }); put("mood", h.mood.ifEmpty { "Neutral" })
                    put("created_at", h.createdAt); put("updated_at", h.updatedAt)
                }, SQLiteDatabase.CONFLICT_REPLACE)
                if (!exists) importedHealthDays++
            }
            archive.settings.forEach { (key, value, updatedAt) -> putSetting(key, value, updatedAt) }
        }
        ImportSummary(importedSessions, skippedSessions, importedAnswers, importedHealthDays)
    }

    private suspend fun <T> write(block: () -> T): T = withContext(Dispatchers.IO) {
        val result = writeLock.withLock { block() }
        reload()
        result
    }

    private inline fun SQLiteDatabase.transaction(block: SQLiteDatabase.() -> Unit) {
        beginTransaction()
        try {
            block()
            setTransactionSuccessful()
        } finally {
            endTransaction()
        }
    }
}
