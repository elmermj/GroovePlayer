package com.aethelsoft.grooveplayer.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.aethelsoft.grooveplayer.data.bluetooth.mapper.BluetoothDeviceMapper
import com.aethelsoft.grooveplayer.data.bluetooth.model.BluetoothDeviceData
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager

@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    private val context: Context
) : BluetoothRepository {
    
    private val bluetoothManager: AndroidBluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    
    private val _availableDevices = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    private val _connectedDevice = MutableStateFlow<BluetoothDeviceData?>(null)

    private var bluetoothA2dp: BluetoothProfile? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothProfileServiceListener: BluetoothProfile.ServiceListener? = null
    private var scanReceiver: BroadcastReceiver? = null
    private var connectionReceiver: BroadcastReceiver? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        setupBluetoothProfile()
        registerReceivers()
        checkCurrentConnection()
    }

    
    override fun observeAvailableDevices(): Flow<List<BluetoothDevice>> {
        return _availableDevices.map { BluetoothDeviceMapper.toDomainList(it) }
    }

    override fun observeIsScanning(): Flow<Boolean> = _isScanning.asStateFlow()

    override fun observeConnectedDevice(): Flow<BluetoothDevice?> {
        return _connectedDevice.map { it?.let { BluetoothDeviceMapper.toDomain(it) } }
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    override fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    override suspend fun startScanning() {
        if (!isBluetoothEnabled() || !hasBluetoothPermissions() || bluetoothAdapter == null) {
            return
        }

        _isScanning.value = true
        _availableDevices.value = emptyList()

        // First, get bonded (paired) devices
        try {
            val bondedDevices = bluetoothAdapter.bondedDevices.map { device ->
                val deviceName = try {
                    device.name ?: "Unknown Device"
                } catch (e: SecurityException) {
                    "Unknown Device"
                }
                BluetoothDeviceData(
                    name = deviceName,
                    address = device.address,
                    isConnected = false,
                    device = device
                )
            }
            _availableDevices.value = bondedDevices
        } catch (e: SecurityException) {
            // Permission not granted
        }

        // Then start discovery for new devices
        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        } catch (e: SecurityException) {
            _isScanning.value = false
        }
    }

    override suspend fun stopScanning() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return

        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
        _isScanning.value = false
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermissions()) return

        // Find the corresponding data model with Android device reference
        val deviceData = _availableDevices.value.firstOrNull { it.address == device.address }
        val androidDevice = deviceData?.device ?: return

        try {
            // For A2DP connection, we need to use reflection on Android 11+
            // For older versions, initiate pairing if not already paired
            if (androidDevice.bondState == AndroidBluetoothDevice.BOND_NONE) {
                // Device is not paired, initiate pairing
                androidDevice.createBond()
            } else if (androidDevice.bondState == AndroidBluetoothDevice.BOND_BONDED) {
                // Device is paired, try to connect via A2DP using reflection
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && bluetoothA2dp != null) {
                    try {
                        val connectMethod = bluetoothA2dp!!::class.java.getMethod("connect", AndroidBluetoothDevice::class.java)
                        connectMethod.invoke(bluetoothA2dp, androidDevice)
                    } catch (e: Exception) {
                        // Method not available or failed, device will connect automatically
                    }
                }
                // On older Android versions, A2DP connection happens automatically when device is paired
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            // Connection failed
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun disconnectDevice() {
        if (!hasBluetoothPermissions()) return

        val connectedDevice = _connectedDevice.value?.device ?: return

        try {
            // Disconnect A2DP device using reflection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && bluetoothA2dp != null) {
                try {
                    val disconnectMethod = bluetoothA2dp!!::class.java.getMethod("disconnect", AndroidBluetoothDevice::class.java)
                    disconnectMethod.invoke(bluetoothA2dp, connectedDevice)
                } catch (e: Exception) {
                    // Method not available or failed, update state anyway
                    handleDeviceDisconnected()
                }
            } else {
                // On older versions, disconnection is handled by the system
                // We just update our state
                handleDeviceDisconnected()
            }
        } catch (e: SecurityException) {
            // Permission not granted
            handleDeviceDisconnected()
        } catch (e: Exception) {
            // Disconnection failed, but update state anyway
            handleDeviceDisconnected()
        }
    }

    // Internal implementation methods
    private fun setupBluetoothProfile() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return

        bluetoothProfileServiceListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                when (profile) {
                    BluetoothProfile.A2DP -> {
                        bluetoothA2dp = proxy
                        checkCurrentConnection()
                    }
                    BluetoothProfile.HEADSET -> {
                        bluetoothHeadset = proxy as BluetoothHeadset
                        checkCurrentConnection()
                    }
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                when (profile) {
                    BluetoothProfile.A2DP -> {
                        bluetoothA2dp = null
                        _connectedDevice.value = null
                    }
                    BluetoothProfile.HEADSET -> {
                        bluetoothHeadset = null
                    }
                }
            }
        }

        try {
            // Connect to A2DP profile for audio playback
            bluetoothAdapter.getProfileProxy(
                context,
                bluetoothProfileServiceListener,
                BluetoothProfile.A2DP
            )
            // Also connect to HEADSET profile for connection state monitoring
            bluetoothAdapter.getProfileProxy(
                context,
                bluetoothProfileServiceListener,
                BluetoothProfile.HEADSET
            )
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun registerReceivers() {
        // Receiver for device discovery
        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    AndroidBluetoothDevice.ACTION_FOUND -> {
                        val device: AndroidBluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE, AndroidBluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                        }
                        device?.let { addDiscoveredDevice(it) }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        _isScanning.value = false
                    }
                }
            }
        }

        // Receiver for connection state changes
        connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "android.bluetooth_category.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> {
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                        val device: AndroidBluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE, AndroidBluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                        }
                        
                        when (state) {
                            BluetoothProfile.STATE_CONNECTED -> {
                                device?.let { handleDeviceConnected(it) }
                            }
                            BluetoothProfile.STATE_DISCONNECTED -> {
                                device?.let { 
                                    if (_connectedDevice.value?.address == it.address) {
                                        handleDeviceDisconnected()
                                    }
                                }
                            }
                        }
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                        // Also monitor headset for additional connection info
                        val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                        if (state == BluetoothProfile.STATE_CONNECTED) {
                            checkCurrentConnection()
                        }
                    }
                }
            }
        }

        val scanFilter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        val connectionFilter = IntentFilter().apply {
            addAction("android.bluetooth_category.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }

        try {
            context.registerReceiver(scanReceiver, scanFilter)
            context.registerReceiver(connectionReceiver, connectionFilter)
        } catch (e: Exception) {
            // Registration failed
        }
    }

    private fun addDiscoveredDevice(device: AndroidBluetoothDevice) {
        if (!hasBluetoothPermissions()) return

        val deviceName = try {
            device.name ?: "Unknown Device"
        } catch (e: SecurityException) {
            "Unknown Device"
        }

        val deviceInfo = BluetoothDeviceData(
            name = deviceName,
            address = device.address,
            isConnected = false,
            device = device
        )

        val currentDevices = _availableDevices.value.toMutableList()
        if (!currentDevices.any { it.address == device.address }) {
            currentDevices.add(deviceInfo)
            _availableDevices.value = currentDevices
        }
    }

    private fun checkCurrentConnection() {
        if (!hasBluetoothPermissions()) return

        try {
            // Check A2DP connected devices (for audio playback)
            val a2dpConnectedDevices = bluetoothA2dp?.connectedDevices
            a2dpConnectedDevices?.firstOrNull()?.let { device ->
                val deviceName = try {
                    device.name ?: "Unknown Device"
                } catch (e: SecurityException) {
                    "Unknown Device"
                }
                _connectedDevice.value = BluetoothDeviceData(
                    name = deviceName,
                    address = device.address,
                    isConnected = true,
                    device = device
                )
                // Audio is automatically routed through A2DP when connected
            } ?: run {
                // If no A2DP device, check headset
                val headsetConnectedDevices = bluetoothHeadset?.connectedDevices
                headsetConnectedDevices?.firstOrNull()?.let { device ->
                    val deviceName = try {
                        device.name ?: "Unknown Device"
                    } catch (e: SecurityException) {
                        "Unknown Device"
                    }
                    _connectedDevice.value = BluetoothDeviceData(
                        name = deviceName,
                        address = device.address,
                        isConnected = true,
                        device = device
                    )
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun handleDeviceConnected(device: AndroidBluetoothDevice) {
        val deviceName = try {
            device.name ?: "Unknown Device"
        } catch (e: SecurityException) {
            "Unknown Device"
        }

        _connectedDevice.value = BluetoothDeviceData(
            name = deviceName,
            address = device.address,
            isConnected = true,
            device = device
        )

        // Update available devices list
        val currentDevices = _availableDevices.value.toMutableList()
        val index = currentDevices.indexOfFirst { it.address == device.address }
        if (index >= 0) {
            currentDevices[index] = _connectedDevice.value!!
        } else {
            currentDevices.add(_connectedDevice.value!!)
        }
        _availableDevices.value = currentDevices

        // Audio is automatically routed through A2DP when connected
        // No need to manually start SCO for music playback
    }

    private fun handleDeviceDisconnected() {
        _connectedDevice.value = null

        // Update available devices list
        val currentDevices = _availableDevices.value.toMutableList()
        val index = currentDevices.indexOfFirst { it.isConnected }
        if (index >= 0) {
            currentDevices[index] = currentDevices[index].copy(isConnected = false)
        }
        _availableDevices.value = currentDevices

        // Audio routing is automatically handled by the system
    }

    fun cleanup() {
        try {
            scanReceiver?.let { context.unregisterReceiver(it) }
            connectionReceiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {
            // Already unregistered
        }

        try {
            bluetoothA2dp?.let {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, it)
            }
            bluetoothHeadset?.let {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, it)
            }
        } catch (e: Exception) {
            // Cleanup failed
        }
    }
}
