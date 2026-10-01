package com.aethelworks.grooveplayer.domain.usecase.bluetooth_category

import com.aethelworks.grooveplayer.domain.model.BluetoothDevice
import com.aethelworks.grooveplayer.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for observing the currently connected Bluetooth device.
 */
class ObserveConnectedDeviceUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    operator fun invoke(): Flow<BluetoothDevice?> {
        return bluetoothRepository.observeConnectedDevice()
    }
}
