package com.aethelsoft.grooveplayer.domain.backup

import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * How the restore client reaches R2.
 *
 * The library snapshot is the first request to the R2 host. Android's
 * getaddrinfo waits for a broken AAAA lookup before it returns A records, and
 * OkHttp then tries those IPv6 addresses one at a time. Each failed connect
 * used to sit for 30s, so two AAAA records matched the 60–74s stall at
 * "0 B of 17.4 KB". Song downloads then reused the pooled connection and
 * looked fast. Long audio bodies stay uncapped ([RestoreDownloadRetry.R2_CALL_TIMEOUT_MS]).
 */
object R2Connect {
    /** TCP handshake only. A long read timeout still covers the body. */
    const val CONNECT_TIMEOUT_MS = 10_000L

    /**
     * Bound for one typed DNS query (A, then AAAA only if A is empty).
     * Short enough that a hung AAAA query cannot consume the minute QA saw.
     */
    const val DNS_QUERY_TIMEOUT_MS = 4_000L

    /** [android.net.DnsResolver.TYPE_A] */
    const val TYPE_A = 1

    /** [android.net.DnsResolver.TYPE_AAAA] */
    const val TYPE_AAAA = 28

    /**
     * Use IPv4 when any A record exists so a blackholed AAAA route is never
     * dialed. [systemFallback] runs only when both typed queries came back empty.
     */
    fun selectAddresses(
        ipv4: List<InetAddress>,
        ipv6: List<InetAddress>,
        systemFallback: () -> List<InetAddress>,
    ): List<InetAddress> {
        val v4 = ipv4.filterIsInstance<Inet4Address>()
        if (v4.isNotEmpty()) return v4
        if (ipv6.isNotEmpty()) return ipv6
        val system = systemFallback()
        if (system.isEmpty()) throw UnknownHostException("R2 host has no addresses")
        val systemV4 = system.filterIsInstance<Inet4Address>()
        return if (systemV4.isNotEmpty()) systemV4 else system
    }
}
