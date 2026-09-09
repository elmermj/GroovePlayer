package com.aethelsoft.grooveplayer.data.transfer

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.aethelsoft.grooveplayer.utils.helpers.logShareNearbyP2PTag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Nearby Connections API for P2P discovery and transfer.
 * Uses P2P_POINT_TO_POINT strategy: transfers are strictly 1:1 and this
 * strategy allows the highest-bandwidth medium upgrades (WiFi direct/hotspot).
 */
@Singleton
class NearbyTransferManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val fileChunkSender: FileChunkSender,
    private val fileChunkReceiver: FileChunkReceiver,
) {
    private val tag = logShareNearbyP2PTag(context)

    private val connectionsClient: ConnectionsClient by lazy {
        Nearby.getConnectionsClient(context)
    }

    private val _discoveredEndpoints = MutableStateFlow<List<DiscoveredEndpoint>>(emptyList())
    val discoveredEndpoints: StateFlow<List<DiscoveredEndpoint>> = _discoveredEndpoints.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentEndpointId: String? = null
    private var isAdvertising = false
    private var isDiscovering = false

    private val advertisingOptions = AdvertisingOptions.Builder()
        .setStrategy(Strategy.P2P_POINT_TO_POINT)
        .build()

    private val discoveryOptions = DiscoveryOptions.Builder()
        .setStrategy(Strategy.P2P_POINT_TO_POINT)
        .build()

    // ===== Outgoing payload delivery tracking =====
    // sendPayload() only enqueues into Play Services; actual delivery is confirmed
    // through onPayloadTransferUpdate. Callers must forward those updates via
    // onOutgoingPayloadUpdate() so awaitSendWindow()/awaitAllPayloadsDelivered() work.
    private val inFlightPayloadIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    private val _inFlightPayloadCount = MutableStateFlow(0)
    private val _sendFailureCount = MutableStateFlow(0)

    private var receiverPayloadCallback: PayloadCallback? = null
    private var onConnectionSuccessCallback: (() -> Unit)? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            _connectionState.value = ConnectionState.Connecting(endpointId, info.endpointName)
            currentEndpointId = endpointId
            receiverPayloadCallback?.let { callback ->
                connectionsClient.acceptConnection(endpointId, callback)
                    .addOnSuccessListener {
                        Log.d(tag, "Receiver accepted connection to endpointId=$endpointId")
                    }
                    .addOnFailureListener {
                        Log.e(tag, "Receiver accept connection failed endpointId=$endpointId", it)
                        _connectionState.value = ConnectionState.Error(it.message ?: "Accept failed")
                    }
            }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                Log.d(tag, "Discovery: connection successful with endpointId=$endpointId")
                resetPayloadTracking()
                _connectionState.value = ConnectionState.Connected(endpointId)
                currentEndpointId = endpointId
                onConnectionSuccessCallback?.invoke()
                onConnectionSuccessCallback = null
            } else {
                Log.w(tag, "Discovery: connection failed endpointId=$endpointId status=${resolution.status.statusCode}")
                _connectionState.value = ConnectionState.Disconnected
                currentEndpointId = null
                onConnectionSuccessCallback = null
            }
            receiverPayloadCallback = null
        }

        override fun onDisconnected(endpointId: String) {
            Log.w(tag, "Discovery: disconnected from endpointId=$endpointId")
            _connectionState.value = ConnectionState.Disconnected
            currentEndpointId = null
            receiverPayloadCallback = null
            onConnectionSuccessCallback = null
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: com.google.android.gms.nearby.connection.DiscoveredEndpointInfo) {
            val name = info.endpointName ?: "Unknown"
            Log.d(tag, "Device found nearby: id=$endpointId name=$name")
            // Replace instead of append: found-events can repeat for the same endpoint.
            _discoveredEndpoints.value = _discoveredEndpoints.value
                .filter { it.endpointId != endpointId } +
                DiscoveredEndpoint(endpointId = endpointId, name = name)
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(tag, "Device lost: endpointId=$endpointId")
            _discoveredEndpoints.value = _discoveredEndpoints.value.filter { it.endpointId != endpointId }
        }
    }

    fun startAdvertising(localName: String, onConnectionRequest: (String, String) -> Unit) {
        if (isAdvertising) return
        connectionsClient.startAdvertising(
            localName,
            TransferProtocol.SERVICE_ID,
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                    onConnectionRequest(endpointId, info.endpointName)
                }

                override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
                    if (resolution.status.isSuccess) {
                        Log.d(tag, "Advertising: connection successful with endpointId=$endpointId")
                        resetPayloadTracking()
                        _connectionState.value = ConnectionState.Connected(endpointId)
                        currentEndpointId = endpointId
                    } else {
                        Log.w(tag, "Advertising: connection failed endpointId=$endpointId status=${resolution.status.statusCode}")
                        _connectionState.value = ConnectionState.Disconnected
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    Log.w(tag, "Advertising: disconnected from endpointId=$endpointId")
                    _connectionState.value = ConnectionState.Disconnected
                    currentEndpointId = null
                }
            },
            advertisingOptions
        ).addOnSuccessListener {
            isAdvertising = true
            Log.d(tag, "Advertising started successfully. Waiting for receiver.")
        }.addOnFailureListener {
            Log.e(tag, "Advertising failed", it)
            _connectionState.value = ConnectionState.Error(it.message ?: "Advertising failed")
        }
    }

    fun acceptConnection(endpointId: String, payloadCallback: PayloadCallback) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
            .addOnSuccessListener {
                Log.d(tag, "Connection accepted. endpointId=$endpointId")
                _connectionState.value = ConnectionState.Connected(endpointId)
                currentEndpointId = endpointId
            }
            .addOnFailureListener {
                Log.e(tag, "Accept connection failed endpointId=$endpointId", it)
                _connectionState.value = ConnectionState.Error(it.message ?: "Accept failed")
            }
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
    }

    fun startDiscovery() {
        if (isDiscovering) return
        _discoveredEndpoints.value = emptyList()
        connectionsClient.startDiscovery(
            TransferProtocol.SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            isDiscovering = true
            Log.d(tag, "Discovery started successfully. Searching for nearby devices.")
        }.addOnFailureListener {
            Log.e(tag, "Discovery failed", it)
            _connectionState.value = ConnectionState.Error(it.message ?: "Discovery failed")
        }
    }

    fun stopDiscovery() {
        if (!isDiscovering) return
        connectionsClient.stopDiscovery()
        isDiscovering = false
        _discoveredEndpoints.value = emptyList()
    }

    fun stopAdvertising() {
        if (!isAdvertising) return
        connectionsClient.stopAdvertising()
        isAdvertising = false
    }

    fun requestConnection(
        endpointId: String,
        localName: String,
        payloadCallback: PayloadCallback,
        onConnectionSuccess: (() -> Unit)? = null,
    ) {
        receiverPayloadCallback = payloadCallback
        onConnectionSuccessCallback = onConnectionSuccess
        connectionsClient.requestConnection(localName, endpointId, connectionLifecycleCallback)
            .addOnSuccessListener {
                Log.d(tag, "Connection request sent to endpointId=$endpointId")
            }
            .addOnFailureListener {
                Log.e(tag, "Connection request failed endpointId=$endpointId", it)
                _connectionState.value = ConnectionState.Error(it.message ?: "Connection failed")
                receiverPayloadCallback = null
                onConnectionSuccessCallback = null
            }
    }

    fun sendPayload(payload: Payload): Boolean {
        val endpointId = currentEndpointId ?: return false
        trackOutgoingPayload(payload.id)
        connectionsClient.sendPayload(endpointId, payload)
            .addOnFailureListener { e ->
                Log.e(tag, "sendPayload enqueue failed payloadId=${payload.id}", e)
                markOutgoingPayloadFailed(payload.id)
            }
        return true
    }

    fun sendBytes(data: ByteArray): Boolean {
        return sendPayload(Payload.fromBytes(data))
    }

    /**
     * Forward PayloadTransferUpdate events here (from any PayloadCallback) so
     * outgoing deliveries and failures are accounted for. Updates for incoming
     * payloads are ignored (their ids are never in the in-flight set).
     */
    fun onOutgoingPayloadUpdate(update: PayloadTransferUpdate) {
        when (update.status) {
            PayloadTransferUpdate.Status.SUCCESS -> {
                if (inFlightPayloadIds.remove(update.payloadId)) {
                    _inFlightPayloadCount.value = inFlightPayloadIds.size
                }
            }
            PayloadTransferUpdate.Status.FAILURE,
            PayloadTransferUpdate.Status.CANCELED,
            -> markOutgoingPayloadFailed(update.payloadId)
            else -> Unit // IN_PROGRESS
        }
    }

    /**
     * Suspends until fewer than [maxInFlight] payloads await delivery confirmation.
     * Returns false if the connection dropped or any send failed.
     */
    suspend fun awaitSendWindow(maxInFlight: Int): Boolean =
        combine(
            _inFlightPayloadCount,
            _sendFailureCount,
            _connectionState,
        ) { inFlight, failures, conn ->
            when {
                failures > 0 -> false
                conn !is ConnectionState.Connected -> false
                inFlight < maxInFlight -> true
                else -> null
            }
        }.filterNotNull().first()

    /** Suspends until every queued payload was confirmed delivered (or false on failure). */
    suspend fun awaitAllPayloadsDelivered(): Boolean = awaitSendWindow(1)

    fun resetPayloadTracking() {
        inFlightPayloadIds.clear()
        _inFlightPayloadCount.value = 0
        _sendFailureCount.value = 0
    }

    private fun trackOutgoingPayload(payloadId: Long) {
        inFlightPayloadIds.add(payloadId)
        _inFlightPayloadCount.value = inFlightPayloadIds.size
    }

    private fun markOutgoingPayloadFailed(payloadId: Long) {
        if (inFlightPayloadIds.remove(payloadId)) {
            _inFlightPayloadCount.value = inFlightPayloadIds.size
            _sendFailureCount.value += 1
        }
    }

    fun disconnect() {
        currentEndpointId?.let { connectionsClient.disconnectFromEndpoint(it) }
        currentEndpointId = null
        stopAdvertising()
        stopDiscovery()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun getConnectedEndpointId(): String? = currentEndpointId

    suspend fun getFileChunkSender(): FileChunkSender = fileChunkSender

    suspend fun getFileChunkReceiver(): FileChunkReceiver = fileChunkReceiver

    data class DiscoveredEndpoint(
        val endpointId: String,
        val name: String,
    )

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data class Connecting(val endpointId: String, val endpointName: String) : ConnectionState()
        data class Connected(val endpointId: String) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}
