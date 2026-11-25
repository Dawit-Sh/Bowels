package com.d_ave.bowels.wear

import com.d_ave.bowels.BowelsApp
import com.d_ave.bowels.notify.Notifications
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
                // The item's host is the watch that sent it: it has Bowels, whatever it announces.
                uri.host?.let { app.prefs.knownWatches = app.prefs.knownWatches + it }
                val payload = raw?.let { runCatching { WatchSessionPayload.fromJson(it) }.getOrNull() }
                if (payload != null) {
                    val saved = app.repository.saveWatchSession(payload)
                    app.markHasRealData()
                    if (saved) Notifications.showWatchSessionSaved(this@WatchSyncService, payload.durationSeconds)
                }
                // Deleting the item is the watch's signal that the phone has it.
                runCatching { Wearable.getDataClient(this@WatchSyncService).deleteDataItems(uri).await() }
            }
        }
    }
}
