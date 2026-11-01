package com.d_ave.bowels.data

import com.d_ave.bowels.shared.VitalsKeys
import java.time.Instant

enum class SessionKind(val key: String) {
    Urine("urine"), Bowel("bowel");

    companion object {
        fun from(value: String?) = if (value == "urine") Urine else Bowel
    }
}

enum class ThemeMode(val key: String) {
    Light("light"), Dark("dark"), System("system");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.key == value } ?: System
    }
}

enum class AccentKey(val key: String, val label: String) {
    Olive("olive", "Olive"), Coral("coral", "Coral"), Rose("rose", "Rose"), Slate("slate", "Slate"),
    Ocean("ocean", "Ocean"), Forest("forest", "Forest"), Lavender("lavender", "Lavender"),
    Sunset("sunset", "Sunset"), Berry("berry", "Berry"), Mono("mono", "Mono"),
    /** Colors from the phone's wallpaper (Android 12 and newer). */
    Wallpaper("wallpaper", "Wallpaper");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.key == value } ?: Olive
    }
}

/** The surfaces behind everything: paper color in light mode, depth in dark mode. */
enum class BackgroundKey(val key: String, val label: String) {
    Cream("cream", "Cream"), Paper("paper", "Paper"), Sand("sand", "Sand"), Mist("mist", "Mist"),
    Blush("blush", "Blush"), Sage("sage", "Sage"), Dusk("dusk", "Dusk"),
    /** True black in dark mode, for OLED screens. */
    Black("black", "Black");

    companion object {
        fun from(value: String?) = entries.firstOrNull { it.key == value } ?: Cream
    }
}

enum class Screen { Onboarding, Home, Active, Questions, History, Analytics, Weekly, Health, Settings, Badges, Session }

data class SessionRecord(
    val id: Long,
    val kind: SessionKind,
    val startTime: Instant,
    val endTime: Instant,
    val durationSeconds: Long,
    val createdAt: Instant,
)

/** A saved session with its answers folded in, which is what most screens want. */
data class SessionWithAnswers(val session: SessionRecord, val answers: Map<String, String>) {
    val id get() = session.id
    val stoolType: Int? get() = answers["stool_type"]?.toIntOrNull()
    val vitals: Vitals get() = Vitals.fromAnswers(answers)
}

data class DailyHealthRecord(
    val id: Long,
    val day: String,
    val water: String,
    val fiber: String,
    val meals: String,
    val stress: String,
    val sleep: String,
    val exercise: String,
    val caffeine: String,
    val alcohol: String,
    val medication: String,
    val mood: String,
    val createdAt: String,
    val updatedAt: String,
) {
    fun toInput() = DailyHealthInput(water, fiber, meals, stress, sleep, exercise, caffeine, alcohol, medication, mood)
}

