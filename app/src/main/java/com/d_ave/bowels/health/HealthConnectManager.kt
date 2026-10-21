package com.d_ave.bowels.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.d_ave.bowels.data.Vitals
import com.d_ave.bowels.shared.VitalsKeys
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Reads heart rate and blood pressure recorded around a session by anything that writes to
 * Health Connect (Samsung Health, Fitbit, Pixel Watch, Omron and other cuffs). Bowels only reads.
 */
class HealthConnectManager(private val context: Context) {
    enum class Status { Available, NeedsInstall, NeedsUpdate, Unsupported }

    val permissions = setOf(
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
    )

    fun status(): Status = when (HealthConnectClient.getSdkStatus(context, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE -> Status.Available
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Status.NeedsUpdate
        else -> if (android.os.Build.VERSION.SDK_INT >= 28) Status.NeedsInstall else Status.Unsupported
    }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun grantedPermissions(): Set<String> =
        if (status() == Status.Available) runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet()) else emptySet()

    suspend fun hasAnyPermission() = grantedPermissions().intersect(permissions).isNotEmpty()

    /** One tap to Health Connect's Play listing, which also finishes its onboarding. */
    fun installIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage("com.android.vending")
        data = Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding")
        putExtra("overlay", true)
        putExtra("callerId", context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    /**
     * Heart rate during the session (with a little slack either side) and the blood pressure
     * reading closest to it within an hour. Missing permissions or data just leave fields empty.
     */
    suspend fun vitalsAround(start: Instant, end: Instant): Vitals {
        val granted = grantedPermissions()
        if (granted.isEmpty()) return Vitals()
        var vitals = Vitals()

        if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) {
            val samples = runCatching {
                client.readRecords(
                    ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(start.minus(HR_SLACK), end.plus(HR_SLACK))),
                ).records.flatMap { it.samples }.map { it.beatsPerMinute }
            }.getOrDefault(emptyList())
            if (samples.isNotEmpty()) {
                vitals = vitals.copy(
                    heartRateAvg = samples.average().roundToInt(),
                    heartRateMin = samples.min().toInt(),
                    heartRateMax = samples.max().toInt(),
                    heartRateSource = VitalsKeys.SOURCE_HEALTH_CONNECT,
                )
            }
        }

        if (HealthPermission.getReadPermission(BloodPressureRecord::class) in granted) {
            val middle = start.plus(Duration.between(start, end).dividedBy(2))
            val reading = runCatching {
                client.readRecords(
                    ReadRecordsRequest(BloodPressureRecord::class, TimeRangeFilter.between(start.minus(BP_WINDOW), end.plus(BP_WINDOW))),
                ).records.minByOrNull { abs(Duration.between(middle, it.time).seconds) }
            }.getOrNull()
            if (reading != null) {
                vitals = vitals.copy(
                    systolic = reading.systolic.inMillimetersOfMercury.roundToInt(),
                    diastolic = reading.diastolic.inMillimetersOfMercury.roundToInt(),
                    bloodPressureTime = reading.time,
                    bloodPressureSource = VitalsKeys.SOURCE_HEALTH_CONNECT,
                )
            }
        }
        return vitals
    }

    companion object {
        const val PROVIDER = "com.google.android.apps.healthdata"
        private val HR_SLACK: Duration = Duration.ofMinutes(2)
        private val BP_WINDOW: Duration = Duration.ofMinutes(60)
    }
}
