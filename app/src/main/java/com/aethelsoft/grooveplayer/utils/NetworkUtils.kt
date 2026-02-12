package com.aethelsoft.grooveplayer.utils

import java.net.NetworkInterface

/**
 * Returns the local IPv4 address (e.g. for sharing over Wi‑Fi).
 */
internal fun getLocalIpAddress(): String? {
    return try {
        NetworkInterface.getNetworkInterfaces().toList().firstOrNull { iface ->
            !iface.isLoopback && iface.isUp
        }?.inetAddresses?.toList()?.firstOrNull { addr ->
            !addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false
        }?.hostAddress
    } catch (_: Exception) {
        null
    }
}
