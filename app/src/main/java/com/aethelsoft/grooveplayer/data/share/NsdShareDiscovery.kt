package com.aethelsoft.grooveplayer.data.share

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.aethelsoft.grooveplayer.domain.model.ShareSessionInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val SERVICE_TYPE = "_grooveplayer._tcp."
private const val SERVICE_NAME_PREFIX = "GroovePlayer"

@Singleton
class NsdShareDiscovery @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private var registeredInfo: NsdServiceInfo? = null

    /**
     * Register this device as a share sender. Other devices can discover it.
     */
    fun register(
        port: Int,
        sessionToken: String,
        deviceName: String
    ): Result<Unit> {
        val manager = nsdManager ?: return Result.failure(IllegalStateException("NSD not available"))
        return runCatching {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = "${SERVICE_NAME_PREFIX}_${UUID.randomUUID().toString().take(8)}"
                serviceType = SERVICE_TYPE
                this.port = port
            }
            manager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    registeredInfo = serviceInfo
                }
                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
            })
        }
    }

    /**
     * Unregister the service. Call when done sending.
     */
    fun unregister() {
        val info = registeredInfo ?: return
        nsdManager?.unregisterService(object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(p0: NsdServiceInfo, p1: Int) {}
            override fun onUnregistrationFailed(p0: NsdServiceInfo, p1: Int) {}
            override fun onServiceRegistered(p0: NsdServiceInfo) {}
            override fun onServiceUnregistered(p0: NsdServiceInfo) {
                registeredInfo = null
            }
        })
        registeredInfo = null
    }

    /**
     * Discover nearby share senders. Emits ShareSessionInfo when a service is resolved.
     */
    fun discover(): Flow<ShareSessionInfo> = callbackFlow {
        val manager = nsdManager
        if (manager == null) {
            close()
            return@callbackFlow
        }

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (!service.serviceType.contains("grooveplayer", ignoreCase = true)) return
                manager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: return
                        val port = serviceInfo.port
                        trySend(ShareSessionInfo(
                            host = host,
                            port = port,
                            sessionToken = "",
                            deviceName = serviceInfo.serviceName.removePrefix("${SERVICE_NAME_PREFIX}_")
                        ))
                    }
                })
            }
            override fun onServiceLost(service: NsdServiceInfo) {}
        }

        try {
            manager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                manager.stopServiceDiscovery(listener)
            } catch (_: Exception) {}
        }
    }
}
