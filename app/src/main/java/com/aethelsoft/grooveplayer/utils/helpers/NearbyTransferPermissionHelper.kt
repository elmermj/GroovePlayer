package com.aethelsoft.grooveplayer.utils.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime permissions required for Nearby Connections API.
 * Ensures compatibility across Samsung, Xiaomi, iQOO, and other OEMs.
 */
@Singleton
class NearbyTransferPermissionHelper @Inject constructor(
    private val context: Context,
) {

    /**
     * Permissions needed for Nearby Connections P2P transfer.
     * - Bluetooth: CONNECT, SCAN, ADVERTISE (API 31+)
     * - Location: ACCESS_COARSE_LOCATION + ACCESS_FINE_LOCATION (Nearby API 8034, 8036)
     * - NEARBY_WIFI_DEVICES (API 34+) for additional discovery
     */
    fun getRequiredPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }.toTypedArray()

    fun hasAllPermissions(): Boolean =
        getRequiredPermissions().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun getMissingPermissions(): Array<String> =
        getRequiredPermissions().filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()
}
