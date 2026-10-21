package com.d_ave.bowels.wear

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.concurrent.futures.await
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.d_ave.bowels.shared.WatchProtocol
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** Finds a paired watch and whether Bowels is on it, so the phone can offer a one-tap install. */
class WatchConnector(private val context: Context) {
    sealed interface State {
        data object NoWatch : State
        data class NeedsInstall(val nodeId: String, val name: String) : State
        data class Ready(val name: String) : State
    }

    suspend fun state(): State = runCatching {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) return@runCatching State.NoWatch
        val withApp = Wearable.getCapabilityClient(context)
            .getCapability(WatchProtocol.WATCH_CAPABILITY, CapabilityClient.FILTER_ALL)
            .await().nodes.map { it.id }.toSet()
        val ready = nodes.firstOrNull { it.id in withApp }
        if (ready != null) State.Ready(ready.displayName) else State.NeedsInstall(nodes.first().id, nodes.first().displayName)
    }.getOrDefault(State.NoWatch) // No Wear OS app on the phone means no watch.

    /** Opens Bowels' Play Store page on the watch itself. */
    suspend fun openInstallOnWatch(nodeId: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(WatchProtocol.PLAY_STORE_URI))
        return runCatching { RemoteActivityHelper(context).startRemoteActivity(intent, nodeId).await() }.isSuccess
    }
}
