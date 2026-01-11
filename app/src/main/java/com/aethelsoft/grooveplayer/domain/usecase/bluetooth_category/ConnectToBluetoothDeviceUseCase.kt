package com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category

import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * UseCase for connecting to a Bluetooth device.
 */
class ConnectToBluetoothDeviceUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke(device: BluetoothDevice) {
        bluetoothRepository.connectToDevice(device)
    }
}
