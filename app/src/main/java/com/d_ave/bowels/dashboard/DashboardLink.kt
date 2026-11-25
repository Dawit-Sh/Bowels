package com.d_ave.bowels.dashboard

import java.net.URI
import java.net.URLDecoder

/**
 * A pairing code from a Life Dashboard receiver (hibi-server is one): where to post and the secret
 * to sign with. Nothing else, so a code can never decide what Bowels sends.
 *
 * The receiver's /pair page shows it as a QR code of an https link with everything in the fragment
 * (after the #, which a browser never sends to a server):
 *
 *     https://owen282000.github.io/life-dashboard-companion-app/pair#v=1&url=...&secret=...&name=...
 *
 * The same payload as lifedashboard://pair#... is read too. Kept free of android.net.Uri so the
 * whole format is covered by JVM tests.
 */
data class DashboardLink(val url: String, val secret: String, val name: String?) {
    /** Host and port, which is how you recognise your own machine. */
    val host: String
        get() = runCatching { URI(url).let { u -> if (u.port == -1) u.host else "${u.host}:${u.port}" } }.getOrNull() ?: url

    companion object {
        private const val PAIR_HOST = "owen282000.github.io"
        private const val PAIR_PATH = "/life-dashboard-companion-app/pair"
        private const val SCHEME = "lifedashboard"
        private const val VERSION = "1"

        /** The link in [text], or null when it isn't a pairing code Bowels can use. */
        fun parse(text: String?): DashboardLink? {
            val trimmed = text?.trim().orEmpty()
            val hash = trimmed.indexOf('#')
            if (hash < 0) return null
            val uri = runCatching { URI(trimmed.substring(0, hash)) }.getOrNull() ?: return null
            val ours = when (uri.scheme?.lowercase()) {
                "https" -> uri.host.equals(PAIR_HOST, true) && uri.path.orEmpty().trimEnd('/').equals(PAIR_PATH, true)
                SCHEME -> (uri.host ?: uri.schemeSpecificPart?.trim('/')).equals("pair", true)
                else -> false
            }
            if (!ours) return null
            // Split on & and = first and decode after, so an encoded & inside the url stays in it.
            val fields = trimmed.substring(hash + 1).split('&').mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else decode(part.substring(0, i)) to decode(part.substring(i + 1))
            }.toMap()
            if (fields["v"] != VERSION) return null
            val url = fields["url"]?.trim().orEmpty()
            val secret = fields["secret"]?.trim().orEmpty()
            if (!usableUrl(url) || secret.length < 16 || secret.length > 512) return null
            val name = fields["name"]?.trim()?.take(64)?.takeIf { it.isNotEmpty() }
            return DashboardLink(url, secret, name)
        }

        /**
         * http(s) with a host. Plain http only to an address on your own network (a private IPv4
         * address or a .local name): the secret signs every payload but the body itself is readable,
         * so it must not cross the internet unencrypted.
         */
        fun usableUrl(url: String): Boolean {
            if (url.length > 2048) return false
            val uri = runCatching { URI(url) }.getOrNull() ?: return false
            val host = uri.host ?: return false
            return when (uri.scheme?.lowercase()) {
                "https" -> true
                "http" -> HomeNetwork.privateIpv4(host) != null || host.endsWith(".local", ignoreCase = true)
                else -> false
            }
        }

        // Percent-decoding only: a literal + stays a + (a base64 secret has them), as in the Life Dashboard app.
        private fun decode(value: String): String =
            runCatching { URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") }.getOrDefault(value)
    }
}
