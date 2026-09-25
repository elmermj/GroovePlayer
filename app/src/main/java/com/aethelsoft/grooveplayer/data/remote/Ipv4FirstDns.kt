package com.aethelsoft.grooveplayer.data.remote

import android.os.Build
import com.aethelsoft.grooveplayer.domain.network.Ipv4First
import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/**
 * OkHttp DNS for grooveplayer-backend.
 *
 * Prefers a typed A query so a blackholed AAAA lookup cannot run first.
 * When both families are returned, IPv4 is listed before IPv6. Pair with
 * [Ipv4First.CONNECT_TIMEOUT_MS] so a dead route fails over quickly.
 */
class Ipv4FirstDns(
    private val typedQueryAvailable: Boolean = Build.VERSION.SDK_INT >= 29,
    private val queryAaaaWhenIpv4Present: Boolean = true,
    private val queryType: (type: Int, hostname: String, timeoutMs: Long) -> List<InetAddress> = { type, hostname, timeoutMs ->
        AndroidTypedDnsQuery.lookup(type, hostname, timeoutMs)
    },
    private val system: (hostname: String) -> List<InetAddress> = { hostname ->
        Dns.SYSTEM.lookup(hostname)
    },
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (typedQueryAvailable) {
            val ipv4 = runCatching {
                queryType(Ipv4First.TYPE_A, hostname, Ipv4First.DNS_QUERY_TIMEOUT_MS)
            }.getOrDefault(emptyList()).filterIsInstance<Inet4Address>()
            val ipv6 = lookupIpv6(hostname, ipv4.isNotEmpty())
            val typed = ipv4 + ipv6
            if (typed.isNotEmpty()) return Ipv4First.order(typed)
        }
        return Ipv4First.order(system(hostname))
    }

    private fun lookupIpv6(hostname: String, haveIpv4: Boolean): List<InetAddress> {
        if (haveIpv4 && !queryAaaaWhenIpv4Present) return emptyList()
        val timeout = if (haveIpv4) Ipv4First.AAAA_GRACE_MS else Ipv4First.DNS_QUERY_TIMEOUT_MS
        return runCatching { queryType(Ipv4First.TYPE_AAAA, hostname, timeout) }
            .getOrDefault(emptyList())
            .filter { it !is Inet4Address }
    }
}
