package com.aethelsoft.grooveplayer.data.remote

import com.aethelsoft.grooveplayer.domain.backup.R2Connect
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class R2DnsTest {

    @Test
    fun aRecordSkipsTheSystemResolver() {
        var systemCalls = 0
        var aaaaQueries = 0
        val v4 = ipv4(5, 6, 7, 8)
        val dns = R2Dns(
            typedQueryAvailable = true,
            queryType = { type, _ ->
                when (type) {
                    R2Connect.TYPE_A -> listOf(v4)
                    R2Connect.TYPE_AAAA -> {
                        aaaaQueries++
                        emptyList()
                    }
                    else -> error("unexpected type $type")
                }
            },
            system = {
                systemCalls++
                error("system dns")
            },
        )
        assertEquals(listOf(v4), dns.lookup("bucket.r2.cloudflarestorage.com"))
        assertEquals(0, systemCalls)
        assertEquals(0, aaaaQueries)
    }

    @Test
    fun emptyTypedQueriesUseSystemIpv4Only() {
        val v4 = ipv4(8, 8, 8, 8)
        val dns = R2Dns(
            typedQueryAvailable = true,
            queryType = { _, _ -> emptyList() },
            system = { listOf(ipv6(), v4) },
        )
        assertEquals(listOf(v4), dns.lookup("bucket.r2.cloudflarestorage.com"))
    }

    private fun ipv4(a: Int, b: Int, c: Int, d: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()))

    private fun ipv6(): InetAddress =
        InetAddress.getByAddress(ByteArray(16) { index -> if (index == 15) 1 else 0 })
}
