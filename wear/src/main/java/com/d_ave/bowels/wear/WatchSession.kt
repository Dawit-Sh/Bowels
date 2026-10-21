package com.d_ave.bowels.wear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import kotlin.math.roundToInt

/** Heart rate seen during the session, kept as running totals so nothing grows unbounded. */
data class HeartRateStats(val latest: Int? = null, val min: Int? = null, val max: Int? = null, val sum: Long = 0, val count: Int = 0) {
    val average: Int? get() = if (count == 0) null else (sum.toDouble() / count).roundToInt()

    fun add(bpm: Int) = HeartRateStats(
        latest = bpm,
        min = minOf(min ?: bpm, bpm),
        max = maxOf(max ?: bpm, bpm),
        sum = sum + bpm,
        count = count + 1,
    )
}

data class RunningSession(val startTime: Instant, val heartRate: HeartRateStats = HeartRateStats(), val measuring: Boolean = false)

/**
 * The one running session on the watch, shared by the screen and the foreground service.
 * The start time is also saved, so a session survives the app being closed.
 */
object WatchSession {
    private val _running = MutableStateFlow<RunningSession?>(null)
    val running: StateFlow<RunningSession?> = _running.asStateFlow()

    fun restore(context: Context) {
        if (_running.value != null) return
        val start = prefs(context).getLong(KEY_START, 0)
        if (start > 0) _running.value = RunningSession(Instant.ofEpochMilli(start))
    }

    fun start(context: Context): RunningSession {
        val session = RunningSession(Instant.now())
        prefs(context).edit().putLong(KEY_START, session.startTime.toEpochMilli()).apply()
        _running.value = session
        return session
    }

    fun addHeartRate(bpm: Int) = _running.update { it?.copy(heartRate = it.heartRate.add(bpm)) }

    fun setMeasuring(measuring: Boolean) = _running.update { it?.copy(measuring = measuring) }

    /** Ends the session and hands back what was recorded. */
    fun stop(context: Context): RunningSession? {
        prefs(context).edit().remove(KEY_START).apply()
        return _running.value.also { _running.value = null }
    }

    private fun prefs(context: Context) = context.getSharedPreferences("bowels-watch", Context.MODE_PRIVATE)

    private const val KEY_START = "running_start"
}
