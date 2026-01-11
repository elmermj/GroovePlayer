package com.aethelsoft.grooveplayer.domain.model

/**
 * Domain model representing a Bluetooth device.
 * This is the model used by the presentation and domain layers.
 */
data class BluetoothDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean = false
)
