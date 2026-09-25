package com.aethelsoft.grooveplayer.domain.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * How API clients reach grooveplayer-backend on a network whose IPv6 route is dead.
 *
 * Android getaddrinfo can block on AAAA before it returns A records, and OkHttp
 * then dials addresses in that order. Each dead IPv6 connect used to sit until
 * the 12s startup cap, so Profile fell back with no quota and no Cloud backup.
 * Listing IPv4 first, and giving up on a handshake quickly, lets that call use
 * the working IPv4 route.
 */
object Ipv4First {
    /** One TCP handshake. A dead address must fail over inside the startup cap. */
    const val CONNECT_TIMEOUT_MS = 5_000L

    /**
     * Bound for one typed DNS query. A is allowed this long. AAAA gets it only
     * when no A record came back.
     */
    const val DNS_QUERY_TIMEOUT_MS = 4_000L

    /**
     * Extra wait for AAAA after A already answered. Long enough for a healthy
     * DNS response, short enough that a blackholed AAAA lookup cannot eat startup.
     */
    const val AAAA_GRACE_MS = 1_000L

    /** [android.net.DnsResolver.TYPE_A] */
    const val TYPE_A = 1

    /** [android.net.DnsResolver.TYPE_AAAA] */
    const val TYPE_AAAA = 28

    /**
     * IPv4 addresses, then IPv6. Order within each family is preserved.
     * An empty list throws so OkHttp does not wait on a route that will never exist.
     */
    fun order(addresses: List<InetAddress>): List<InetAddress> {
        if (addresses.isEmpty()) throw UnknownHostException("Host has no addresses")
        val ipv4 = addresses.filterIsInstance<Inet4Address>()
        val ipv6 = addresses.filter { it !is Inet4Address }
        return ipv4 + ipv6
    }
}
