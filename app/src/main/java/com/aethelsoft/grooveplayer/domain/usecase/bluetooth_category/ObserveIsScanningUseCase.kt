package com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category

import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * UseCase for observing Bluetooth scanning state.
 */
class ObserveIsScanningUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return bluetoothRepository.observeIsScanning()
    }
}
