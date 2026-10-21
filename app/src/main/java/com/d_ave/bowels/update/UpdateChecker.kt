package com.d_ave.bowels.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Looks at the latest GitHub release for a newer phone APK. */
object UpdateChecker {
    private const val REPO = "Dawit-Sh/Bowels"

    data class Release(val tag: String, val apkUrl: String)

    sealed interface Result {
        data class Available(val release: Release) : Result
        data object UpToDate : Result
        data object NoApk : Result
        data object Failed : Result
    }

    suspend fun check(currentVersion: String): Result = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }
            if (connection.responseCode != 200) return@runCatching Result.Failed
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.getString("tag_name")
            if (!isNewer(currentVersion, tag)) return@runCatching Result.UpToDate
            val assets = json.optJSONArray("assets")
            // Releases may carry both the phone and the watch APK; the phone one doesn't say "wear".
            val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
                .filter { it.getString("name").endsWith(".apk") }
                .sortedBy { "wear" in it.getString("name").lowercase() }
                .firstOrNull() ?: return@runCatching Result.NoApk
            Result.Available(Release(tag, apk.getString("browser_download_url")))
        }.getOrDefault(Result.Failed)
    }

    fun isNewer(current: String, latest: String): Boolean {
        val a = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val b = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (y != x) return y > x
        }
        return false
    }
}
