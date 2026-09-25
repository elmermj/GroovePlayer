package com.aethelsoft.grooveplayer.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class R2ConnectTest {

    @Test
    fun ipv4AnswersSkipIpv6AndTheSystemResolver() {
        var systemCalls = 0
        val chosen = R2Connect.selectAddresses(
            ipv4 = listOf(ipv4(1, 2, 3, 4)),
            ipv6 = listOf(ipv6()),
            systemFallback = {
                systemCalls++
                error("system dns")
            },
        )
        assertEquals(listOf(ipv4(1, 2, 3, 4)), chosen)
        assertEquals(0, systemCalls)
    }

    @Test
    fun ipv6IsUsedOnlyWhenThereIsNoIpv4() {
        val v6 = ipv6()
        val chosen = R2Connect.selectAddresses(
            ipv4 = emptyList(),
            ipv6 = listOf(v6),
            systemFallback = { error("system dns") },
        )
        assertEquals(listOf(v6), chosen)
    }

    @Test
    fun systemFallbackDropsBlackholedIpv6WhenIpv4Exists() {
        val v4 = ipv4(9, 9, 9, 9)
        val chosen = R2Connect.selectAddresses(
            ipv4 = emptyList(),
            ipv6 = emptyList(),
            systemFallback = { listOf(ipv6(), v4) },
        )
        assertEquals(listOf(v4), chosen)
    }

    @Test
    fun emptyLookupFailsInsteadOfHanging() {
        try {
            R2Connect.selectAddresses(emptyList(), emptyList()) { emptyList() }
            error("expected UnknownHostException")
        } catch (e: UnknownHostException) {
            assertTrue(e.message.orEmpty().isNotBlank())
        }
    }

    @Test
    fun snapshotConnectBudgetStaysUnderTheMinuteQaSaw() {
        assertTrue(R2Connect.CONNECT_TIMEOUT_MS in 1L..10_000L)
        assertTrue(R2Connect.DNS_QUERY_TIMEOUT_MS in 1L..5_000L)
        assertEquals(1, R2Connect.TYPE_A)
        assertEquals(28, R2Connect.TYPE_AAAA)
        // A then AAAA, then one TCP connect. Two 30s connects were the 60s stall.
        val budget = R2Connect.DNS_QUERY_TIMEOUT_MS * 2 + R2Connect.CONNECT_TIMEOUT_MS
        assertTrue(budget < 30_000L)
        // Long recordings still have no whole-call deadline (SCRUM-74).
        assertEquals(0, RestoreDownloadRetry.R2_CALL_TIMEOUT_MS)
        assertEquals(3, RestoreDownloadRetry.MAX_ATTEMPTS)
    }

    private fun ipv4(a: Int, b: Int, c: Int, d: Int): InetAddress =
        InetAddress.getByAddress(byteArrayOf(a.toByte(), b.toByte(), c.toByte(), d.toByte()))

    private fun ipv6(): InetAddress =
        InetAddress.getByAddress(ByteArray(16) { index -> if (index == 15) 1 else 0 })
}
