package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Bluetooth operations.
 * This interface defines the contract for Bluetooth functionality
 * that the data layer must implement.
 */
interface BluetoothRepository {
    /**
     * Observes the list of available Bluetooth devices
     */
    fun observeAvailableDevices(): Flow<List<BluetoothDevice>>
    
    /**
     * Observes whether Bluetooth scanning is active
     */
    fun observeIsScanning(): Flow<Boolean>
    
    /**
     * Observes the currently connected Bluetooth device
     */
    fun observeConnectedDevice(): Flow<BluetoothDevice?>
    
    /**
     * Checks if Bluetooth is enabled on the device
     */
    fun isBluetoothEnabled(): Boolean
    
    /**
     * Checks if Bluetooth is supported on the device
     */
    fun isBluetoothSupported(): Boolean
    
    /**
     * Checks if the app has necessary Bluetooth permissions
     */
    fun hasBluetoothPermissions(): Boolean
    
    /**
     * Starts scanning for Bluetooth devices
     */
    suspend fun startScanning()
    
    /**
     * Stops scanning for Bluetooth devices
     */
    suspend fun stopScanning()
    
    /**
     * Connects to a specific Bluetooth device
     */
    suspend fun connectToDevice(device: BluetoothDevice)
    
    /**
     * Disconnects from the currently connected device
     */
    suspend fun disconnectDevice()
}
