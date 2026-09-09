package com.aethelsoft.grooveplayer.presentation.transfer

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aethelsoft.grooveplayer.data.transfer.NearbyTransferManager
import com.aethelsoft.grooveplayer.data.transfer.NearbyTransferOrchestrator
import com.aethelsoft.grooveplayer.data.transfer.TransferNotificationBridge
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelsoft.grooveplayer.domain.usecase.transfer.GetTransferFilePathsUseCase
import com.aethelsoft.grooveplayer.presentation.share.ShareIntentHolder
import com.aethelsoft.grooveplayer.utils.helpers.NearbyDeviceCapability
import com.aethelsoft.grooveplayer.utils.helpers.NearbyDeviceCapabilityHelper
import com.aethelsoft.grooveplayer.utils.helpers.logShareNearbyP2PTag
import com.aethelsoft.grooveplayer.services.NearbyTransferService
import com.aethelsoft.grooveplayer.utils.helpers.BatteryOptimizationHelper
import com.aethelsoft.grooveplayer.utils.helpers.NearbyTransferPermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nearbyTransferManager: NearbyTransferManager,
    private val capabilityHelper: NearbyDeviceCapabilityHelper,
    private val transferRepository: TransferRepository,
    private val getTransferFilePathsUseCase: GetTransferFilePathsUseCase,
    private val nearbyTransferOrchestrator: NearbyTransferOrchestrator,
    private val permissionHelper: NearbyTransferPermissionHelper,
    private val batteryHelper: BatteryOptimizationHelper,
    private val notificationBridge: TransferNotificationBridge,
) : ViewModel() {

    val discoveredEndpoints = nearbyTransferManager.discoveredEndpoints
    val connectionState = nearbyTransferManager.connectionState

    val deviceCapability: StateFlow<NearbyDeviceCapability> = MutableStateFlow(capabilityHelper.analyze()).asStateFlow()

    private val _showBatteryOptimizationPrompt = MutableStateFlow(false)
    val showBatteryOptimizationPrompt: StateFlow<Boolean> = _showBatteryOptimizationPrompt.asStateFlow()

    private val tag = logShareNearbyP2PTag(context)

    val hasAllPermissions: Boolean
        get() = permissionHelper.hasAllPermissions()

    fun getMissingPermissions(): Array<String> = permissionHelper.getMissingPermissions()

    fun shouldPromptBatteryWhitelist(): Boolean = batteryHelper.shouldPromptForBatteryWhitelist()

    fun startDiscovery() {
        viewModelScope.launch {
            if (!hasAllPermissions) {
                Log.w(tag, "startDiscovery: missing permissions. Request user grant: ${getMissingPermissions().contentToString()}")
                return@launch
            }
            Log.d(tag, "startDiscovery: starting discovery for nearby devices")
            if (batteryHelper.shouldPromptForBatteryWhitelist() && !batteryHelper.isIgnoringBatteryOptimizations()) {
                _showBatteryOptimizationPrompt.value = true
            }
            nearbyTransferManager.startDiscovery()
        }
    }

    fun stopDiscovery() {
        nearbyTransferManager.stopDiscovery()
    }

    suspend fun loadFilePathsFromShareIntent(): List<String> {
        val songs = ShareIntentHolder.songs.value.ifEmpty {
            ShareIntentHolder.songs.first()
        }
        val paths = getTransferFilePathsUseCase(songs)
        Log.d(tag, "loadFilePathsFromShareIntent: ${songs.size} songs -> ${paths.size} file paths")
        if (paths.isEmpty()) Log.w(tag, "loadFilePathsFromShareIntent: no valid file paths for transfer")
        return paths
    }

    fun startAdvertising(filePaths: List<String>, onConnectionRequest: (String, String) -> Unit) {
        viewModelScope.launch {
            if (!hasAllPermissions) {
                Log.w(tag, "startAdvertising: missing permissions. Request user grant: ${getMissingPermissions().contentToString()}")
                return@launch
            }
            Log.d(tag, "startAdvertising: starting with ${filePaths.size} files")
            if (batteryHelper.shouldPromptForBatteryWhitelist() && !batteryHelper.isIgnoringBatteryOptimizations()) {
                _showBatteryOptimizationPrompt.value = true
            }
            // A new session begins: fail any leftover non-terminal transfers so the
            // progress screen can never fall back to a stale 0% row.
            transferRepository.terminateActiveTransfers()
            val totalBytes = filePaths.sumOf { java.io.File(it).length() }
            val transferId = transferRepository.insertTransfer(
                deviceName = "Waiting...",
                totalBytes = totalBytes,
                filePaths = filePaths,
                isSender = true,
            )
            // Clear any terminal state left by a previous session; a stale
            // Completed/Failed would make the service tear down immediately.
            notificationBridge.reset()
            NearbyTransferService.start(context)
            val localName = android.os.Build.MODEL
            nearbyTransferManager.startAdvertising(localName) { endpointId, endpointName ->
                viewModelScope.launch {
                    transferRepository.updateTransferStatus(transferId, "CONNECTING")
                    transferRepository.updateTransferDeviceName(transferId, endpointName)
                    val payloadCallback = nearbyTransferOrchestrator.createPayloadCallback(
                        context, transferId, filePaths, endpointName
                    )
                    nearbyTransferManager.acceptConnection(endpointId, payloadCallback)
                }
                onConnectionRequest(endpointId, endpointName)
            }
        }
    }

    fun requestConnection(endpointId: String, deviceName: String) {
        viewModelScope.launch {
            // Fail leftover non-terminal transfers before starting this session.
            transferRepository.terminateActiveTransfers()
            val transferId = transferRepository.insertTransfer(
                deviceName = deviceName,
                totalBytes = 0L,
                filePaths = emptyList(),
                isSender = false,
            )
            val payloadCallback = nearbyTransferOrchestrator.createReceiverPayloadCallback(context, transferId)
            nearbyTransferManager.requestConnection(
                endpointId = endpointId,
                localName = android.os.Build.MODEL,
                payloadCallback = payloadCallback,
                onConnectionSuccess = {
                    viewModelScope.launch {
                        // Clear stale terminal state from a previous session before
                        // starting the foreground service.
                        notificationBridge.reset()
                        NearbyTransferService.start(context)
                        nearbyTransferOrchestrator.sendOffsetResponseToStartTransfer()
                    }
                },
            )
        }
    }

    fun dismissBatteryPrompt() {
        _showBatteryOptimizationPrompt.value = false
    }

    fun openBatterySettings() {
        batteryHelper.openBatteryOptimizationSettings()
        _showBatteryOptimizationPrompt.value = false
    }

    override fun onCleared() {
        super.onCleared()
        if (connectionState.value is NearbyTransferManager.ConnectionState.Connected) {
            // Handed off to TransferProgressScreen — keep the connection alive, but
            // stop the radios: leaving advertising running is what made this device
            // show up as a ghost/duplicate in later discovery sessions.
            nearbyTransferManager.stopAdvertising()
            nearbyTransferManager.stopDiscovery()
        } else {
            nearbyTransferManager.disconnect()
        }
    }
}
