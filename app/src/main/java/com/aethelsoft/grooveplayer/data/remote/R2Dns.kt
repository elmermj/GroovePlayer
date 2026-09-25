package com.aethelsoft.grooveplayer.data.remote

import android.os.Build
import com.aethelsoft.grooveplayer.domain.backup.R2Connect
import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/**
 * OkHttp DNS for signed R2 PUT/GET.
 *
 * Uses [Ipv4FirstDns] (A before AAAA, IPv4 listed first), then drops IPv6 when
 * any A record exists so a blackholed AAAA route is never dialed. See [R2Connect].
 */
class R2Dns(
    typedQueryAvailable: Boolean = Build.VERSION.SDK_INT >= 29,
    queryType: (type: Int, hostname: String) -> List<InetAddress> = { type, hostname ->
        AndroidTypedDnsQuery.lookup(type, hostname, R2Connect.DNS_QUERY_TIMEOUT_MS)
    },
    system: (hostname: String) -> List<InetAddress> = { hostname ->
        Dns.SYSTEM.lookup(hostname)
    },
) : Dns {
    private val ordered = Ipv4FirstDns(
        typedQueryAvailable = typedQueryAvailable,
        // Backup transfers still skip AAAA once an A record exists (SCRUM-75).
        queryAaaaWhenIpv4Present = false,
        queryType = { type, hostname, _ -> queryType(type, hostname) },
        system = system,
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = ordered.lookup(hostname)
        val ipv4 = addresses.filterIsInstance<Inet4Address>()
        return if (ipv4.isNotEmpty()) ipv4 else addresses
    }
}
