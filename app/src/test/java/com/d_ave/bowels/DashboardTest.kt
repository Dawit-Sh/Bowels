package com.d_ave.bowels

import com.d_ave.bowels.dashboard.DashboardLink
import com.d_ave.bowels.dashboard.DashboardPayloads
import com.d_ave.bowels.dashboard.DashboardRecords
import com.d_ave.bowels.dashboard.Envelope
import com.d_ave.bowels.dashboard.HomeNetwork
import com.d_ave.bowels.dashboard.Route
import com.d_ave.bowels.data.DailyHealthRecord
import com.d_ave.bowels.data.SessionKind
import com.d_ave.bowels.data.SessionRecord
import com.d_ave.bowels.data.SessionWithAnswers
import com.d_ave.bowels.data.StoredData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DashboardTest {
    private val zone = ZoneId.of("Africa/Addis_Ababa")

    // As hibi-server's pairing.py writes it (urlencode with quote, safe="").
    private val link = "https://owen282000.github.io/life-dashboard-companion-app/pair#v=1" +
        "&url=http%3A%2F%2F192.168.100.100%3A8080%2Fwebhook%2Fbowels&secret=ab%2Bcd%2Fef%3D%3D0123456789abcdef&name=hibi-server"

    private fun session(id: Long, start: String, answers: Map<String, String>, kind: SessionKind = SessionKind.Bowel): SessionWithAnswers {
        val s = Instant.parse(start)
        return SessionWithAnswers(SessionRecord(id, kind, s, s.plusSeconds(300), 300, s.plusSeconds(320)), answers)
    }

    private val visit = session(
        7, "2026-11-25T05:10:00Z",
        mapOf(
            "stool_type" to "4", "pain" to "mild", "blood" to "false", "urgency" to "3", "completion" to "normal",
            "adaptive_tags" to "bloating,gas", "hr_avg" to "72", "hr_min" to "65", "hr_max" to "80", "hr_source" to "watch",
            "watch_id" to "w-1", "vitals_checked" to "true",
        ),
    )

    private val check = DailyHealthRecord(
        1, "2026-11-25", "Okay", "High", "Balanced", "Low", "Great", "Light", "Medium", "None", "OTC", "Happy",
        "2026-11-25T18:00:00.000Z", "2026-11-25T19:30:00.000Z",
    )

    @Test
    fun `reads the receiver's pairing code, a plus in the secret included`() {
        val parsed = DashboardLink.parse(link)
        assertNotNull(parsed)
        assertEquals("http://192.168.100.100:8080/webhook/bowels", parsed!!.url)
        assertEquals("ab+cd/ef==0123456789abcdef", parsed.secret)
        assertEquals("hibi-server", parsed.name)
        assertEquals("192.168.100.100:8080", parsed.host)
        assertNotNull(DashboardLink.parse(link.replace("https://owen282000.github.io/life-dashboard-companion-app/pair", "lifedashboard://pair")))
    }

    @Test
    fun `refuses what isn't a usable code`() {
        assertNull(DashboardLink.parse("https://example.com/pair#v=1&url=http%3A%2F%2F192.168.1.2&secret=0123456789abcdef0"))
        assertNull(DashboardLink.parse(link.replace("v=1", "v=2")))
        assertNull(DashboardLink.parse(link.replace("secret=ab%2Bcd%2Fef%3D%3D0123456789abcdef", "secret=short")))
        // Plain http only on your own network.
        assertNull(DashboardLink.parse(link.replace("192.168.100.100", "dash.example.com")))
        assertTrue(DashboardLink.usableUrl("https://dash.example.com/webhook/bowels"))
        assertNull(DashboardLink.parse("not a link"))
    }

    @Test
    fun `a home server is reached only on a network with a route to it`() {
        val home = listOf(Route(HomeNetwork.ipv4("192.168.100.0")!!, 24), Route(0, 0))
        val away = listOf(Route(HomeNetwork.ipv4("10.20.0.0")!!, 16), Route(0, 0))
        assertTrue(HomeNetwork.reachable("192.168.100.100", home))
        assertFalse(HomeNetwork.reachable("192.168.100.100", away))
        assertFalse(HomeNetwork.reachable("192.168.100.100", listOf(Route(0, 0))))
        assertTrue(HomeNetwork.reachable("dash.example.com", emptyList()))
    }

    @Test
    fun `a visit goes with everything it recorded`() {
        val json = DashboardRecords.session(visit, zone)
        assertEquals("bowels-bowel-${Instant.parse("2026-11-25T05:10:00Z").toEpochMilli()}", json.getString("uuid"))
        assertEquals("2026-11-25T05:10:00Z", json.getString("time"))
        assertEquals("2026-11-25T05:15:00Z", json.getString("end_time"))
        assertEquals(300, json.getInt("duration_seconds"))
        assertEquals("bowel", json.getString("kind"))
        assertEquals(4, json.getInt("stool_type"))
        assertEquals("mild", json.getString("pain"))
        assertFalse(json.getBoolean("blood"))
        assertEquals(3, json.getInt("urgency"))
        assertEquals("normal", json.getString("completion"))
        assertEquals(listOf("bloating", "gas"), (0 until json.getJSONArray("tags").length()).map { json.getJSONArray("tags").getString(it) })
        assertEquals(72, json.getInt("heart_rate_avg"))
        assertEquals("watch", json.getString("heart_rate_source"))
        assertEquals("watch", json.getString("from"))
        assertEquals("+03:00", json.getString("zone_offset"))
        assertFalse(json.has("other"))
        assertFalse(json.has("bp_systolic"))
    }

    @Test
    fun `unknown answers travel too, bookkeeping doesn't`() {
        val json = DashboardRecords.session(session(1, "2026-11-25T05:10:00Z", mapOf("stool_type" to "5", "smell" to "strong")), zone)
        assertEquals("strong", json.getJSONObject("other").getString("smell"))
        assertEquals("phone", json.getString("from"))
    }

    @Test
    fun `a daily check goes once per day, in lower case`() {
        val json = DashboardRecords.dailyCheck(check, zone)
        assertEquals("bowels-day-2026-11-25", json.getString("uuid"))
        assertEquals("2026-11-25", json.getString("day"))
        assertEquals("2026-11-25T19:30:00Z", json.getString("time"))
        assertEquals("high", json.getString("fiber"))
        assertEquals("otc", json.getString("medication"))
        assertEquals("happy", json.getString("mood"))
    }

    @Test
    fun `the same visit saved twice goes once, and uuids don't depend on row ids`() {
        val twice = session(9, "2026-11-25T05:10:00Z", visit.answers)
        val records = DashboardRecords.all(StoredData(sessions = listOf(twice, visit), dailyHealth = listOf(check)), zone)
        assertEquals(2, records.size)
        assertEquals(DashboardRecords.SESSIONS, records[0].type)
        assertEquals("w-1", visit.answers["watch_id"])
        assertEquals("watch", records[0].json.getString("from")) // the first one saved, id 7
        val reinstalled = DashboardRecords.all(StoredData(sessions = listOf(session(1, "2026-11-25T05:10:00Z", visit.answers))), zone)
        assertEquals(records[0].uuid, reinstalled[0].uuid)
    }

    @Test
    fun `only what's new or changed is sent again`() {
        val data = StoredData(sessions = listOf(visit), dailyHealth = listOf(check))
        val records = DashboardRecords.all(data, zone)
        assertEquals(2, DashboardPayloads.pending(records, emptyMap()).size)
        val sent = records.associate { it.uuid to DashboardPayloads.fingerprint(it) }
        assertTrue(DashboardPayloads.pending(records, sent).isEmpty())
        // Vitals filled in later change the visit: it goes again, under the same uuid.
        val later = DashboardRecords.all(data.copy(sessions = listOf(visit.copy(answers = visit.answers + ("bp_systolic" to "120") + ("bp_diastolic" to "80")))), zone)
        val again = DashboardPayloads.pending(later, sent)
        assertEquals(listOf(records[0].uuid), again.map { it.uuid })
    }

    @Test
    fun `payloads carry the envelope and one array per record type`() {
        val envelope = Envelope(Instant.parse("2026-11-25T20:00:00Z"), "Africa/Addis_Ababa", "bowels 3.3.0", "Pixel 10a", 1764100800000)
        val records = DashboardRecords.all(StoredData(sessions = listOf(visit), dailyHealth = listOf(check)), zone)
        val json = JSONObject(DashboardPayloads.payload(envelope, records))
        assertEquals("bowels", json.getString("source"))
        assertEquals(1764100800000, json.getLong("sequence"))
        assertEquals("Africa/Addis_Ababa", json.getString("timezone"))
        assertEquals(1, json.getJSONArray("bowels_sessions").length())
        assertEquals(1, json.getJSONArray("bowels_daily_checks").length())
        assertTrue(JSONObject(DashboardPayloads.ping(envelope)).getBoolean("test"))
        assertEquals(1764100800001, DashboardPayloads.nextSequence(1764100800000, 5))
    }

    @Test
    fun `signs as the receiver checks it`() {
        // python: hmac.new(b"test-secret-0123456789abcdef", body, hashlib.sha256).hexdigest()
        assertEquals(
            "sha256=3a3e871506390887e564b13a63392f8913137a8d87b9ccc8738c3262ae860cd2",
            DashboardPayloads.signature("test-secret-0123456789abcdef", """{"source":"bowels","test":true}""".toByteArray()),
        )
    }

    @Test
    fun `reads the receiver's answers`() {
        assertTrue(DashboardPayloads.stored(200, """{"stored":2,"buckets":0,"deleted":0,"skipped":0}"""))
        assertTrue(DashboardPayloads.stored(200, """{"duplicate":true}"""))
        assertFalse(DashboardPayloads.stored(401, """{"error":"invalid signature"}"""))
        assertFalse(DashboardPayloads.stored(200, "<html>"))
        assertTrue(DashboardPayloads.confirms(200, """{"stored":0,"ping":true,"writeback":{"in_reply_to":"sha256=ab"}}""", "sha256=ab"))
        assertFalse(DashboardPayloads.confirms(200, """{"stored":0,"ping":true}""", "sha256=ab"))
    }
}
