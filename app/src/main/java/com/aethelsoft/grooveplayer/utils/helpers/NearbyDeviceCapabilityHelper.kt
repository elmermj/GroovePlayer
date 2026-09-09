package com.aethelsoft.grooveplayer.utils.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of device capability analysis for Nearby P2P transfer.
 */
data class NearbyDeviceCapability(
    val wifiDirectSupported: Boolean,
    val wifiP2pAvailable: Boolean,
    val bluetoothSupported: Boolean,
    val bluetoothEnabled: Boolean,
    val nearbyConnectionsAvailable: Boolean,
) {
    /** Human-readable summary of device readiness for P2P transfer. */
    val summary: String
        get() = when {
            !wifiDirectSupported && !bluetoothSupported -> "Device does not support Wi-Fi Direct or Bluetooth"
            !nearbyConnectionsAvailable -> "Google Play Services required for nearby transfer"
            !bluetoothEnabled -> "Turn on Bluetooth for device discovery"
            wifiDirectSupported && bluetoothEnabled -> "Ready — Wi-Fi Direct and Bluetooth available"
            bluetoothEnabled -> "Ready — Bluetooth available"
            else -> "Turn on Bluetooth to discover nearby devices"
        }

    /** True if the device can participate in nearby P2P transfer. */
    val isReady: Boolean
        get() = nearbyConnectionsAvailable &&
            bluetoothSupported &&
            bluetoothEnabled &&
            (wifiDirectSupported || wifiP2pAvailable)
}

/**
 * Analyzes device capabilities for Nearby P2P (Wi-Fi Direct, Bluetooth, Nearby Connections).
 */
@Singleton
class NearbyDeviceCapabilityHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun analyze(): NearbyDeviceCapability {
        val pm = context.packageManager

        val wifiDirectSupported = pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)

        val wifiP2pManager = @Suppress("DEPRECATION")
        context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        val wifiP2pAvailable = wifiP2pManager != null

        val bluetoothSupported = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

        val bluetoothEnabled = run {
            val adapter = @Suppress("DEPRECATION")
            (android.bluetooth.BluetoothAdapter.getDefaultAdapter())
            adapter?.isEnabled == true
        }

        val nearbyConnectionsAvailable = run {
            try {
                val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                availability.isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS
            } catch (_: Throwable) {
                false
            }
        }

        return NearbyDeviceCapability(
            wifiDirectSupported = wifiDirectSupported,
            wifiP2pAvailable = wifiP2pAvailable,
            bluetoothSupported = bluetoothSupported,
            bluetoothEnabled = bluetoothEnabled,
            nearbyConnectionsAvailable = nearbyConnectionsAvailable,
        )
    }
}
