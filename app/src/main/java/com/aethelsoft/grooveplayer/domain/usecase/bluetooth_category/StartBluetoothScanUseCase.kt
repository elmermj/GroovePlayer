package com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category

import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * UseCase for starting Bluetooth device scanning.
 */
class StartBluetoothScanUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke() {
        bluetoothRepository.startScanning()
    }
}
