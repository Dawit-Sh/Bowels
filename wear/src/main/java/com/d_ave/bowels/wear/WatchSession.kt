package com.d_ave.bowels.wear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

data class RunningSession(val startTime: Instant)

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

    /** Ends the session and hands back when it started. */
    fun stop(context: Context): RunningSession? {
        prefs(context).edit().remove(KEY_START).apply()
        return _running.value.also { _running.value = null }
    }

    private fun prefs(context: Context) = context.getSharedPreferences("bowels-watch", Context.MODE_PRIVATE)

    private const val KEY_START = "running_start"
}
