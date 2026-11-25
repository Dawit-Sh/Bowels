package com.d_ave.bowels.dashboard

/** An IPv4 route of the current network: [address] as a number, and its prefix length. */
data class Route(val address: Int, val prefixLength: Int)

/**
 * Whether the receiver can be reached from where the phone is now, without trying.
 *
 * A receiver on a private address (a home server like 192.168.1.10) is only reachable on a network
 * that has a route to it other than the default one: the home Wi-Fi's own subnet, or a VPN that
 * carries it. Anywhere else a payload would only time out, so it waits for home. A receiver with a
 * public name is reachable from any connected network.
 */
object HomeNetwork {
    fun reachable(host: String, routes: Collection<Route>): Boolean {
        val address = privateIpv4(host) ?: return true
        return routes.any { it.prefixLength in 1..32 && covers(it, address) }
    }

    /** [host] as a number when it is a private IPv4 address (home networks, carrier NAT, Tailscale), else null. */
    fun privateIpv4(host: String): Int? {
        val address = ipv4(host) ?: return null
        return address.takeIf { a -> PRIVATE.any { covers(it, a) } }
    }

    fun ipv4(text: String): Int? {
        val parts = text.trim().split('.')
        if (parts.size != 4) return null
        var value = 0
        for (part in parts) {
            if (part.isEmpty() || part.length > 3 || !part.all { it.isDigit() }) return null
            val n = part.toInt()
            if (n > 255) return null
            value = (value shl 8) or n
        }
        return value
    }

    /** IPv4 bytes in network order as a number, or null for anything else (IPv6). */
    fun ipv4(bytes: ByteArray): Int? =
        if (bytes.size != 4) null else bytes.fold(0) { acc, b -> (acc shl 8) or (b.toInt() and 0xFF) }

    private val PRIVATE = listOf(
        Route(ipv4("10.0.0.0")!!, 8), Route(ipv4("172.16.0.0")!!, 12),
        Route(ipv4("192.168.0.0")!!, 16), Route(ipv4("100.64.0.0")!!, 10),
    )

    private fun covers(route: Route, address: Int): Boolean {
        if (route.prefixLength <= 0) return true
        val mask = if (route.prefixLength >= 32) -1 else (-1 shl (32 - route.prefixLength))
        return (route.address and mask) == (address and mask)
    }
}
