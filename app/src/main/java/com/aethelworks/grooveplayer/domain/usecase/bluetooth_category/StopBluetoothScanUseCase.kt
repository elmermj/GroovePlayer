package com.aethelworks.grooveplayer.domain.usecase.bluetooth_category

import com.aethelworks.grooveplayer.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * UseCase for stopping Bluetooth device scanning.
 */
class StopBluetoothScanUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke() {
        bluetoothRepository.stopScanning()
    }
}
