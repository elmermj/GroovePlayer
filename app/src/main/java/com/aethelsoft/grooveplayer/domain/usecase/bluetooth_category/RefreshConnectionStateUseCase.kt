package com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category

import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import javax.inject.Inject

/**
 * UseCase for refreshing the current Bluetooth connection state.
 * Useful for detecting connections made through Android settings or other apps.
 */
class RefreshConnectionStateUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke() {
        bluetoothRepository.refreshConnectionState()
    }
}
