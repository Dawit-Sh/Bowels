package com.d_ave.bowels.dashboard

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.util.AtomicFile
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.d_ave.bowels.BowelsApp
import com.d_ave.bowels.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** What the Settings card shows. */
data class DashboardStatus(
    val link: DashboardLink? = null,
    /** Records the receiver doesn't have yet (or has in an older form). */
    val waiting: Int = 0,
    val total: Int = 0,
    val lastSentAt: Instant? = null,
    val lastProblem: String? = null,
    val sending: Boolean = false,
)

/** The outcome of pairing: what the receiver said to a signed ping. */
enum class PairResult { Confirmed, WrongSecret, NotReachable }

/**
 * Sends Bowels' visits and daily checks to a Life Dashboard receiver on your own server, once it is
 * paired with the receiver's QR code. Off until then.
 *
 * It sends by difference (see [DashboardPayloads]): after every change, when the app opens, and every
 * hour as a fallback, whatever the receiver doesn't have yet goes, provided the phone can reach it
 * (for a server at home: on the home Wi-Fi, see [HomeNetwork]). Away from home nothing is tried and
 * nothing is lost. The pairing and the bookkeeping live in no-backup storage, so the secret never
 * goes into a cloud backup.
 */
class DashboardSync(private val context: Context) {
    private val dir = File(context.noBackupFilesDir, "dashboard").apply { mkdirs() }
    private val configFile = AtomicFile(File(dir, "config.json"))
    private val stateFile = AtomicFile(File(dir, "state.json"))
    private val lock = Mutex()
    private val _status = MutableStateFlow(DashboardStatus(link = readLink()))
    val status: StateFlow<DashboardStatus> = _status.asStateFlow()

    val isPaired: Boolean get() = _status.value.link != null

    /** Saves a scanned code unless the receiver refused its secret, and starts sending. */
    suspend fun pair(link: DashboardLink): PairResult = withContext(Dispatchers.IO) {
        val result = ping(link)
        if (result != PairResult.WrongSecret) {
            lock.withLock {
                write(configFile, JSONObject().put("url", link.url).put("secret", link.secret).put("name", link.name ?: JSONObject.NULL)
                    .put("paired_at", Instant.now().toString()).toString())
                // A new receiver has none of it: start the bookkeeping over.
                write(stateFile, JSONObject().put("sequence", readState().sequence).toString())
            }
            _status.value = DashboardStatus(link = link)
            refresh()
            schedule(context)
            kick(context)
        }
        result
    }

    suspend fun unpair() {
        withContext(Dispatchers.IO) {
            stop(context)
            lock.withLock {
                configFile.delete()
                write(stateFile, JSONObject().put("sequence", readState().sequence).toString())
            }
            _status.value = DashboardStatus()
        }
    }

    /** Recounts what is waiting, after the data changed. */
    suspend fun refresh() {
        withContext(Dispatchers.IO) {
            val link = readLink()
            if (link == null) {
                _status.value = DashboardStatus()
            } else {
                val records = records()
                val state = readState()
                _status.value = _status.value.copy(
                    link = link, total = records.size, waiting = DashboardPayloads.pending(records, state.sent).size,
                    lastSentAt = state.lastSentAt, lastProblem = state.lastProblem,
                )
            }
        }
    }

    /** Whether the paired receiver can be reached from the network the phone is on now. */
    fun canReach(): Boolean = readLink()?.let(::reachable) ?: false

