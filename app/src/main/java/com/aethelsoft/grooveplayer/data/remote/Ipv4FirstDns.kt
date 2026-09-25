package com.aethelsoft.grooveplayer.data.remote

import android.os.Build
import com.aethelsoft.grooveplayer.domain.network.Ipv4First
import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * OkHttp DNS for grooveplayer-backend.
 *
 * Asks for A and AAAA together so a blackholed AAAA lookup cannot run first
 * or add its full wait after A. IPv4 is listed before IPv6. On API 24–28, and
 * when both typed queries are empty, [Dns.SYSTEM] is bounded so getaddrinfo
 * cannot sit on AAAA. Pair with [Ipv4First.CONNECT_TIMEOUT_MS].
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
    private val aaaaGraceMs: Long = Ipv4First.AAAA_GRACE_MS,
    private val systemTimeoutMs: Long = Ipv4First.DNS_QUERY_TIMEOUT_MS,
    private val pool: ExecutorService = sharedPool,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (typedQueryAvailable) {
            val typed = lookupTyped(hostname)
            if (typed.isNotEmpty()) return Ipv4First.order(typed)
        }
        return Ipv4First.order(lookupSystem(hostname))
    }

    private fun lookupTyped(hostname: String): List<InetAddress> {
        val aFuture = submit {
            queryFamily(Ipv4First.TYPE_A, hostname, Ipv4First.DNS_QUERY_TIMEOUT_MS)
                .filterIsInstance<Inet4Address>()
        }
        // Start AAAA with A when this client keeps IPv6 as a fallback. R2 skips
        // that second query once an A record exists.
        val aaaaFuture = if (queryAaaaWhenIpv4Present) {
            submit {
                queryFamily(Ipv4First.TYPE_AAAA, hostname, Ipv4First.DNS_QUERY_TIMEOUT_MS)
                    .filter { it !is Inet4Address }
            }
        } else {
            null
        }
        try {
            val ipv4 = awaitTyped(aFuture, Ipv4First.DNS_QUERY_TIMEOUT_MS)
            val ipv6 = when {
                aaaaFuture == null && ipv4.isEmpty() ->
                    queryFamily(Ipv4First.TYPE_AAAA, hostname, Ipv4First.DNS_QUERY_TIMEOUT_MS)
                        .filter { it !is Inet4Address }
                aaaaFuture == null -> emptyList()
                ipv4.isNotEmpty() -> awaitTyped(aaaaFuture, aaaaGraceMs)
                else -> awaitTyped(aaaaFuture, Ipv4First.DNS_QUERY_TIMEOUT_MS)
            }
            return ipv4 + ipv6
        } finally {
            aFuture.cancel(true)
            aaaaFuture?.cancel(true)
        }
    }

    /**
     * getaddrinfo on a broken IPv6 network can block well past the startup cap.
     * minSdk is 24, and [android.net.DnsResolver] typed queries need API 29.
     */
    private fun lookupSystem(hostname: String): List<InetAddress> {
        val future = submit { system(hostname) }
        try {
            return future.get(systemTimeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw UnknownHostException("System DNS timed out for $hostname").apply { initCause(e) }
        } catch (e: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            throw e
        } catch (e: ExecutionException) {
            future.cancel(true)
            throw unwrap(e)
        }
    }

    private fun queryFamily(type: Int, hostname: String, timeoutMs: Long): List<InetAddress> {
        try {
            return queryType(type, hostname, timeoutMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        } catch (_: Exception) {
            return emptyList()
        }
    }

    private fun awaitTyped(future: Future<List<InetAddress>>, timeoutMs: Long): List<InetAddress> {
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            return emptyList()
        } catch (e: InterruptedException) {
            future.cancel(true)
            Thread.currentThread().interrupt()
            throw e
        } catch (e: ExecutionException) {
            future.cancel(true)
            val cause = e.cause
            if (cause is InterruptedException) {
                Thread.currentThread().interrupt()
                throw cause
            }
            return emptyList()
        }
    }

    private fun unwrap(error: ExecutionException): Nothing {
        val cause = error.cause
        if (cause is InterruptedException) {
            Thread.currentThread().interrupt()
            throw cause
        }
        if (cause is UnknownHostException) throw cause
        throw UnknownHostException("System DNS failed").apply { initCause(cause ?: error) }
    }

    private fun submit(block: () -> List<InetAddress>): Future<List<InetAddress>> =
        pool.submit<List<InetAddress>> {
            // A cancelled lookup interrupts the worker. Clear that before the next task.
            Thread.interrupted()
            block()
        }

    companion object {
        private val sharedPool: ExecutorService = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "ipv4-first-dns").apply { isDaemon = true }
        }
    }
}
