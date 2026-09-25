package com.aethelsoft.grooveplayer.data.remote

import android.net.DnsResolver
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Typed A / AAAA lookup. getaddrinfo on a broken IPv6 network waits for AAAA
 * before it returns A records; [DnsResolver] can ask for A on its own.
 */
internal object AndroidTypedDnsQuery {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "backend-dns").apply { isDaemon = true }
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
