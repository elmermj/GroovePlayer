package com.aethelsoft.grooveplayer.presentation.player

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.CheckBluetoothStateUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ConnectToBluetoothDeviceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.DisconnectBluetoothDeviceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ObserveAvailableDevicesUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ObserveConnectedDeviceUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.ObserveIsScanningUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.RefreshConnectionStateUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.StartBluetoothScanUseCase
import com.aethelsoft.grooveplayer.domain.usecase.bluetooth_category.StopBluetoothScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val disconnectBluetoothDeviceUseCase: DisconnectBluetoothDeviceUseCase,
    private val refreshConnectionStateUseCase: RefreshConnectionStateUseCase
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

    private val _connectingDeviceAddress = MutableStateFlow<String?>(null)
    val connectingDeviceAddress: StateFlow<String?> = _connectingDeviceAddress.asStateFlow()

    private val _connectionSuccessDisplay = MutableStateFlow(false)
    val connectionSuccessDisplay: StateFlow<Boolean> = _connectionSuccessDisplay.asStateFlow()

    private val _connectionFailedDisplay = MutableStateFlow(false)
    val connectionFailedDisplay: StateFlow<Boolean> = _connectionFailedDisplay.asStateFlow()

    private var connectionSuccessJob: Job? = null
    private var connectionFailedJob: Job? = null
    private var connectingTimeoutJob: Job? = null
    private var periodicRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            startBluetoothScanUseCase()
            observeConnectedDeviceUseCase().collect { connected ->
                val connecting = _connectingDeviceAddress.value
                
                Log.d("BluetoothViewModel", "Connected device changed: ${connected?.address}, connecting: $connecting, timeoutJob active: ${connectingTimeoutJob?.isActive}")
                
                // If we have an active timeout job, check if we should cancel it
                if (connectingTimeoutJob != null && connecting != null) {
                    if (connected != null && connected.address == connecting) {
                        // Success! The device we're connecting to is now connected
                        Log.d("BluetoothViewModel", "Connection successful for ${connected.address} - cancelling timeout")
                        _connectingDeviceAddress.value = null
                        connectingTimeoutJob?.cancel()
                        connectingTimeoutJob = null
                        connectionFailedJob?.cancel()
                        _connectionFailedDisplay.value = false
                        _connectionSuccessDisplay.value = true
                        connectionSuccessJob?.cancel()
                        connectionSuccessJob = viewModelScope.launch {
                            delay(1200)
                            _connectionSuccessDisplay.value = false
                        }
                    }
                    // If connected is null or different device, keep timeout running
                }
            }
        }

        // Keep UI in sync with external/automatic connections (e.g., TWS auto-connect).
        // Broadcasts can be missed; a lightweight periodic refresh makes the UI reliable.
        periodicRefreshJob?.cancel()
        periodicRefreshJob = viewModelScope.launch {
            while (true) {
                try {
                    val hasPerms = checkBluetoothStateUseCase.hasBluetoothPermissions()
                    val enabled = checkBluetoothStateUseCase.isBluetoothEnabled()
                    if (hasPerms && enabled) {
                        refreshConnectionStateUseCase()
                        delay(2000)
                    } else {
                        delay(5000)
                    }
                } catch (_: Exception) {
                    delay(5000)
                }
            }
        }
    }
    
    fun refreshConnectionState() {
        viewModelScope.launch {
            refreshConnectionStateUseCase()
        }
    }

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
        _connectingDeviceAddress.value = device.address
        connectingTimeoutJob?.cancel()
        connectionFailedJob?.cancel()
        _connectionFailedDisplay.value = false
        _connectionSuccessDisplay.value = false
        
        connectingTimeoutJob = viewModelScope.launch {
            delay(20000)
            // Only show failure if we're still trying to connect this device AND it's not connected
            val stillConnecting = _connectingDeviceAddress.value == device.address
            val isConnected = connectedDevice.value?.address == device.address
            
            if (stillConnecting && !isConnected) {
                Log.w("BluetoothViewModel", "Connection timeout for ${device.address}")
                _connectingDeviceAddress.value = null
                _connectionSuccessDisplay.value = false
                _connectionFailedDisplay.value = true
                connectionFailedJob?.cancel()
                connectionFailedJob = viewModelScope.launch {
                    delay(1200)
                    _connectionFailedDisplay.value = false
                }
            } else if (isConnected) {
                Log.d("BluetoothViewModel", "Connection succeeded before timeout for ${device.address}")
                _connectingDeviceAddress.value = null
            }
        }
        viewModelScope.launch {
            connectToBluetoothDeviceUseCase(device)
        }
    }

    fun disconnectDevice() {
        _connectingDeviceAddress.value = null
        _connectionSuccessDisplay.value = false
        _connectionFailedDisplay.value = false
        connectingTimeoutJob?.cancel()
        connectionSuccessJob?.cancel()
        connectionFailedJob?.cancel()
        viewModelScope.launch {
            disconnectBluetoothDeviceUseCase()
        }
    }
}

/* ─────────────────────── CONN STATE ENUMS ─────────────────────── */
enum class BTConnectionState {
    IDLE,
    FAILED,
    SUCCESS,
}
