package com.aethelsoft.grooveplayer.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothA2dp
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
import com.aethelsoft.grooveplayer.utils.helpers.BluetoothHelpers


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
    private var pendingConnectionAddress: String? = null // Track device we're trying to connect to

    init {
        Log.d(TAG, "BluetoothRepositoryImpl initializing...")
        setupBluetoothProfile()
        registerReceivers()
        if (hasBluetoothPermissions()) checkCurrentConnection()
        Log.d(TAG, "BluetoothRepositoryImpl initialized")
    }

    // ------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------

    override fun observeAvailableDevices(): Flow<List<BluetoothDevice>> =
        _availableDevices.map { BluetoothDeviceMapper.toDomainList(it) }

    override fun observeIsScanning(): Flow<Boolean> = _isScanning.asStateFlow()

    override fun observeConnectedDevice(): Flow<BluetoothDevice?> =
        _connectedDevice.asStateFlow().map { it?.let { BluetoothDeviceMapper.toDomain(it) } }

    override fun isBluetoothEnabled(): Boolean {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return false
        return try {
            bluetoothAdapter.isEnabled
        } catch (_: SecurityException) {
            false
        }
    }

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
                delay(500)
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
                        if (!hasBluetoothPermissions()) return
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

                        try {
                            logDiscoveredDevice(device, intent)
                            devicesFoundDuringDiscovery++
                            if (!isAudioDevice(device)) {
                                Log.d(TAG, "Skipping non-audio device: ${device.address} (${device.name})")
                                return
                            }
                            addDiscoveredDevice(device, intent)
                        } catch (_: SecurityException) { }
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
                if (!hasBluetoothPermissions()) return
                try {
                    when (intent.action) {
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
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

                            Log.d(TAG, "ACTION_CONNECTION_STATE_CHANGED: state=$state, device=${device?.address}")

                            when (state) {
                                BluetoothProfile.STATE_CONNECTED -> {
                                    device?.let { 
                                        Log.d(TAG, "A2DP connection established: ${it.address} (${it.name})")
                                        handleDeviceConnected(it) 
                                    } ?: run {
                                        // Device is null but state is connected - check current connection
                                        Log.d(TAG, "A2DP connected but device is null, checking current connection")
                                        checkCurrentConnection()
                                    }
                                }

                                BluetoothProfile.STATE_DISCONNECTED -> {
                                    device?.let {
                                        Log.d(TAG, "A2DP disconnection broadcast: ${it.address}")
                                        if (_connectedDevice.value?.address == it.address) {
                                            Log.d(TAG, "Disconnected device matches tracked device, calling handleDeviceDisconnected()")
                                            handleDeviceDisconnected()
                                        } else {
                                            // Device disconnected but wasn't tracked - refresh state
                                            Log.d(TAG, "Disconnected device ${it.address} doesn't match tracked device ${_connectedDevice.value?.address}, checking current connection")
                                            checkCurrentConnection()
                                        }
                                    } ?: run {
                                        // Device is null but state is disconnected - check current connection
                                        Log.d(TAG, "A2DP disconnected but device is null, checking current connection")
                                        checkCurrentConnection()
                                    }
                                }
                                
                                BluetoothProfile.STATE_CONNECTING -> {
                                    Log.d(TAG, "A2DP connecting: ${device?.address}")
                                }
                                
                                BluetoothProfile.STATE_DISCONNECTING -> {
                                    Log.d(TAG, "A2DP disconnecting: ${device?.address}")
                                }
                            }
                        }

                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                            if (state == BluetoothProfile.STATE_CONNECTED) {
                                checkCurrentConnection()
                            }
                        }

                        AndroidBluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
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
                            val bondState = intent.getIntExtra(AndroidBluetoothDevice.EXTRA_BOND_STATE, -1)
                            val previousBondState = intent.getIntExtra(AndroidBluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)

                            device?.let { d ->
                                Log.d(TAG, "ACTION_BOND_STATE_CHANGED: ${d.address}, state=$bondState (prev=$previousBondState)")
                                
                                when (bondState) {
                                    AndroidBluetoothDevice.BOND_BONDED -> {
                                        // Bonding completed successfully
                                        if (d.address == pendingConnectionAddress) {
                                            Log.d(TAG, "Bonding completed for ${d.address}, attempting A2DP connection")
                                            pendingConnectionAddress = null
                                            attemptA2dpConnection(d)
                                        }
                                    }
                                    AndroidBluetoothDevice.BOND_NONE -> {
                                        // Bonding failed or was removed
                                        if (d.address == pendingConnectionAddress && previousBondState == AndroidBluetoothDevice.BOND_BONDING) {
                                            Log.w(TAG, "Bonding failed for ${d.address}")
                                            pendingConnectionAddress = null
                                        }
                                    }
                                }
                            }
                        }

                        AndroidBluetoothDevice.ACTION_PAIRING_REQUEST -> {
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
                            val pairingVariant = intent.getIntExtra(AndroidBluetoothDevice.EXTRA_PAIRING_VARIANT, -1)
                            
                            device?.let { d ->
                                Log.d(TAG, "ACTION_PAIRING_REQUEST: ${d.address}, variant=$pairingVariant")
                                // For most devices, Android handles pairing automatically
                                // Some devices may require PIN entry, let the system handle it
                                // If needed, set PIN here: d.setPin(...) or d.setPairingConfirmation(true)
                            }
                        }

                        BluetoothAdapter.ACTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                            Log.d(TAG, "Bluetooth adapter state changed: $state")
                            when (state) {
                                BluetoothAdapter.STATE_ON -> {
                                    Log.d(TAG, "Bluetooth turned ON, checking current connection")
                                    // When Bluetooth is turned on, check for existing connections
                                    checkCurrentConnection()
                                }
                                BluetoothAdapter.STATE_OFF -> {
                                    Log.d(TAG, "Bluetooth turned OFF, clearing connection state")
                                    _connectedDevice.value = null
                                    _isScanning.value = false
                                }
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException in connectionReceiver: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in connectionReceiver: ${e.message}", e)
                }
            }
        }

        val scanFilter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        }

        val connectionFilter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AndroidBluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(AndroidBluetoothDevice.ACTION_PAIRING_REQUEST)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED) // Listen for Bluetooth on/off
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
                ContextCompat.registerReceiver(
                    context,
                    connectionReceiver,
                    connectionFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
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
        val groupedByName = devices.groupBy { BluetoothHelpers.normalizedNameKey(it.name) }
        
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
        if (!hasBluetoothPermissions()) return false
        return try {
            val btClass = device.bluetoothClass ?: return false
            if (btClass.hasService(BluetoothClass.Service.RENDER)) return true
            if (btClass.hasService(BluetoothClass.Service.CAPTURE)) return true
            Log.d(
                TAG,
                "AudioCheck addr=${device.address} " +
                        "render=${btClass.hasService(BluetoothClass.Service.RENDER)} " +
                        "capture=${btClass.hasService(BluetoothClass.Service.CAPTURE)} " +
                        "major=${btClass.majorDeviceClass}"
            )
            false
        } catch (_: SecurityException) {
            false
        }
    }

    // ------------------------------------------------------------------------
    // Discovery helpers
    // ------------------------------------------------------------------------

    private fun addDiscoveredDevice(device: AndroidBluetoothDevice, intent: Intent) {
        if (!hasBluetoothPermissions()) return
        try {
            val advertisedName = intent.getStringExtra(AndroidBluetoothDevice.EXTRA_NAME)
            val resolvedName = when {
                !advertisedName.isNullOrBlank() -> advertisedName
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !device.alias.isNullOrBlank() -> device.alias
                !device.name.isNullOrBlank() -> device.name
                else -> "Unknown Device"
            }
            val deviceInfo = BluetoothDeviceData(
                name = if (resolvedName.isNullOrEmpty()) "Unknown Device" else resolvedName,
                address = device.address,
                isConnected = device.address == _connectedDevice.value?.address,
                device = device
            )
            val list = _availableDevices.value.toMutableList()
            if (list.none { it.address == device.address }) {
                list.add(deviceInfo)
                val connectedAddress = _connectedDevice.value?.address
                val deduplicated = deduplicateDevicesByName(list, connectedAddress)
                _availableDevices.value = deduplicated
            }
        } catch (_: SecurityException) { }
    }

    private fun logDiscoveredDevice(device: AndroidBluetoothDevice, intent: Intent) {
        if (!hasBluetoothPermissions()) return
        try {
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
        } catch (_: SecurityException) { }
    }

    // ------------------------------------------------------------------------
    // Connection handling (unchanged)
    // ------------------------------------------------------------------------

    private fun setupBluetoothProfile() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return

        bluetoothProfileServiceListener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                when (profile) {
                    BluetoothProfile.A2DP -> {
                        bluetoothA2dp = proxy
                        Log.d(TAG, "A2DP profile proxy connected")
                        checkCurrentConnection()
                        // If we have a pending connection, attempt it now
                        pendingConnectionAddress?.let { address ->
                            try {
                                val device = bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == address }
                                device?.let { attemptA2dpConnection(it) }
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException getting device for pending connection: ${e.message}")
                            }
                        }
                    }
                    BluetoothProfile.HEADSET -> bluetoothHeadset = proxy as BluetoothHeadset
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) {
                    Log.d(TAG, "A2DP profile proxy disconnected")
                    bluetoothA2dp = null
                    _connectedDevice.value = null
                }
            }
        }

        try {
            bluetoothAdapter.getProfileProxy(context, bluetoothProfileServiceListener, BluetoothProfile.A2DP)
            bluetoothAdapter.getProfileProxy(context, bluetoothProfileServiceListener, BluetoothProfile.HEADSET)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException setting up Bluetooth profiles: ${e.message}")
        }
    }

    private fun checkCurrentConnection() {
        if (!hasBluetoothPermissions()) return
        try {
            val connected = bluetoothA2dp?.connectedDevices?.firstOrNull()
            if (connected != null) {
                val connectedData = BluetoothDeviceData(
                    name = connected.name ?: "Unknown Device",
                    address = connected.address,
                    isConnected = true,
                    device = connected
                )
                _connectedDevice.value = connectedData
                
                // Update available devices list to mark this device as connected
                val list = _availableDevices.value.toMutableList()
                val index = list.indexOfFirst { it.address == connected.address }
                if (index >= 0) {
                    list[index] = connectedData
                } else {
                    // Device not in list, add it
                    list.add(connectedData)
                }
                // Ensure TWS devices don't show as duplicates when the connected bud differs
                _availableDevices.value = deduplicateDevicesByName(list, connected.address)
                Log.d(TAG, "checkCurrentConnection: Found connected device ${connected.address} (${connected.name})")
            } else {
                // No device connected - clear connected device and update available devices
                val previousConnected = _connectedDevice.value
                _connectedDevice.value = null
                
                if (previousConnected != null) {
                    // Update available devices to mark previous device as disconnected
                    val list = _availableDevices.value.toMutableList()
                    val index = list.indexOfFirst { it.address == previousConnected.address }
                    if (index >= 0) {
                        list[index] = list[index].copy(isConnected = false)
                        _availableDevices.value = deduplicateDevicesByName(list, null)
                    }
                    Log.d(TAG, "checkCurrentConnection: No device connected (was ${previousConnected.address})")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in checkCurrentConnection: ${e.message}")
            _connectedDevice.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Exception in checkCurrentConnection: ${e.message}", e)
            _connectedDevice.value = null
        }
    }

    override suspend fun refreshConnectionState() {
        Log.d(TAG, "refreshConnectionState() called")
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "refreshConnectionState: no permissions")
            return
        }
        checkCurrentConnection()
    }

    private fun handleDeviceConnected(device: AndroidBluetoothDevice) {
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "handleDeviceConnected: no permissions")
            return
        }
        try {
            val deviceName = device.name ?: "Unknown Device"
            val connectedData = BluetoothDeviceData(
                name = deviceName,
                address = device.address,
                isConnected = true,
                device = device
            )
            Log.d(TAG, "handleDeviceConnected: Setting connected device to ${device.address} (${deviceName})")
            
            val list = _availableDevices.value.toMutableList()
            val index = list.indexOfFirst { it.address == device.address }
            if (index >= 0) {
                list[index] = connectedData
            } else {
                list.add(connectedData)
            }
            _availableDevices.value = deduplicateDevicesByName(list, device.address)
            
            // Update connected device - this should trigger the flow
            val previousConnected = _connectedDevice.value
            _connectedDevice.value = connectedData
            Log.d(TAG, "handleDeviceConnected: Updated _connectedDevice from ${previousConnected?.address} to ${connectedData.address}")
            
            // Stop scanning when device successfully connects
            if (_isScanning.value) {
                Log.d(TAG, "Device connected, stopping scan")
                CoroutineScope(Dispatchers.IO).launch {
                    stopScanning()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in handleDeviceConnected: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Exception in handleDeviceConnected: ${e.message}", e)
        }
    }

    private fun handleDeviceDisconnected() {
        val connected = _connectedDevice.value
        if (connected == null) {
            Log.d(TAG, "handleDeviceDisconnected: No device was connected")
            return
        }

        Log.d(TAG, "handleDeviceDisconnected: Disconnecting device ${connected.address} (${connected.name})")
        
        val list = _availableDevices.value.toMutableList()
        val index = list.indexOfFirst { it.address == connected.address }

        if (index >= 0) {
            list[index] = list[index].copy(isConnected = false)
        }

        _availableDevices.value = list
        
        // Update connected device - this should trigger the flow
        val previousConnected = _connectedDevice.value
        _connectedDevice.value = null
        Log.d(TAG, "handleDeviceDisconnected: Updated _connectedDevice from ${previousConnected?.address} to null")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connectToDevice(device: BluetoothDevice) {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) return

        val deviceData = _availableDevices.value.firstOrNull { it.address == device.address }
            ?: run {
                // Device not in list (e.g. from another screen); try bonded by address
                val bonded = try {
                    bluetoothAdapter!!.bondedDevices.firstOrNull { it.address == device.address }
                } catch (_: SecurityException) {
                    null
                }
                if (bonded != null) {
                    BluetoothDeviceData(
                        name = device.name,
                        address = device.address,
                        isConnected = false,
                        device = bonded
                    )
                } else {
                    Log.w(TAG, "connectToDevice: device not found in available or bonded list: ${device.address}")
                    return
                }
            }

        val androidDevice = deviceData.device
            ?: try {
                bluetoothAdapter.bondedDevices.firstOrNull { it.address == device.address }
            } catch (_: SecurityException) {
                null
            }
            ?: run {
                Log.w(TAG, "connectToDevice: no Android device for ${device.address}")
                return
            }

        try {
            when (androidDevice.bondState) {
                AndroidBluetoothDevice.BOND_NONE -> {
                    Log.d(TAG, "connectToDevice: device not bonded, starting bonding for ${androidDevice.address}")
                    pendingConnectionAddress = androidDevice.address
                    val bondResult = androidDevice.createBond()
                    Log.d(TAG, "createBond() returned: $bondResult")
                    if (!bondResult) {
                        Log.w(TAG, "createBond() failed for ${androidDevice.address}")
                        pendingConnectionAddress = null
                    }
                }
                AndroidBluetoothDevice.BOND_BONDING -> {
                    // Pairing in progress; connection will be attempted after ACTION_BOND_STATE_CHANGED
                    Log.d(TAG, "connectToDevice: bonding in progress for ${androidDevice.address}, waiting for completion")
                    pendingConnectionAddress = androidDevice.address
                }
                AndroidBluetoothDevice.BOND_BONDED -> {
                    Log.d(TAG, "connectToDevice: device already bonded, attempting A2DP connection for ${androidDevice.address}")
                    pendingConnectionAddress = null
                    // TWS earbuds can appear as two separate bonded devices with the same name.
                    // If we connect to the "wrong" side, connect() may fail. Try fallbacks.
                    if (bluetoothA2dp == null) {
                        attemptA2dpConnection(androidDevice)
                        return
                    }

                    val targetKey = BluetoothHelpers.normalizedNameKey(deviceData.name)
                    val candidates = try {
                        bluetoothAdapter.bondedDevices
                            .filter { isAudioDevice(it) }
                            .filter { BluetoothHelpers.normalizedNameKey(it.name ?: "") == targetKey }
                    } catch (_: SecurityException) {
                        emptyList()
                    }

                    val orderedCandidates = buildList {
                        add(androidDevice)
                        candidates.forEach { d ->
                            if (d.address != androidDevice.address) add(d)
                        }
                    }

                    // Try each candidate briefly until one becomes connected.
                    for (candidate in orderedCandidates) {
                        Log.d(TAG, "connectToDevice: attempting A2DP connect for candidate ${candidate.address} (${candidate.name})")
                        attemptA2dpConnection(candidate)
                        // Give the system a moment; some devices connect instantly, others take a second.
                        delay(2500)
                        val nowConnected = try {
                            bluetoothA2dp?.connectedDevices?.any { it.address == candidate.address } == true
                        } catch (_: SecurityException) {
                            false
                        }
                        if (nowConnected) {
                            Log.d(TAG, "connectToDevice: candidate connected ${candidate.address}")
                            checkCurrentConnection()
                            break
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in connectToDevice: ${e.message}")
            pendingConnectionAddress = null
        } catch (e: Exception) {
            Log.e(TAG, "Exception in connectToDevice: ${e.message}", e)
            pendingConnectionAddress = null
        }
    }

    /**
     * Attempts to connect to a device via A2DP profile.
     * This is called after bonding completes or if device is already bonded.
     */
    private fun attemptA2dpConnection(androidDevice: AndroidBluetoothDevice) {
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "attemptA2dpConnection: no permissions")
            return
        }
        
        if (bluetoothA2dp == null) {
            Log.w(TAG, "attemptA2dpConnection: A2DP proxy not ready, will retry when proxy connects")
            // Store pending connection - will be attempted when proxy is ready
            pendingConnectionAddress = androidDevice.address
            return
        }

        try {
            // Use reflection to call connect() (hidden API on all Android versions)
            val connectMethod = bluetoothA2dp!!::class.java.getMethod(
                "connect",
                AndroidBluetoothDevice::class.java
            )
            val result = connectMethod.invoke(bluetoothA2dp, androidDevice)
            Log.d(TAG, "A2DP connect() called for ${androidDevice.address}, result: $result")
            
            // After calling connect(), check connection state periodically
            // Sometimes the broadcast doesn't fire immediately or at all
            CoroutineScope(Dispatchers.IO).launch {
                // Wait a bit first - connection might be immediate
                delay(1000)
                
                var attempts = 0
                val maxAttempts = 18 // Check for up to 9 more seconds (18 * 500ms) after initial 1s delay
                while (attempts < maxAttempts) {
                    // Check if device is now connected
                    try {
                        val connected = bluetoothA2dp?.connectedDevices?.firstOrNull { 
                            it.address == androidDevice.address 
                        }
                        if (connected != null) {
                            Log.d(TAG, "Device ${androidDevice.address} is now connected (checked after ${1000 + attempts * 500}ms)")
                            handleDeviceConnected(connected)
                            return@launch
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "SecurityException checking connection state: ${e.message}")
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception checking connection state: ${e.message}")
                        break
                    }
                    
                    delay(500)
                    attempts++
                }
                if (attempts >= maxAttempts) {
                    Log.d(TAG, "Stopped checking connection state for ${androidDevice.address} after ${1000 + maxAttempts * 500}ms - connection may have failed or broadcast will handle it")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call A2DP connect() for ${androidDevice.address}: ${e.message}", e)
            // On some devices, the system may auto-connect
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun disconnectDevice() {
        if (!hasBluetoothPermissions()) {
            Log.w(TAG, "disconnectDevice: no permissions")
            return
        }

        // Clear any pending connection
        pendingConnectionAddress = null

        val androidDevice = _connectedDevice.value?.device
        if (androidDevice == null) {
            Log.d(TAG, "disconnectDevice: No device connected, clearing state anyway")
            handleDeviceDisconnected()
            return
        }

        Log.d(TAG, "disconnectDevice: Disconnecting ${androidDevice.address}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && bluetoothA2dp != null) {
                try {
                    val disconnectMethod =
                        bluetoothA2dp!!::class.java.getMethod(
                            "disconnect",
                            AndroidBluetoothDevice::class.java
                        )
                    val result = disconnectMethod.invoke(bluetoothA2dp, androidDevice)
                    Log.d(TAG, "A2DP disconnect() called for ${androidDevice.address}, result: $result")
                    
                    // After calling disconnect(), check connection state periodically
                    // Sometimes the broadcast doesn't fire immediately or at all
                    CoroutineScope(Dispatchers.IO).launch {
                        // Wait a bit first - disconnection might be immediate
                        delay(500)
                        
                        var attempts = 0
                        val maxAttempts = 10 // Check for up to 5 seconds (10 * 500ms) after initial 500ms delay
                        while (attempts < maxAttempts) {
                            // Check if device is now disconnected
                            try {
                                val stillConnected = bluetoothA2dp?.connectedDevices?.firstOrNull { 
                                    it.address == androidDevice.address 
                                }
                                if (stillConnected == null) {
                                    Log.d(TAG, "Device ${androidDevice.address} is now disconnected (checked after ${500 + attempts * 500}ms)")
                                    handleDeviceDisconnected()
                                    return@launch
                                }
                            } catch (e: SecurityException) {
                                Log.e(TAG, "SecurityException checking disconnection state: ${e.message}")
                                break
                            } catch (e: Exception) {
                                Log.e(TAG, "Exception checking disconnection state: ${e.message}")
                                break
                            }
                            
                            delay(500)
                            attempts++
                        }
                        if (attempts >= maxAttempts) {
                            Log.d(TAG, "Stopped checking disconnection state for ${androidDevice.address} after ${500 + maxAttempts * 500}ms - checking current connection")
                            // Final check - if no device is connected, update state
                            checkCurrentConnection()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to call A2DP disconnect(), handling as disconnected: ${e.message}")
                    handleDeviceDisconnected()
                }
            } else {
                handleDeviceDisconnected()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in disconnectDevice: ${e.message}")
            handleDeviceDisconnected()
        }
    }

    override fun cleanup() {
        try {
            scanReceiver?.let { context.unregisterReceiver(it) }
            scanReceiver = null
        } catch (_: Exception) { /* already unregistered */ }
        try {
            connectionReceiver?.let { context.unregisterReceiver(it) }
            connectionReceiver = null
        } catch (_: Exception) { /* already unregistered */ }
        try {
            bluetoothA2dp?.let { proxy ->
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.A2DP, proxy)
            }
        } catch (_: SecurityException) { }
        bluetoothA2dp = null
        try {
            bluetoothHeadset?.let { proxy ->
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, proxy)
            }
        } catch (_: SecurityException) { }
        bluetoothHeadset = null
    }
}
