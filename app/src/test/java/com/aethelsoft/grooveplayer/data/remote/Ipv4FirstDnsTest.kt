package com.aethelsoft.grooveplayer.data.remote

import com.aethelsoft.grooveplayer.domain.network.Ipv4First
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class Ipv4FirstDnsTest {

    @Test
    fun aRecordIsListedBeforeAaaaAndSkipsTheSystemResolver() {
        var systemCalls = 0
        var aaaaTimeout = -1L
        val v4 = ipv4(5, 6, 7, 8)
        val v6 = ipv6(1)
        val dns = Ipv4FirstDns(
            typedQueryAvailable = true,
            queryType = { type, _, timeoutMs ->
                when (type) {
                    Ipv4First.TYPE_A -> listOf(v4)
                    Ipv4First.TYPE_AAAA -> {
                        aaaaTimeout = timeoutMs
                        listOf(v6)
                    }
                    else -> error("unexpected type $type")
                }
            },
            system = {
                systemCalls++
                error("system dns")
            },
        )
        assertEquals(listOf(v4, v6), dns.lookup("grooveplayer-backend.fly.dev"))
        assertEquals(0, systemCalls)
        assertEquals(Ipv4First.AAAA_GRACE_MS, aaaaTimeout)
    }

    @Test
    fun systemFallbackListsIpv4BeforeIpv6() {
        val v4 = ipv4(8, 8, 8, 8)
        val v6 = ipv6(1)
        val dns = Ipv4FirstDns(
            typedQueryAvailable = true,
            queryType = { _, _, _ -> emptyList() },
            system = { listOf(v6, v4) },
        )
        assertEquals(listOf(v4, v6), dns.lookup("grooveplayer-backend.fly.dev"))
    }

    @Test
    fun aaaaIsUsedWhenThereIsNoARecord() {
        val v6 = ipv6(4)
        var aaaaTimeout = -1L
        val dns = Ipv4FirstDns(
            typedQueryAvailable = true,
            queryType = { type, _, timeoutMs ->
                if (type == Ipv4First.TYPE_AAAA) {
                    aaaaTimeout = timeoutMs
                    listOf(v6)
                } else {
                    emptyList()
                }
            },
            system = { error("system dns") },
        )
        assertEquals(listOf(v6), dns.lookup("grooveplayer-backend.fly.dev"))
        assertEquals(Ipv4First.DNS_QUERY_TIMEOUT_MS, aaaaTimeout)
    }

    private fun ipv4(a: Int, b: Int, c: Int, d: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()))

    private fun ipv6(last: Int): InetAddress =
        InetAddress.getByAddress(ByteArray(16) { index -> if (index == 15) last.toByte() else 0 })
}
