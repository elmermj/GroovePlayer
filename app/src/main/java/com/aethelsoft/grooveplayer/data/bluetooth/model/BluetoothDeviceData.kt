package com.aethelsoft.grooveplayer.data.bluetooth.model

import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice

/**
 * Data layer model for Bluetooth device information.
 * This model can hold Android-specific BluetoothDevice reference
 * which should not be exposed to domain/presentation layers.
 */
internal data class BluetoothDeviceData(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val device: AndroidBluetoothDevice? = null
)
