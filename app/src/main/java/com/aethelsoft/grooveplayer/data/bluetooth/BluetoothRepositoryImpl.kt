package com.aethelsoft.grooveplayer.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.aethelsoft.grooveplayer.data.bluetooth.mapper.BluetoothDeviceMapper
import com.aethelsoft.grooveplayer.data.bluetooth.model.BluetoothDeviceData
import com.aethelsoft.grooveplayer.domain.model.BluetoothDevice
import com.aethelsoft.grooveplayer.domain.repository.BluetoothRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager

@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    private val context: Context
) : BluetoothRepository {

    companion object {
        private const val TAG = "BluetoothRepoImpl"
    }

    private val bluetoothManager: AndroidBluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _availableDevices = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    private val _connectedDevice = MutableStateFlow<BluetoothDeviceData?>(null)
    private var discoveryStartTime: Long = 0
    private var devicesFoundDuringDiscovery = 0

    private var bluetoothA2dp: BluetoothProfile? = null
    private var bluetoothHeadset: BluetoothHeadset? = null
    private var bluetoothProfileServiceListener: BluetoothProfile.ServiceListener? = null
    private var scanReceiver: BroadcastReceiver? = null
    private var connectionReceiver: BroadcastReceiver? = null

    init {
        Log.d(TAG, "BluetoothRepositoryImpl initializing...")
        setupBluetoothProfile()
        registerReceivers()
        checkCurrentConnection()
        Log.d(TAG, "BluetoothRepositoryImpl initialized")
    }

    // ------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------

    override fun observeAvailableDevices(): Flow<List<BluetoothDevice>> =
        _availableDevices.map { BluetoothDeviceMapper.toDomainList(it) }

    override fun observeIsScanning(): Flow<Boolean> = _isScanning.asStateFlow()

    override fun observeConnectedDevice(): Flow<BluetoothDevice?> =
        _connectedDevice.map { it?.let { BluetoothDeviceMapper.toDomain(it) } }

    override fun isBluetoothEnabled(): Boolean =
        bluetoothAdapter?.isEnabled == true

    override fun isBluetoothSupported(): Boolean =
        bluetoothAdapter != null

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

    // ------------------------------------------------------------------------
    // Scanning
    // ------------------------------------------------------------------------

    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ]
    )
    override suspend fun startScanning() {
        Log.d(TAG, "startScanning() called")
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled")
            return
        }
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "Bluetooth permissions not granted")
            return
        }
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth adapter is null")
            return
        }

        Log.d(TAG, "Starting Bluetooth scan...")
        _isScanning.value = true
        _availableDevices.value = emptyList()

        // ---- Bonded devices
        try {
            val connectedAddress = _connectedDevice.value?.address
            val allBondedDevices = bluetoothAdapter.bondedDevices
            Log.d(TAG, "Found ${allBondedDevices.size} bonded device(s)")
            
            // Log ALL bonded devices first
            allBondedDevices.forEach { device ->
                val intent = Intent().apply {
                    putExtra(AndroidBluetoothDevice.EXTRA_DEVICE, device)
                    putExtra(AndroidBluetoothDevice.EXTRA_NAME, device.name)
                }
                logDiscoveredDevice(device, intent)
            }
            
            // Then filter to audio devices only and deduplicate TWS earbuds
            val audioDevices = allBondedDevices
                .filter { isAudioDevice(it) }
                .map { device ->
                    BluetoothDeviceData(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        isConnected = device.address == connectedAddress,
                        device = device
                    )
                }
            
            // Deduplicate devices with the same name (e.g., TWS earbuds)
            val deduplicatedDevices = deduplicateDevicesByName(audioDevices, connectedAddress)
            
            Log.d(TAG, "Filtered to ${audioDevices.size} audio device(s), deduplicated to ${deduplicatedDevices.size} device(s)")
            _availableDevices.value = deduplicatedDevices
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException accessing bonded devices: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception accessing bonded devices: ${e.message}", e)
        }

        try {
            if (bluetoothAdapter.isDiscovering) {
                Log.d(TAG, "Cancelling existing discovery")
                bluetoothAdapter.cancelDiscovery()
                // Wait a bit for cancellation to complete
                kotlinx.coroutines.delay(500)
            }
            val discoveryStarted = bluetoothAdapter.startDiscovery()
            Log.d(TAG, "startDiscovery() called, returned: $discoveryStarted")
            if (!discoveryStarted) {
                Log.w(TAG, "Failed to start Bluetooth discovery - may be restricted on Android 12+")
                _isScanning.value = false
            } else {
                discoveryStartTime = System.currentTimeMillis()
                devicesFoundDuringDiscovery = 0
                
                // On Android 12+, discovery may start but not send broadcasts for unpaired devices
                // Set a timeout to detect if discovery finishes silently
                CoroutineScope(Dispatchers.IO).launch {
                    delay(12000) // Typical discovery takes 10-12 seconds
                    try {
                        if (_isScanning.value && !bluetoothAdapter.isDiscovering) {
                            val duration = System.currentTimeMillis() - discoveryStartTime
                            if (devicesFoundDuringDiscovery == 0) {
                                Log.w(TAG, "Discovery finished after ${duration}ms but no ACTION_FOUND broadcasts received. " +
                                        "On Android 12+ (API 31+), classic Bluetooth discovery (startDiscovery()) is heavily restricted. " +
                                        "The system may only send broadcasts for already-paired devices. " +
                                        "Unpaired devices may not be discoverable via this method. " +
                                        "Only ${_availableDevices.value.size} paired audio device(s) are shown.")
                            } else {
                                Log.d(TAG, "Discovery finished after ${duration}ms, found $devicesFoundDuringDiscovery new device(s)")
                            }
                            _isScanning.value = false
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException checking discovery state: ${e.message}")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException when starting discovery: ${e.message}")
            _isScanning.value = false
        } catch (e: Exception) {
            Log.e(TAG, "Exception when starting discovery: ${e.message}", e)
            _isScanning.value = false
        }
    }

    override suspend fun stopScanning() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return

        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (_: SecurityException) {
        }

        _isScanning.value = false
    }

    // ------------------------------------------------------------------------
    // Receivers
    // ------------------------------------------------------------------------

    private fun registerReceivers() {
        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.d(TAG, "ScanReceiver received action: ${intent.action}")
                when (intent.action) {
                    AndroidBluetoothDevice.ACTION_FOUND -> {
                        val device =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    AndroidBluetoothDevice.EXTRA_DEVICE,
                                    AndroidBluetoothDevice::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                            }

                        if (device == null) {
                            Log.w(TAG, "ACTION_FOUND received but device is null")
                            return
                        }

                        // Log ALL discovered devices first, before filtering
                        logDiscoveredDevice(device, intent)
                        devicesFoundDuringDiscovery++

                        // Then filter and add only audio devices
                        if (!isAudioDevice(device)) {
                            Log.d(TAG, "Skipping non-audio device: ${device.address} (${device.name})")
                            return
                        }

                        addDiscoveredDevice(device, intent)
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        val duration = if (discoveryStartTime > 0) {
                            System.currentTimeMillis() - discoveryStartTime
                        } else 0
                        if (devicesFoundDuringDiscovery == 0) {
                            Log.w(TAG, "Bluetooth discovery finished after ${duration}ms. No new unpaired devices found. " +
                                    "On Android 12+, classic Bluetooth discovery may only show paired devices. " +
                                    "Currently showing ${_availableDevices.value.size} paired audio device(s).")
                        } else {
                            Log.d(TAG, "Bluetooth discovery finished after ${duration}ms. Found $devicesFoundDuringDiscovery new device(s)")
                        }
                        _isScanning.value = false
                        discoveryStartTime = 0
                        devicesFoundDuringDiscovery = 0
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Log.d(TAG, "Bluetooth discovery started")
                    }
                }
            }
        }

        connectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "android.bluetooth_category.a2dp.profile.action.CONNECTION_STATE_CHANGED" -> {
                        val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                        val device =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    AndroidBluetoothDevice.EXTRA_DEVICE,
                                    AndroidBluetoothDevice::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                            }

                        when (state) {
                            BluetoothProfile.STATE_CONNECTED ->
                                device?.let { handleDeviceConnected(it) }

                            BluetoothProfile.STATE_DISCONNECTED ->
                                device?.let {
                                    if (_connectedDevice.value?.address == it.address) {
                                        handleDeviceDisconnected()
                                    }
                                }
                        }
                    }

                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
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
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        }

        val connectionFilter = IntentFilter().apply {
            addAction("android.bluetooth_category.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // On Android 12+, use RECEIVER_NOT_EXPORTED for security
                // Note: System Bluetooth broadcasts may still work even with NOT_EXPORTED
                context.registerReceiver(
                    scanReceiver,
                    scanFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
                context.registerReceiver(
                    connectionReceiver,
                    connectionFilter,
                    Context.RECEIVER_NOT_EXPORTED
                )
                val actions = listOf(
                    AndroidBluetoothDevice.ACTION_FOUND,
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED
                ).joinToString()
                Log.d(TAG, "Receivers registered (Android 12+) with actions: $actions")
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(scanReceiver, scanFilter)

                @Suppress("DEPRECATION")
                context.registerReceiver(connectionReceiver, connectionFilter)
                val actions = listOf(
                    AndroidBluetoothDevice.ACTION_FOUND,
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED,
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED
                ).joinToString()
                Log.d(TAG, "Receivers registered (Android < 12) with actions: $actions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register receivers: ${e.message}", e)
        }
    }

    // ------------------------------------------------------------------------
    // Device deduplication (TWS earbuds)
    // ------------------------------------------------------------------------

    /**
     * Deduplicates devices with the same name (e.g., TWS earbuds that appear as two separate devices).
     * When multiple devices have the same name, keeps the connected one if any, otherwise keeps the first one.
     */
    private fun deduplicateDevicesByName(
        devices: List<BluetoothDeviceData>,
        connectedAddress: String?
    ): List<BluetoothDeviceData> {
        val groupedByName = devices.groupBy { it.name.lowercase().trim() }
        
        return groupedByName.values.map { devicesWithSameName ->
            if (devicesWithSameName.size > 1) {
                // Multiple devices with the same name (likely TWS earbuds)
                val deviceNames = devicesWithSameName.joinToString(", ") { "${it.name} (${it.address})" }
                Log.d(TAG, "Deduplicating ${devicesWithSameName.size} devices with name '${devicesWithSameName.first().name}': $deviceNames")
                
                // If any device with this name is connected, prefer that one
                val connected = devicesWithSameName.firstOrNull { it.address == connectedAddress }
                if (connected != null) {
                    Log.d(TAG, "Keeping connected device: ${connected.address}")
                    connected
                } else {
                    // Otherwise, keep the first one (prefer the one that appears first in the list)
                    val kept = devicesWithSameName.first()
                    Log.d(TAG, "Keeping first device: ${kept.address}")
                    kept
                }
            } else {
                // Only one device with this name, keep it as is
                devicesWithSameName.first()
            }
        }
    }

    // ------------------------------------------------------------------------
    // Audio device filter (CRITICAL)
    // ------------------------------------------------------------------------

    private fun isAudioDevice(device: AndroidBluetoothDevice): Boolean {
        val btClass = device.bluetoothClass ?: return false

        // Audio output devices (headphones, speakers, car audio)
        if (btClass.hasService(BluetoothClass.Service.RENDER)) {
            return true
        }

        // Audio input devices (headsets with mic)
        if (btClass.hasService(BluetoothClass.Service.CAPTURE)) {
            return true
        }
        Log.d(
            TAG,
            "AudioCheck addr=${device.address} " +
                    "render=${btClass.hasService(BluetoothClass.Service.RENDER)} " +
                    "capture=${btClass.hasService(BluetoothClass.Service.CAPTURE)} " +
                    "major=${btClass.majorDeviceClass}"
        )
        return false
    }

    // ------------------------------------------------------------------------
    // Discovery helpers
    // ------------------------------------------------------------------------

    private fun addDiscoveredDevice(device: AndroidBluetoothDevice, intent: Intent) {
        if (!hasBluetoothPermissions()) return

        val advertisedName = intent.getStringExtra(AndroidBluetoothDevice.EXTRA_NAME)

        val resolvedName = when {
            !advertisedName.isNullOrBlank() -> advertisedName
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !device.alias.isNullOrBlank() -> device.alias
            !device.name.isNullOrBlank() -> device.name
            else -> "Unknown Device"
        }

        val deviceInfo = BluetoothDeviceData(
            name = if(resolvedName.isNullOrEmpty()) "Unknown Device" else resolvedName,
            address = device.address,
            isConnected = device.address == _connectedDevice.value?.address,
            device = device
        )

        val list = _availableDevices.value.toMutableList()
        if (list.none { it.address == device.address }) {
            list.add(deviceInfo)
            // Deduplicate after adding to handle TWS earbuds
            val connectedAddress = _connectedDevice.value?.address
            val deduplicated = deduplicateDevicesByName(list, connectedAddress)
            _availableDevices.value = deduplicated
        }
    }

    private fun logDiscoveredDevice(device: AndroidBluetoothDevice, intent: Intent) {
        Log.d(
            TAG,
            """
            ───── Bluetooth Audio Device Found ─────
            Address       : ${device.address}
            Name          : ${device.name}
            Alias         : ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) device.alias else null}
            Advertised    : ${intent.getStringExtra(AndroidBluetoothDevice.EXTRA_NAME)}
            RSSI          : ${intent.getShortExtra(AndroidBluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)}
            Bond State    : ${device.bondState}
            Device Class  : ${device.bluetoothClass?.deviceClass}
            Major Class   : ${device.bluetoothClass?.majorDeviceClass}
            Type          : ${device.type}
            UUIDs         : ${device.uuids?.joinToString()}
            ────────────────────────────────────────
            """.trimIndent()
        )
    }

    // ------------------------------------------------------------------------
    // Connection handling (unchanged)
    // ------------------------------------------------------------------------

    private fun setupBluetoothProfile() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return

        bluetoothProfileServiceListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                when (profile) {
                    BluetoothProfile.A2DP -> bluetoothA2dp = proxy
                    BluetoothProfile.HEADSET -> bluetoothHeadset = proxy as BluetoothHeadset
                }
                checkCurrentConnection()
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    bluetoothA2dp = null
                    _connectedDevice.value = null
                }
            }
        }

        bluetoothAdapter.getProfileProxy(context, bluetoothProfileServiceListener, BluetoothProfile.A2DP)
        bluetoothAdapter.getProfileProxy(context, bluetoothProfileServiceListener, BluetoothProfile.HEADSET)
    }

    private fun checkCurrentConnection() {
        bluetoothA2dp?.connectedDevices?.firstOrNull()?.let {
            _connectedDevice.value = BluetoothDeviceData(
                name = it.name ?: "Unknown Device",
                address = it.address,
                isConnected = true,
                device = it
            )
        }
    }

    private fun handleDeviceConnected(device: AndroidBluetoothDevice) {
        val deviceName = device.name ?: "Unknown Device"

        val connectedData = BluetoothDeviceData(
            name = deviceName,
            address = device.address,
            isConnected = true,
            device = device
        )

        val list = _availableDevices.value.toMutableList()
        val index = list.indexOfFirst { it.address == device.address }

        if (index >= 0) {
            // Update existing entry
            list[index] = connectedData
        } else {
            // Edge case: device connected without being scanned
            list.add(connectedData)
        }

        _availableDevices.value = list
        _connectedDevice.value = connectedData
    }

    private fun handleDeviceDisconnected() {
        val connected = _connectedDevice.value ?: return

        val list = _availableDevices.value.toMutableList()
        val index = list.indexOfFirst { it.address == connected.address }

        if (index >= 0) {
            list[index] = list[index].copy(isConnected = false)
        }

        _availableDevices.value = list
        _connectedDevice.value = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermissions()) return

        val deviceData =
            _availableDevices.value.firstOrNull { it.address == device.address }
                ?: return

        val androidDevice = deviceData.device

        try {
            when (androidDevice?.bondState) {
                AndroidBluetoothDevice.BOND_NONE -> {
                    // Trigger pairing
                    androidDevice.createBond()
                }

                AndroidBluetoothDevice.BOND_BONDED -> {
                    // Try A2DP connect (reflection on Android 11+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && bluetoothA2dp != null) {
                        try {
                            val connectMethod =
                                bluetoothA2dp!!::class.java.getMethod(
                                    "connect",
                                    AndroidBluetoothDevice::class.java
                                )
                            connectMethod.invoke(bluetoothA2dp, androidDevice)
                        } catch (_: Exception) {
                            // System may auto-connect
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun disconnectDevice() {
        if (!hasBluetoothPermissions()) return

        val androidDevice = _connectedDevice.value?.device ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && bluetoothA2dp != null) {
                try {
                    val disconnectMethod =
                        bluetoothA2dp!!::class.java.getMethod(
                            "disconnect",
                            AndroidBluetoothDevice::class.java
                        )
                    disconnectMethod.invoke(bluetoothA2dp, androidDevice)
                } catch (_: Exception) {
                    handleDeviceDisconnected()
                }
            } else {
                handleDeviceDisconnected()
            }
        } catch (_: SecurityException) {
            handleDeviceDisconnected()
        }
    }

    fun cleanup() {
        scanReceiver?.let { context.unregisterReceiver(it) }
        connectionReceiver?.let { context.unregisterReceiver(it) }
    }
}
