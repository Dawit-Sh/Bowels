package com.d_ave.bowels.wear

import android.content.Context
import android.net.Uri
import com.d_ave.bowels.shared.WatchProtocol
import com.d_ave.bowels.shared.WatchSessionPayload
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Hands finished sessions to Google Play services' Data Layer. It stores them on the watch and
 * delivers them to the phone as soon as it is reachable, so a session logged with the phone
 * in another room, or switched off, still arrives. The phone deletes each item once saved.
 */
class PhoneSync(private val context: Context) {
    private val dataClient get() = Wearable.getDataClient(context)

    suspend fun send(payload: WatchSessionPayload) {
        val request = PutDataMapRequest.create(WatchProtocol.sessionPath(payload.id)).apply {
            dataMap.putString(WatchProtocol.PAYLOAD_KEY, payload.toJson())
            dataMap.putLong("sentAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        dataClient.putDataItem(request).await()
    }

    /** Sessions the phone hasn't picked up yet. */
    suspend fun pendingCount(): Int = runCatching {
        val buffer = dataClient.getDataItems(Uri.parse("wear://*${WatchProtocol.SESSION_PATH_PREFIX}"), DataClient.FILTER_PREFIX).await()
        buffer.count.also { buffer.release() }
    }.getOrDefault(0)

    /** True when a paired phone has Bowels installed. */
    suspend fun phoneHasApp(): Boolean = runCatching {
        Wearable.getCapabilityClient(context).getCapability(WatchProtocol.PHONE_CAPABILITY, CapabilityClient.FILTER_ALL).await().nodes.isNotEmpty()
    }.getOrDefault(false)

    /** Changes to the session items (sent or picked up by the phone). */
    fun addListener(onChange: () -> Unit): DataClient.OnDataChangedListener {
        val listener = DataClient.OnDataChangedListener { it.release(); onChange() }
        dataClient.addListener(listener, Uri.parse("wear://*${WatchProtocol.SESSION_PATH_PREFIX}"), DataClient.FILTER_PREFIX)
        return listener
    }

    fun removeListener(listener: DataClient.OnDataChangedListener) {
        dataClient.removeListener(listener)
    }
}
