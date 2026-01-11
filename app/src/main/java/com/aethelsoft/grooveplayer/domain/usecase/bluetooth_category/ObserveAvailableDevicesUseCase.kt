package com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category

import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
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
