package com.aethelsoft.grooveplayer.domain.network

import com.aethelsoft.grooveplayer.domain.auth.StartupSessionRecovery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class Ipv4FirstTest {

    @Test
    fun ipv4AddressesComeBeforeIpv6() {
        val v4a = ipv4(1, 2, 3, 4)
        val v4b = ipv4(5, 6, 7, 8)
        val v6a = ipv6(1)
        val v6b = ipv6(2)
        assertEquals(
            listOf(v4a, v4b, v6a, v6b),
            Ipv4First.order(listOf(v6a, v4a, v6b, v4b)),
        )
    }

    @Test
    fun ipv6OnlyListIsUnchanged() {
        val v6 = ipv6(9)
        assertEquals(listOf(v6), Ipv4First.order(listOf(v6)))
    }

    @Test
    fun emptyLookupFailsInsteadOfHanging() {
        try {
            Ipv4First.order(emptyList())
            error("expected UnknownHostException")
        } catch (e: UnknownHostException) {
            assertTrue(e.message.orEmpty().isNotBlank())
        }
    }

    @Test
    fun oneDnsBoundPlusOneConnectFitsUnderTheStartupCap() {
        assertTrue(Ipv4First.CONNECT_TIMEOUT_MS in 1L..5_000L)
        assertTrue(Ipv4First.DNS_QUERY_TIMEOUT_MS in 1L..5_000L)
        assertTrue(Ipv4First.AAAA_GRACE_MS in 1L..Ipv4First.DNS_QUERY_TIMEOUT_MS)
        assertEquals(1, Ipv4First.TYPE_A)
        assertEquals(28, Ipv4First.TYPE_AAAA)
        // A and AAAA overlap, and the system fallback uses this same DNS bound
        // only when typed queries return nothing. Do not add those waits together.
        val budget = Ipv4First.DNS_QUERY_TIMEOUT_MS + Ipv4First.CONNECT_TIMEOUT_MS
        assertTrue(budget < StartupSessionRecovery.NETWORK_TIMEOUT_MS)
    }

    private fun ipv4(a: Int, b: Int, c: Int, d: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()))

    private fun ipv6(last: Int): InetAddress =
        InetAddress.getByAddress(ByteArray(16) { index -> if (index == 15) last.toByte() else 0 })
}
