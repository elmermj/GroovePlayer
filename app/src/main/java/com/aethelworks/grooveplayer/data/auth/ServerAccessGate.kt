package com.aethelworks.grooveplayer.data.auth

import com.aethelworks.grooveplayer.domain.auth.BackendCallGuard
import com.aethelworks.grooveplayer.domain.model.PrivilegeTier
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live session snapshot read by the backend OkHttp interceptor.
 * [probe] marks the startup `/v1/me` (and its refresh) so a Free session
 * can make that one check without opening every other API.
 */
@Singleton
class ServerAccessGate @Inject constructor() {
    @Volatile private var sessionOpen: Boolean = false
    @Volatile private var tier: PrivilegeTier = PrivilegeTier.FREE
    private val probes = AtomicInteger(0)

    fun publish(hasSession: Boolean, tier: PrivilegeTier) {
        sessionOpen = hasSession
        this.tier = tier
    }

    fun sessionPresent(): Boolean = sessionOpen

    fun allowsPaidApis(): Boolean = BackendCallGuard.allowsPaidApis(sessionOpen, tier)

    fun allows(path: String, method: String): Boolean =
        BackendCallGuard.allow(
            path = path,
            method = method,
            hasSession = sessionOpen,
            tier = tier,
            sessionProbe = probes.get() > 0,
        )

    suspend fun <T> probe(block: suspend () -> T): T {
        probes.incrementAndGet()
        try {
            return block()
        } finally {
            probes.decrementAndGet()
        }
    }
}