    /** Sends what the receiver doesn't have. False when it should be tried again soon (the receiver failed). */
    suspend fun send(): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            val link = readLink() ?: return@withLock true
            if (!reachable(link)) {
                Log.i(TAG, "not on a network that reaches ${link.host}; waiting")
                return@withLock true
            }
            var state = readState()
            val pending = DashboardPayloads.pending(records(), state.sent)
            if (pending.isEmpty()) return@withLock true
            _status.value = _status.value.copy(sending = true)
            var ok = true
            try {
                for (chunk in pending.chunked(DashboardPayloads.CHUNK)) {
                    val sequence = DashboardPayloads.nextSequence(state.sequence, System.currentTimeMillis())
                    val body = DashboardPayloads.payload(envelope(sequence), chunk).toByteArray(Charsets.UTF_8)
                    state = state.copy(sequence = sequence)
                    val (code, answer) = post(link, body)
                    if (!DashboardPayloads.stored(code, answer)) {
                        state = state.copy(lastProblem = problem(code))
                        ok = false
                        Log.w(TAG, "receiver answered $code for ${chunk.size} records")
                        break
                    }
                    state = state.copy(
                        sent = state.sent + chunk.associate { it.uuid to DashboardPayloads.fingerprint(it) },
                        lastSentAt = Instant.now(), lastProblem = null,
                    )
                    Log.i(TAG, "sent ${chunk.size} records")
                }
            } catch (e: Exception) {
                state = state.copy(lastProblem = "Couldn't reach ${link.host}.")
                ok = false
                Log.w(TAG, "send failed: ${e.javaClass.simpleName}")
            } finally {
                writeState(state)
                _status.value = _status.value.copy(sending = false)
            }
            ok
        }.also { refresh() }
    }

    private suspend fun records(): List<Outgoing> {
        val app = context.applicationContext as BowelsApp
        val data = app.repository.data.value ?: app.repository.reload()
        return DashboardRecords.all(data, ZoneId.systemDefault())
    }

    private fun ping(link: DashboardLink): PairResult = runCatching {
        val sequence = DashboardPayloads.nextSequence(readState().sequence, System.currentTimeMillis())
        val body = DashboardPayloads.ping(envelope(sequence)).toByteArray(Charsets.UTF_8)
        val signature = DashboardPayloads.signature(link.secret, body)
        val (code, answer) = post(link, body, signature)
        when {
            DashboardPayloads.confirms(code, answer, signature) -> PairResult.Confirmed
            code == 401 -> PairResult.WrongSecret
            else -> PairResult.NotReachable
        }
    }.getOrDefault(PairResult.NotReachable)

    private fun post(link: DashboardLink, body: ByteArray, signature: String = DashboardPayloads.signature(link.secret, body)): Pair<Int, String?> {
        val connection = URL(link.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setFixedLengthStreamingMode(body.size)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Signature", signature)
            connection.setRequestProperty("User-Agent", "Bowels/${BuildConfig.VERSION_NAME}")
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            return code to stream?.bufferedReader()?.use { it.readText().take(64_000) }
        } finally {
            connection.disconnect()
        }
    }

    private fun reachable(link: DashboardLink): Boolean {
        val host = runCatching { URI(link.url).host }.getOrNull() ?: return false
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val routes = cm.getLinkProperties(network)?.routes.orEmpty().mapNotNull { route ->
            val destination = route.destination ?: return@mapNotNull null
            HomeNetwork.ipv4(destination.address.address)?.let { Route(it, destination.prefixLength) }
        }
        return HomeNetwork.reachable(host, routes)
    }

    private fun envelope(sequence: Long) = Envelope(
        builtAt = Instant.now(),
        timezone = ZoneId.systemDefault().id,
        appVersion = "bowels ${BuildConfig.VERSION_NAME}",
        device = Build.MODEL ?: "phone",
        sequence = sequence,
    )

    private fun problem(code: Int) = when (code) {
        401 -> "The receiver refused the signature. Scan its pairing code again."
        413 -> "The receiver said the payload is too big."
        in 500..599 -> "The receiver had a problem ($code). Trying again later."
        else -> "The receiver answered $code."
    }

    // ------------------------------------------------------------------ storage

    private data class State(val sequence: Long = 0, val sent: Map<String, String> = emptyMap(), val lastSentAt: Instant? = null, val lastProblem: String? = null)

    private fun readLink(): DashboardLink? = runCatching {
        val json = JSONObject(String(configFile.readFully(), Charsets.UTF_8))
        DashboardLink(json.getString("url"), json.getString("secret"), json.optString("name").takeIf { it.isNotEmpty() && !json.isNull("name") })
    }.getOrNull()

    private fun readState(): State = runCatching {
        val json = JSONObject(String(stateFile.readFully(), Charsets.UTF_8))
        val sent = json.optJSONObject("sent")?.let { s -> s.keys().asSequence().associateWith { s.getString(it) } }.orEmpty()
        State(
            sequence = json.optLong("sequence", 0),
            sent = sent,
            lastSentAt = json.optString("last_sent_at").takeIf { it.isNotEmpty() }?.let { runCatching { Instant.parse(it) }.getOrNull() },
            lastProblem = json.optString("last_problem").takeIf { it.isNotEmpty() && !json.isNull("last_problem") },
        )
    }.getOrDefault(State())

    private fun writeState(state: State) {
        val json = JSONObject().put("sequence", state.sequence).put("sent", JSONObject(state.sent as Map<*, *>))
        state.lastSentAt?.let { json.put("last_sent_at", it.toString()) }
        state.lastProblem?.let { json.put("last_problem", it) }
        write(stateFile, json.toString())
    }

    private fun write(file: AtomicFile, text: String) {
        val out = file.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    companion object {
        private const val TAG = "BowelsDashboard"
        private const val NOW = "dashboard-send-now"
        private const val HOURLY = "dashboard-send-hourly"

        private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Sends soon (a few seconds, so a burst of changes goes as one), once there is a network. */
        fun kick(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                NOW, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<DashboardWorker>().setConstraints(connected).setInitialDelay(5, TimeUnit.SECONDS).build(),
            )
        }

        /** The hourly fallback, for what waited while the phone was away from home. */
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                HOURLY, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DashboardWorker>(1, TimeUnit.HOURS).setConstraints(connected).build(),
            )
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NOW)
            WorkManager.getInstance(context).cancelUniqueWork(HOURLY)
        }
    }
}

class DashboardWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val sync = (applicationContext as BowelsApp).dashboard
        if (!sync.isPaired) return Result.success()
        return if (sync.send() || runAttemptCount >= 5) Result.success() else Result.retry()
    }
}