data class DailyHealthInput(
    val water: String = "Okay",
    val fiber: String = "Medium",
    val meals: String = "Balanced",
    val stress: String = "Low",
    val sleep: String = "Fair",
    val exercise: String = "Light",
    val caffeine: String = "None",
    val alcohol: String = "None",
    val medication: String = "None",
    val mood: String = "Neutral",
) {
    operator fun get(field: String): String = when (field) {
        "water" -> water
        "fiber" -> fiber
        "meals" -> meals
        "stress" -> stress
        "sleep" -> sleep
        "exercise" -> exercise
        "caffeine" -> caffeine
        "alcohol" -> alcohol
        "medication" -> medication
        else -> mood
    }

    fun with(field: String, value: String) = when (field) {
        "water" -> copy(water = value)
        "fiber" -> copy(fiber = value)
        "meals" -> copy(meals = value)
        "stress" -> copy(stress = value)
        "sleep" -> copy(sleep = value)
        "exercise" -> copy(exercise = value)
        "caffeine" -> copy(caffeine = value)
        "alcohol" -> copy(alcohol = value)
        "medication" -> copy(medication = value)
        else -> copy(mood = value)
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val accent: AccentKey = AccentKey.Olive,
    val background: BackgroundKey = BackgroundKey.Cream,
    val reminderHour: Int = 20,
    /** Daily reminders, as many as you like (up to [MAX_REMINDERS]), at the times you pick. */
    val reminderTimes: List<java.time.LocalTime> = listOf(java.time.LocalTime.of(reminderHour, 0)),
    val notificationsEnabled: Boolean = true,
    val hasOnboarded: Boolean = false,
    val hasRealData: Boolean = false,
) {
    fun toMap() = mapOf(
        "themeMode" to themeMode.key,
        "accent" to accent.key,
        "background" to background.key,
        "reminderHour" to (reminderTimes.firstOrNull()?.hour ?: reminderHour).toString(),
        "reminderTimes" to reminderTimes.joinToString(",") { "%02d:%02d".format(it.hour, it.minute) },
        "notificationsEnabled" to notificationsEnabled.toString(),
        "hasOnboarded" to hasOnboarded.toString(),
        "hasRealData" to hasRealData.toString(),
    )

    companion object {
        const val MAX_REMINDERS = 6

        fun parseTimes(value: String?): List<java.time.LocalTime>? = value?.split(",")
            ?.mapNotNull { runCatching { java.time.LocalTime.parse(it.trim()) }.getOrNull() }
            ?.distinct()?.sorted()?.take(MAX_REMINDERS)

        fun fromMap(rows: Map<String, String>) = AppSettings(
            themeMode = ThemeMode.from(rows["themeMode"]),
            accent = AccentKey.from(rows["accent"]),
            background = BackgroundKey.from(rows["background"]),
            reminderHour = rows["reminderHour"]?.toIntOrNull() ?: 20,
            // Before 3.2 there was one reminder hour.
            reminderTimes = parseTimes(rows["reminderTimes"])
                ?: listOf(java.time.LocalTime.of((rows["reminderHour"]?.toIntOrNull() ?: 20).coerceIn(0, 23), 0)),
            notificationsEnabled = rows["notificationsEnabled"]?.let { it == "true" } ?: true,
            hasOnboarded = rows["hasOnboarded"] == "true",
            hasRealData = rows["hasRealData"] == "true",
        )
    }
}

/** Heart rate and blood pressure around a session. Every field is optional. */
data class Vitals(
    val heartRateAvg: Int? = null,
    val heartRateMin: Int? = null,
    val heartRateMax: Int? = null,
    val heartRateSource: String? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val bloodPressureTime: Instant? = null,
    val bloodPressureSource: String? = null,
) {
    val hasHeartRate get() = heartRateAvg != null
    val hasBloodPressure get() = systolic != null && diastolic != null
    val isEmpty get() = !hasHeartRate && !hasBloodPressure

    fun toAnswers(): Map<String, String> = buildMap {
        heartRateAvg?.let { put(VitalsKeys.HR_AVG, it.toString()) }
        heartRateMin?.let { put(VitalsKeys.HR_MIN, it.toString()) }
        heartRateMax?.let { put(VitalsKeys.HR_MAX, it.toString()) }
        if (heartRateAvg != null) heartRateSource?.let { put(VitalsKeys.HR_SOURCE, it) }
        if (hasBloodPressure) {
            put(VitalsKeys.BP_SYSTOLIC, systolic.toString())
            put(VitalsKeys.BP_DIASTOLIC, diastolic.toString())
            bloodPressureTime?.let { put(VitalsKeys.BP_TIME, it.toString()) }
            bloodPressureSource?.let { put(VitalsKeys.BP_SOURCE, it) }
        }
    }

    /** Fills in whatever this one is missing from [other]. Readings already here win. */
    fun mergeMissing(other: Vitals): Vitals = Vitals(
        heartRateAvg = heartRateAvg ?: other.heartRateAvg,
        heartRateMin = if (hasHeartRate) heartRateMin else other.heartRateMin,
        heartRateMax = if (hasHeartRate) heartRateMax else other.heartRateMax,
        heartRateSource = if (hasHeartRate) heartRateSource else other.heartRateSource,
        systolic = if (hasBloodPressure) systolic else other.systolic,
        diastolic = if (hasBloodPressure) diastolic else other.diastolic,
        bloodPressureTime = if (hasBloodPressure) bloodPressureTime else other.bloodPressureTime,
        bloodPressureSource = if (hasBloodPressure) bloodPressureSource else other.bloodPressureSource,
    )

    companion object {
        fun fromAnswers(answers: Map<String, String>) = with(VitalsKeys) {
            Vitals(
                heartRateAvg = answers[HR_AVG]?.toIntOrNull(),
                heartRateMin = answers[HR_MIN]?.toIntOrNull(),
                heartRateMax = answers[HR_MAX]?.toIntOrNull(),
                heartRateSource = answers[HR_SOURCE],
                systolic = answers[BP_SYSTOLIC]?.toIntOrNull(),
                diastolic = answers[BP_DIASTOLIC]?.toIntOrNull(),
                bloodPressureTime = answers[BP_TIME]?.let { runCatching { Instant.parse(it) }.getOrNull() },
                bloodPressureSource = answers[BP_SOURCE],
            )
        }
    }
}

data class SessionDraft(
    val kind: SessionKind = SessionKind.Bowel,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val durationSeconds: Long = 0,
    val urineUrgency: String? = null,
    val urineColor: String? = null,
    val stoolType: Int? = 4,
    val pain: String? = "none",
    val blood: Boolean? = false,
    val urgency: Int? = 3,
    val completion: String? = "normal",
    val adaptiveTags: List<String> = emptyList(),
    val vitals: Vitals = Vitals(),
) {
    val isRunning get() = startTime != null && endTime == null

    fun answers(): Map<String, String> {
        val base = if (kind == SessionKind.Urine) {
            mapOf("urine_urgency" to urineUrgency, "urine_color" to urineColor)
        } else {
            mapOf(
                "stool_type" to stoolType?.toString(),
                "pain" to pain,
                "blood" to blood?.toString(),
                "urgency" to urgency?.toString(),
                "completion" to completion,
                "adaptive_tags" to adaptiveTags.joinToString(","),
            )
        }
        return base.filterValues { !it.isNullOrEmpty() }.mapValues { it.value!! } + vitals.toAnswers()
    }
}

data class InsightItem(val id: String, val title: String, val body: String, val severity: Severity) {
    enum class Severity { Info, Warning, High }
}

data class DayCount(val day: java.time.LocalDate, val count: Int)
