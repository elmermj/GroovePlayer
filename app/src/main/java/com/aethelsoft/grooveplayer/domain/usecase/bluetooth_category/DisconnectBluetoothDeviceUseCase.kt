package com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category

import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * UseCase for disconnecting from the currently connected Bluetooth device.
 */
class DisconnectBluetoothDeviceUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke() {
        bluetoothRepository.disconnectDevice()
    }
}
