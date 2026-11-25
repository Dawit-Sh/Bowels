package com.d_ave.bowels.dashboard

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** What every payload carries besides its records, as the Life Dashboard apps send it. */
data class Envelope(val builtAt: Instant, val timezone: String, val appVersion: String, val device: String, val sequence: Long)

/**
 * Payloads, signatures and the bookkeeping of what the receiver already has. Free of Android, so
 * all of it runs in JVM tests.
 *
 * Sending is by difference, not by queue: every record has a uuid that never changes and the
 * receiver replaces a record it has by its uuid, so the app remembers a fingerprint per uuid of
 * what it last sent and sends whatever is new or changed. Nothing is lost while the phone is away
 * from home, the first send is the whole history, and sending something twice does no harm.
 */
object DashboardPayloads {
    /** Records per payload: a long history goes in a few payloads, each well under the receiver's limit. */
    const val CHUNK = 400

    fun fingerprint(record: Outgoing): String = sha256(record.type + "\u0000" + record.json.toString())

    /** The records the receiver doesn't have in this form yet, oldest first. */
    fun pending(records: List<Outgoing>, sent: Map<String, String>): List<Outgoing> =
        records.filter { sent[it.uuid] != fingerprint(it) }

    fun payload(envelope: Envelope, records: List<Outgoing>): String {
        val json = envelopeJson(envelope)
        records.groupBy { it.type }.forEach { (type, list) -> json.put(type, JSONArray(list.map { it.json })) }
        return json.toString()
    }

    /** A signed payload with no records, which the receiver answers to prove it has the same secret. */
    fun ping(envelope: Envelope): String = envelopeJson(envelope).put("test", true).toString()

    /** The X-Signature header: HMAC-SHA256 of the exact bytes sent, keyed with the shared secret. */
    fun signature(secret: String, body: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return "sha256=" + mac.doFinal(body).toHex()
    }

    /** The next sequence: always higher than the last, and close to the time, as the receiver orders writes by it. */
    fun nextSequence(last: Long, nowMillis: Long): Long = maxOf(last + 1, nowMillis)

    /**
     * Whether an answer says the payload was stored: HTTP 200 and the receiver's count, or its
     * "seen it before" (a resend after a lost answer).
     */
    fun stored(code: Int, body: String?): Boolean {
        if (code != 200) return false
        val json = runCatching { JSONObject(body ?: "") }.getOrNull() ?: return false
        return json.has("stored") || json.optBoolean("duplicate", false)
    }

    /** Whether the answer to a ping comes from a receiver that checked it with the same secret. */
    fun confirms(code: Int, body: String?, requestSignature: String): Boolean {
        if (code != 200) return false
        val json = runCatching { JSONObject(body ?: "") }.getOrNull() ?: return false
        return json.optJSONObject("writeback")?.optString("in_reply_to") == requestSignature
    }

    private fun envelopeJson(e: Envelope) = JSONObject()
        .put("timestamp", e.builtAt.toString())
        .put("timezone", e.timezone)
        .put("app_version", e.appVersion)
        .put("device", e.device)
        .put("source", DashboardRecords.SOURCE)
        .put("sequence", e.sequence)

    private fun sha256(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
