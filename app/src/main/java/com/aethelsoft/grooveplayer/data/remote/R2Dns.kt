package com.aethelsoft.grooveplayer.data.remote

import android.net.DnsResolver
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import com.aethelsoft.grooveplayer.domain.backup.R2Connect
import okhttp3.Dns
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * OkHttp DNS for signed R2 PUT/GET.
 *
 * Prefers a typed A query so the library-snapshot GET does not block in
 * getaddrinfo's AAAA wait. See [R2Connect].
 */
class R2Dns(
    private val typedQueryAvailable: Boolean = Build.VERSION.SDK_INT >= 29,
    private val queryType: (type: Int, hostname: String) -> List<InetAddress> = { type, hostname ->
        AndroidR2DnsQuery.lookup(type, hostname, R2Connect.DNS_QUERY_TIMEOUT_MS)
    },
    private val system: (hostname: String) -> List<InetAddress> = { hostname ->
        Dns.SYSTEM.lookup(hostname)
    },
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (typedQueryAvailable) {
            val ipv4 = runCatching { queryType(R2Connect.TYPE_A, hostname) }.getOrDefault(emptyList())
            val ipv6 = if (ipv4.any { it is java.net.Inet4Address }) {
                emptyList()
            } else {
                runCatching { queryType(R2Connect.TYPE_AAAA, hostname) }.getOrDefault(emptyList())
            }
            return R2Connect.selectAddresses(ipv4, ipv6) { system(hostname) }
        }
        return R2Connect.selectAddresses(emptyList(), emptyList()) { system(hostname) }
    }
}

internal object AndroidR2DnsQuery {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "r2-dns").apply { isDaemon = true }
    }

    fun lookup(type: Int, hostname: String, timeoutMs: Long): List<InetAddress> {
        if (Build.VERSION.SDK_INT < 29) return emptyList()
        return queryApi29(type, hostname, timeoutMs)
    }

    @RequiresApi(29)
    private fun queryApi29(type: Int, hostname: String, timeoutMs: Long): List<InetAddress> {
        val result = AtomicReference<List<InetAddress>>(emptyList())
        val latch = CountDownLatch(1)
        val signal = CancellationSignal()
        try {
            DnsResolver.getInstance().query(
                null,
                hostname,
                DnsResolver.CLASS_IN,
                type,
                executor,
                signal,
                object : DnsResolver.Callback<List<InetAddress>> {
                    override fun onAnswer(answer: List<InetAddress>, rcode: Int) {
                        if (rcode == 0) result.set(answer)
                        latch.countDown()
                    }

                    override fun onError(error: DnsResolver.DnsException) {
                        latch.countDown()
                    }
                },
            )
        } catch (_: Exception) {
            signal.cancel()
            return emptyList()
        }
        if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            signal.cancel()
        }
        return result.get()
    }
}
