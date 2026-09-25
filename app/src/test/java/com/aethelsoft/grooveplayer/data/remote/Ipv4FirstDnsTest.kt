package com.aethelsoft.grooveplayer.data.remote

import com.aethelsoft.grooveplayer.domain.network.Ipv4First
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        assertEquals(Ipv4First.DNS_QUERY_TIMEOUT_MS, aaaaTimeout)
    }

    @Test(timeout = 5_000)
    fun aAndAaaaRunConcurrently() {
        val aStarted = CountDownLatch(1)
        val aaaaStarted = CountDownLatch(1)
        val v4 = ipv4(1, 2, 3, 4)
        val v6 = ipv6(1)
        val dns = Ipv4FirstDns(
            typedQueryAvailable = true,
            queryType = { type, _, _ ->
                if (type == Ipv4First.TYPE_A) {
                    aStarted.countDown()
                    check(aaaaStarted.await(2, TimeUnit.SECONDS)) { "AAAA did not start while A was in flight" }
                    listOf(v4)
                } else {
                    aaaaStarted.countDown()
                    check(aStarted.await(2, TimeUnit.SECONDS)) { "A did not start while AAAA was in flight" }
                    listOf(v6)
                }
            },
            system = { error("system dns") },
        )
        assertEquals(listOf(v4, v6), dns.lookup("grooveplayer-backend.fly.dev"))
    }

    @Test(timeout = 2_000)
    fun slowAaaaDoesNotHoldUpIpv4() {
        val v4 = ipv4(9, 9, 9, 9)
        val dns = Ipv4FirstDns(
            typedQueryAvailable = true,
            aaaaGraceMs = 80,
            queryType = { type, _, _ ->
                if (type == Ipv4First.TYPE_A) {
                    listOf(v4)
                } else {
                    Thread.sleep(5_000)
                    listOf(ipv6(1))
                }
            },
            system = { error("system dns") },
        )
        assertEquals(listOf(v4), dns.lookup("grooveplayer-backend.fly.dev"))
    }

    @Test(timeout = 2_000)
    fun systemFallbackIsBoundedWhenTypedQueriesAreUnavailable() {
        val dns = Ipv4FirstDns(
            typedQueryAvailable = false,
            systemTimeoutMs = 80,
            system = {
                Thread.sleep(30_000)
                emptyList()
            },
        )
        try {
            dns.lookup("grooveplayer-backend.fly.dev")
            error("expected UnknownHostException")
        } catch (e: UnknownHostException) {
            assertTrue(e.message.orEmpty().isNotBlank())
        }
    }

    @Test
    fun interruptedQueryRestoresTheFlagAndAborts() {
        val dns = Ipv4FirstDns(
            typedQueryAvailable = true,
            queryType = { _, _, _ -> throw InterruptedException("dns") },
            system = { error("system dns") },
        )
        try {
            dns.lookup("grooveplayer-backend.fly.dev")
            error("expected InterruptedException")
        } catch (_: InterruptedException) {
            assertTrue(Thread.currentThread().isInterrupted)
        } finally {
            Thread.interrupted()
        }
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
