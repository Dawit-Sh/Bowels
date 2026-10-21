package com.d_ave.bowels.wear

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DeltaDataType
import androidx.lifecycle.LifecycleService
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import kotlin.math.roundToInt

/**
 * Keeps the heart-rate sensor on while a session runs, even with the screen off, and shows
 * the session as an ongoing activity on the watch face.
 */
class SessionService : LifecycleService() {
    private val measureClient by lazy { HealthServices.getClient(this).measureClient }
    private var registered = false

    private val callback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) = Unit

        override fun onDataReceived(data: DataPointContainer) {
            data.getData(DataType.HEART_RATE_BPM)
                .map { it.value.roundToInt() }
                .filter { it in 25..250 } // Skip the zeros the sensor reports while it settles.
                .forEach(WatchSession::addHeartRate)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val session = WatchSession.running.value
        if (session == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground(session.startTime.toEpochMilli())
        if (!registered && hasSensorPermission(this)) {
            measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            registered = true
            WatchSession.setMeasuring(true)
        }
        return START_STICKY
    }

    private fun startInForeground(startMillis: Long) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Running session", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, WearActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Session running")
            .setContentText("Tap to finish")
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setUsesChronometer(true)
            .setWhen(startMillis)

        // The stopwatch on the watch face counts from boot time, not wall-clock time.
        val zero = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startMillis)
        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_notification)
            .setTouchIntent(open)
            .setStatus(Status.Builder().addTemplate("#time#").addPart("time", Status.StopwatchPart(zero)).build())
            .build()
            .apply(applicationContext)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        startForeground(NOTIFICATION_ID, builder.build(), type)
    }

    override fun onDestroy() {
        if (registered) {
            measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
            registered = false
        }
        WatchSession.setMeasuring(false)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "bowels-watch-session"
        private const val NOTIFICATION_ID = 7

        fun hasSensorPermission(context: Context) =
            ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED

        /** The foreground service type "health" needs the sensor permission; without it the timer still runs. */
        fun start(context: Context) {
            if (!hasSensorPermission(context)) return
            ContextCompat.startForegroundService(context, Intent(context, SessionService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionService::class.java))
        }
    }
}
