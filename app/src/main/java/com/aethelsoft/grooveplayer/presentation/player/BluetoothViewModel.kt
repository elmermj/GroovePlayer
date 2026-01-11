package com.aethelsoft.grooveplayer.presentation.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.CheckBluetoothStateUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ConnectToBluetoothDeviceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.DisconnectBluetoothDeviceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ObserveAvailableDevicesUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ObserveConnectedDeviceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ObserveIsScanningUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.StartBluetoothScanUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.StopBluetoothScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing Bluetooth device connections.
 * Follows MVVM and Clean Architecture by only using UseCases.
 */
@HiltViewModel
class BluetoothViewModel @Inject constructor(
    application: Application,
    private val observeAvailableDevicesUseCase: ObserveAvailableDevicesUseCase,
    private val observeIsScanningUseCase: ObserveIsScanningUseCase,
    private val observeConnectedDeviceUseCase: ObserveConnectedDeviceUseCase,
    private val checkBluetoothStateUseCase: CheckBluetoothStateUseCase,
    private val startBluetoothScanUseCase: StartBluetoothScanUseCase,
    private val stopBluetoothScanUseCase: StopBluetoothScanUseCase,
    private val connectToBluetoothDeviceUseCase: ConnectToBluetoothDeviceUseCase,
    private val disconnectBluetoothDeviceUseCase: DisconnectBluetoothDeviceUseCase
) : AndroidViewModel(application) {

    val availableDevices: StateFlow<List<BluetoothDevice>> = 
        observeAvailableDevicesUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    
    val isScanning: StateFlow<Boolean> = 
        observeIsScanningUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val connectedDevice: StateFlow<BluetoothDevice?> = 
        observeConnectedDeviceUseCase()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun isBluetoothEnabled(): Boolean = checkBluetoothStateUseCase.isBluetoothEnabled()
    
    fun isBluetoothSupported(): Boolean = checkBluetoothStateUseCase.isBluetoothSupported()
    
    fun hasBluetoothPermissions(): Boolean = checkBluetoothStateUseCase.hasBluetoothPermissions()

    fun startScanning() {
        viewModelScope.launch {
            startBluetoothScanUseCase()
        }
    }

    fun stopScanning() {
        viewModelScope.launch {
            stopBluetoothScanUseCase()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            connectToBluetoothDeviceUseCase(device)
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            disconnectBluetoothDeviceUseCase()
        }
    }
}
