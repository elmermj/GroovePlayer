package com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category

import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * UseCase for checking Bluetooth state (enabled, supported, permissions).
 */
class CheckBluetoothStateUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    fun isBluetoothEnabled(): Boolean {
        return bluetoothRepository.isBluetoothEnabled()
    }
    
    fun isBluetoothSupported(): Boolean {
        return bluetoothRepository.isBluetoothSupported()
    }
    
    fun hasBluetoothPermissions(): Boolean {
        return bluetoothRepository.hasBluetoothPermissions()
    }
}
