package com.d_ave.bowels.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

/** Small local state: the running session, earned milestones, one-time flags. */
class Prefs(private val context: Context) {
    private val prefs = context.getSharedPreferences("bowels", Context.MODE_PRIVATE)

    var activeDraft: SessionDraft?
        get() = prefs.getString(KEY_DRAFT, null)?.let { runCatching { draftFromJson(JSONObject(it)) }.getOrNull() }
        set(value) {
            prefs.edit().apply { if (value?.startTime != null) putString(KEY_DRAFT, draftToJson(value).toString()) else remove(KEY_DRAFT) }.apply()
        }

    var unlockedMilestones: Set<Int>
        get() = prefs.getStringSet(KEY_MILESTONES, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        set(value) = prefs.edit().putStringSet(KEY_MILESTONES, value.map { it.toString() }.toSet()).apply()

    /** The watch install card was dismissed. */
    var watchPromptDismissed: Boolean
        get() = prefs.getBoolean(KEY_WATCH_PROMPT, false)
        set(value) = prefs.edit().putBoolean(KEY_WATCH_PROMPT, value).apply()

    var healthConnectAsked: Boolean
        get() = prefs.getBoolean(KEY_HC_ASKED, false)
        set(value) = prefs.edit().putBoolean(KEY_HC_ASKED, value).apply()

    /**
     * Brings over what the React Native build kept in AsyncStorage (its own SQLite file,
     * "RKStorage"): the running session and the milestones already announced, so updating
     * doesn't re-send every milestone notification.
     */
    fun migrateFromAsyncStorage() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val file = context.getDatabasePath("RKStorage")
        if (file.exists()) {
            runCatching {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    db.rawQuery("SELECT key, value FROM catalystLocalStorage WHERE key IN (?, ?)", arrayOf(OLD_DRAFT, OLD_MILESTONES)).use { c ->
                        while (c.moveToNext()) {
                            val value = c.getString(1) ?: continue
                            when (c.getString(0)) {
                                OLD_MILESTONES -> unlockedMilestones = JSONArray(value).let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() }
                                OLD_DRAFT -> runCatching { draftFromJson(JSONObject(value)) }.getOrNull()?.let { activeDraft = it }
                            }
                        }
                    }
                }
            }
        }
        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }

    private fun draftToJson(d: SessionDraft) = JSONObject().apply {
        put("kind", d.kind.key)
        put("startTime", d.startTime?.toIsoString())
        put("endTime", d.endTime?.toIsoString())
        put("durationSeconds", d.durationSeconds)
        put("urineUrgency", d.urineUrgency)
        put("urineColor", d.urineColor)
        put("stoolType", d.stoolType)
        put("pain", d.pain)
        put("blood", d.blood)
        put("urgency", d.urgency)
        put("completion", d.completion)
        put("adaptiveTags", JSONArray(d.adaptiveTags))
        put("vitals", JSONObject(d.vitals.toAnswers()))
    }

    // Also reads the Expo draft, which has the same field names.
    private fun draftFromJson(j: JSONObject): SessionDraft {
        fun str(key: String) = if (j.isNull(key)) null else j.optString(key).takeIf { it.isNotEmpty() }
        fun int(key: String) = if (j.isNull(key) || !j.has(key)) null else j.optInt(key)
        val tags = j.optJSONArray("adaptiveTags")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        val vitals = j.optJSONObject("vitals")?.let { v -> Vitals.fromAnswers(v.keys().asSequence().associateWith { v.getString(it) }) } ?: Vitals()
        return SessionDraft(
            kind = SessionKind.from(str("kind")),
            startTime = parseInstant(str("startTime")),
            endTime = parseInstant(str("endTime")),
            durationSeconds = j.optLong("durationSeconds", 0),
            urineUrgency = str("urineUrgency"),
            urineColor = str("urineColor"),
            stoolType = int("stoolType"),
            pain = str("pain"),
            blood = if (j.isNull("blood") || !j.has("blood")) null else j.optBoolean("blood"),
            urgency = int("urgency"),
            completion = str("completion"),
            adaptiveTags = tags,
            vitals = vitals,
        )
    }

    private companion object {
        const val KEY_DRAFT = "active_draft"
        const val KEY_MILESTONES = "unlocked_milestones"
        const val KEY_MIGRATED = "migrated_async_storage"
        const val KEY_WATCH_PROMPT = "watch_prompt_dismissed"
        const val KEY_HC_ASKED = "health_connect_asked"
        const val OLD_DRAFT = "bowels-active-draft"
        const val OLD_MILESTONES = "bowels-unlocked-milestones"
    }
}
