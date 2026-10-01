package com.aethelworks.grooveplayer.domain.usecase.bluetooth_category

import com.aethelworks.grooveplayer.domain.model.BluetoothDevice
import com.aethelworks.grooveplayer.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for observing available Bluetooth devices.
 */
class ObserveAvailableDevicesUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    operator fun invoke(): Flow<List<BluetoothDevice>> {
        return bluetoothRepository.observeAvailableDevices()
    }
}
