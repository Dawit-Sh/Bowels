package com.d_ave.bowels.wear

import com.d_ave.bowels.BowelsApp
import com.d_ave.bowels.data.parseInstant
import com.d_ave.bowels.notify.Notifications
import com.d_ave.bowels.shared.VitalsKeys
import com.d_ave.bowels.shared.WatchProtocol
import com.d_ave.bowels.shared.WatchSessionPayload
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

/**
 * Receives sessions logged on the watch. Play services wakes this up whenever the watch
 * reconnects, so sessions logged with the phone out of range arrive on their own.
 */
class WatchSyncService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        val app = application as BowelsApp
        val changed = events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path.orEmpty().startsWith(WatchProtocol.SESSION_PATH_PREFIX) }
            .map { it.dataItem.uri to DataMapItem.fromDataItem(it.dataItem).dataMap.getString(WatchProtocol.PAYLOAD_KEY) }
        events.release()

        // Listener callbacks already run off the main thread, so blocking here is fine.
        runBlocking {
            for ((uri, raw) in changed) {
                val payload = raw?.let { runCatching { WatchSessionPayload.fromJson(it) }.getOrNull() }
                if (payload != null) {
                    val saved = app.repository.saveWatchSession(payload)
                    app.markHasRealData()
                    if (saved) {
                        attachBloodPressure(app, payload)
                        Notifications.showWatchSessionSaved(this@WatchSyncService, payload.durationSeconds, payload.heartRateAvg)
                    }
                }
                // Deleting the item is the watch's signal that the phone has it.
                runCatching { Wearable.getDataClient(this@WatchSyncService).deleteDataItems(uri).await() }
            }
        }
    }

    /** The watch measures heart rate itself; blood pressure can only come from Health Connect. */
    private suspend fun attachBloodPressure(app: BowelsApp, payload: WatchSessionPayload) {
        val start = parseInstant(payload.startTime) ?: return
        val end = parseInstant(payload.endTime) ?: start
        val found = app.healthConnect.vitalsAround(start, end)
        if (!found.hasBloodPressure) return // The app checks again later, when it next opens.
        val session = app.repository.data.value?.sessions?.firstOrNull { it.answers[VitalsKeys.WATCH_ID] == payload.id } ?: return
        app.repository.putAnswers(session.id, session.vitals.mergeMissing(found).toAnswers())
    }
}
