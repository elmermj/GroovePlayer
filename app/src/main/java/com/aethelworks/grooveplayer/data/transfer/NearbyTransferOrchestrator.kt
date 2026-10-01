package com.aethelworks.grooveplayer.data.transfer

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import java.nio.ByteOrder
import com.aethelworks.grooveplayer.data.backup.GrooveDownloadsLocator
import com.aethelworks.grooveplayer.data.library.SongIdentityStore
import com.aethelworks.grooveplayer.domain.library.PrivateLibrarySongs
import com.aethelworks.grooveplayer.domain.library.SongHashRemap
import com.aethelworks.grooveplayer.domain.model.transfer.Transfer
import com.aethelworks.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelworks.grooveplayer.domain.repository.transfer.IncomingTransferFile
import com.aethelworks.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelworks.grooveplayer.domain.transfer.ReceiveFailure
import com.aethelworks.grooveplayer.domain.transfer.ReceivedTransferIntake
import com.aethelworks.grooveplayer.domain.transfer.userMessage
import com.aethelworks.grooveplayer.domain.usecase.home_category.RefreshMusicCatalogUseCase
import com.aethelworks.grooveplayer.services.NearbyTransferService
import com.aethelworks.grooveplayer.services.TransferServiceState
import com.aethelworks.grooveplayer.utils.helpers.logShareNearbyP2PTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the Nearby P2P transfer protocol.
 * Handles metadata exchange, chunk streaming, pause/resume/cancel.
 */
@Singleton
class NearbyTransferOrchestrator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val nearbyTransferManager: NearbyTransferManager,
    private val transferRepository: TransferRepository,
    private val fileChunkSender: FileChunkSender,
    private val fileChunkReceiver: FileChunkReceiver,
    private val transferController: TransferController,
    private val notificationBridge: TransferNotificationBridge,
    private val grooveDownloads: GrooveDownloadsLocator,
    private val refreshMusicCatalogUseCase: RefreshMusicCatalogUseCase,
    private val songIdentity: SongIdentityStore,
) {
    private val tag = logShareNearbyP2PTag(context)

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var currentTransferId: Long? = null
    private var isPaused = false

    // Receiver-side in-memory state. Chunk handling must be cheap: a full Room
    // query + two writes + a notification per 64KB chunk starves the pipeline.
    private var receiverFiles: MutableList<ReceiverFileState>? = null
    private var receiverDeviceName: String = "Sender"
    private var receiverTotalBytes: Long = 0L
    private var receiverTransferredBytes: Long = 0L
    private var receiverStatusCache: TransferStatus = TransferStatus.PENDING
    private var lastReceiverStatusCheckAt = 0L
    private var lastReceiverDbWriteAt = 0L

    fun createPayloadCallback(
        context: android.content.Context,
        transferId: Long,
        filePaths: List<String>,
        deviceName: String,
    ): PayloadCallback {
        transferController.setActiveTransfer(transferId)
        currentTransferId = transferId
        // Reset first: the bridge ignores non-terminal updates while in a terminal
        // state, so a leftover Completed/Failed would swallow this Connecting update.
        notificationBridge.reset()
        notificationBridge.updateState(TransferServiceState.Connecting(deviceName = deviceName))
        NearbyTransferService.start(context)
        return object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                scope.launch {
                    handlePayloadReceived(payload, transferId, filePaths, deviceName)
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                // Confirms delivery of our outgoing chunks (drives the send window).
                nearbyTransferManager.onOutgoingPayloadUpdate(update)
            }
        }
    }

    /** @return false when metadata was refused and the transfer is already marked failed. */
    private suspend fun sendFileMetadata(transferId: Long, filePaths: List<String>): Boolean {
        val files = filePaths.map { java.io.File(it) }
        if (files.isEmpty() || files.any { file ->
                val size = file.length()
                size <= 0L || size > ReceivedTransferIntake.MAX_FILE_BYTES
            }
        ) {
            failSenderTransfer(
                transferId,
                "Sender: refusing metadata outside the size limit transferId=$transferId",
            )
            return false
        }
        val metas = try {
            files.map { file ->
                TransferFileMeta(
                    name = file.name.ifBlank { "audio.bin" },
                    sizeBytes = file.length(),
                    checksum = fileChunkSender.computeChecksum(file.absolutePath),
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Sender: checksum failed transferId=$transferId", e)
            failSenderTransfer(transferId, "Sender: could not checksum files transferId=$transferId")
            return false
        }
        val totalBytes = metas.sumOf { it.sizeBytes }
        Log.d(tag, "Sender: sendFileMetadata fileCount=${metas.size} totalBytes=$totalBytes")
        val transfer = transferRepository.getTransferWithFiles(transferId)
        transfer?.files?.forEachIndexed { index, file ->
            val checksum = metas.getOrNull(index)?.checksum ?: return@forEachIndexed
            transferRepository.updateFileChecksum(file.id, checksum)
        }
        nearbyTransferManager.sendBytes(TransferProtocol.encodeFileMetadata(metas))
        return true
    }

    private suspend fun handlePayloadReceived(
        payload: Payload,
        transferId: Long,
        filePaths: List<String>,
        deviceName: String,
    ) = withContext(Dispatchers.IO) {
        val data = when (payload.type) {
            Payload.Type.BYTES -> payload.asBytes()
            else -> null
        }
        data?.let {
            if (data.isNotEmpty()) {
                val type = data[0].toInt() and 0xFF
                when (type) {
                    TransferProtocol.MSG_OFFSET_RESPONSE -> {
                        // Receiver responded with offset - send metadata first, then chunks
                        if (data.size >= 9) {
                            val offset = java.nio.ByteBuffer.wrap(data.copyOfRange(1, 9)).order(ByteOrder.BIG_ENDIAN).long
                            Log.d(tag, "Sender: MSG_OFFSET_RESPONSE received, sending ${filePaths.size} files (${filePaths.sumOf { java.io.File(it).length() }} bytes)")
                            if (!sendFileMetadata(transferId, filePaths)) return@withContext
                            startSendingChunks(transferId, filePaths, deviceName, 0, offset)
                        }
                    }
                }
            }
        }
    }

    private suspend fun startSendingChunks(
        transferId: Long,
        filePaths: List<String>,
        deviceName: String,
        fileIndex: Int,
        offset: Long,
    ) : Unit {
        withContext(Dispatchers.IO) {
            if (fileIndex >= filePaths.size) {
                // All chunks are only QUEUED in Play Services at this point. Wait until the
                // receiver confirmed delivery of every payload before declaring success —
                // otherwise tearing down the connection discards everything still queued.
                Log.d(tag, "Sender: all chunks queued for transferId=$transferId, awaiting delivery confirmations")
                if (!awaitAllPayloadsDeliveredOrFail(transferId, "final delivery")) return@withContext

                sendTransferComplete()
                if (!awaitAllPayloadsDeliveredOrFail(transferId, "MSG_COMPLETE delivery")) return@withContext

                Log.d(tag, "Sender: all payloads delivered for transferId=$transferId, marking COMPLETED")
                val finalTransfer = transferRepository.getTransferWithFiles(transferId)
                if (finalTransfer != null) {
                    transferRepository.updateTransferProgress(
                        transferId,
                        finalTransfer.totalBytes,
                        TransferStatus.COMPLETED.name
                    )
                    notificationBridge.updateState(
                        TransferServiceState.Transferring(
                            deviceName = deviceName,
                            currentFileName = "",
                            transferredBytes = finalTransfer.totalBytes,
                            totalBytes = finalTransfer.totalBytes,
                            bytesPerSec = 0L,
                        )
                    )
                }
                transferRepository.completeTransfer(transferId, TransferStatus.COMPLETED.name)
                transferController.setActiveTransfer(null)
                notificationBridge.updateState(TransferServiceState.Completed)
                return@withContext
            }
            val path = filePaths[fileIndex]
            val transfer = transferRepository.getTransferWithFiles(transferId) ?: return@withContext
            val files = transfer.files
            Log.d(tag, "Sender: starting fileIndex=$fileIndex path=$path size=${java.io.File(path).length()} transferId=$transferId")
            notificationBridge.updateState(
                TransferServiceState.Transferring(
                    deviceName = deviceName,
                    currentFileName = java.io.File(path).name,
                    transferredBytes = transfer.transferredBytes,
                    totalBytes = transfer.totalBytes,
                    bytesPerSec = 0L,
                )
            )
            val fileId = files.getOrNull(fileIndex)?.id ?: return@withContext
            val fileSize = java.io.File(path).length()
            var currentOffset = offset
            val chunkSize = TransferProtocol.DEFAULT_CHUNK_SIZE
            var lastUpdateTime = System.currentTimeMillis()
            var lastBytes = 0L
            var lastDbWriteTime = 0L

            while (currentOffset < fileSize) {
                if (transferController.consumeCancelRequested()) {
                    Log.w(tag, "Sender: cancel requested for transferId=$transferId, stopping at fileIndex=$fileIndex offset=$currentOffset")
                    transferRepository.completeTransfer(transferId, TransferStatus.CANCELLED.name)
                    notificationBridge.updateState(TransferServiceState.Failed("Cancelled"))
                    return@withContext
                }
                if (transferController.isPauseRequested()) {
                    isPaused = true
                    Log.d(tag, "Sender: pause requested for transferId=$transferId, fileIndex=$fileIndex offset=$currentOffset")
                    transferRepository.updateTransferStatus(transferId, TransferStatus.PAUSED.name)
                    kotlinx.coroutines.delay(500)
                    continue
                }
                isPaused = false
                // Backpressure: don't queue more chunks until earlier ones are delivered.
                val windowOpen = kotlinx.coroutines.withTimeoutOrNull(TransferProtocol.SEND_STALL_TIMEOUT_MS) {
                    nearbyTransferManager.awaitSendWindow(TransferProtocol.MAX_IN_FLIGHT_CHUNKS)
                } ?: false
                if (!windowOpen) {
                    failSenderTransfer(
                        transferId,
                        "Sender: send window never opened (stall/disconnect/send failure) " +
                            "transferId=$transferId fileIndex=$fileIndex offset=$currentOffset",
                    )
                    return@withContext
                }
                val chunk = fileChunkSender.readChunk(path, currentOffset, chunkSize) ?: break
                val message = ByteArray(1 + 8 + 8 + chunk.size)
                message[0] = TransferProtocol.MSG_CHUNK.toByte()
                java.nio.ByteBuffer.wrap(message, 1, 8).order(ByteOrder.BIG_ENDIAN)
                    .putLong(fileIndex.toLong())
                java.nio.ByteBuffer.wrap(message, 9, 8).order(ByteOrder.BIG_ENDIAN)
                    .putLong(currentOffset)
                System.arraycopy(chunk, 0, message, 17, chunk.size)
                val sent = nearbyTransferManager.sendBytes(message)
                if (!sent) {
                    Log.e(tag, "Sender: sendBytes failed for transferId=$transferId fileIndex=$fileIndex offset=$currentOffset chunkSize=${chunk.size}")
                    notificationBridge.updateState(TransferServiceState.Failed("Send failed"))
                    break
                }
                currentOffset += chunk.size
                val totalTransferred = transfer.transferredBytes + (currentOffset - offset)
                val now = System.currentTimeMillis()
                // Throttle Room writes: per-chunk writes at 64KB granularity slow the
                // pipeline without adding useful progress resolution.
                if (now - lastDbWriteTime >= 500 || currentOffset >= fileSize) {
                    lastDbWriteTime = now
                    transferRepository.updateTransferProgress(
                        transferId,
                        totalTransferred,
                        TransferStatus.TRANSFERRING.name
                    )
                    transferRepository.updateFileProgress(
                        fileId,
                        currentOffset,
                        TransferStatus.TRANSFERRING.name,
                        0
                    )
                }
                if (now - lastUpdateTime >= 500) {
                    val bytesPerSec = if (now > lastUpdateTime) (currentOffset - lastBytes) * 1000 / (now - lastUpdateTime) else 0L
                    lastUpdateTime = now
                    lastBytes = currentOffset
                    val currentFileName = java.io.File(path).name
                    notificationBridge.updateState(
                        TransferServiceState.Transferring(
                            deviceName = deviceName,
                            currentFileName = currentFileName,
                            transferredBytes = totalTransferred,
                            totalBytes = transfer.totalBytes,
                            bytesPerSec = bytesPerSec,
                        )
                    )
                }
            }
            if (currentOffset >= fileSize) {
                Log.d(tag, "Sender: completed fileIndex=$fileIndex size=$fileSize transferId=$transferId")
                transferRepository.updateFileProgress(
                    fileId,
                    fileSize,
                    TransferStatus.COMPLETED.name,
                    0
                )
                startSendingChunks(
                    transferId,
                    filePaths,
                    deviceName,
                    fileIndex + 1,
                    0
                )
            }
        }
    }

    private fun sendTransferComplete() {
        val message = byteArrayOf(TransferProtocol.MSG_COMPLETE.toByte())
        nearbyTransferManager.sendBytes(message)
    }

    /** Waits for all queued payloads to be delivered; marks the transfer FAILED on stall/disconnect. */
    private suspend fun awaitAllPayloadsDeliveredOrFail(transferId: Long, stage: String): Boolean {
        val delivered = kotlinx.coroutines.withTimeoutOrNull(TransferProtocol.SEND_STALL_TIMEOUT_MS) {
            nearbyTransferManager.awaitAllPayloadsDelivered()
        } ?: false
        if (!delivered) {
            failSenderTransfer(transferId, "Sender: $stage not confirmed (stall/disconnect) transferId=$transferId")
        }
        return delivered
    }

    private suspend fun failSenderTransfer(transferId: Long, logMessage: String) {
        Log.e(tag, logMessage)
        transferRepository.completeTransfer(transferId, TransferStatus.FAILED.name)
        transferController.setActiveTransfer(null)
        notificationBridge.updateState(TransferServiceState.Failed("Connection lost"))
    }

    /**
     * PayloadCallback for the receiver (discoverer). Must accept connection and receive chunks.
     */
    fun createReceiverPayloadCallback(context: android.content.Context, transferId: Long): PayloadCallback {
        // Chunks land in a private staging dir, then move into groove-library on complete.
        val receiveDir = incomingDir()
        receiverFiles = null
        receiverDeviceName = "Sender"
        receiverTotalBytes = 0L
        receiverTransferredBytes = 0L
        receiverStatusCache = TransferStatus.PENDING
        lastReceiverStatusCheckAt = 0L
        lastReceiverDbWriteAt = 0L
        // Nearby delivers BYTES payloads in send order, but scope.launch-per-payload
        // executes them in ARBITRARY order (a Mutex is not FIFO). A single consumer
        // over a channel preserves arrival order end-to-end, so MSG_COMPLETE can never
        // overtake pending chunk writes and drop the rest of the transfer.
        val payloadPipeline = Channel<Payload>(Channel.UNLIMITED)
        scope.launch {
            for (payload in payloadPipeline) {
                handleReceiverPayloadReceived(transferId, payload, receiveDir)
                val type = payload.asBytes()?.firstOrNull()?.toInt()?.and(0xFF)
                if (type == TransferProtocol.MSG_COMPLETE || type == TransferProtocol.MSG_CANCEL) break
            }
            payloadPipeline.close()
        }
        return object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                Log.d(tag, "Receiver: onPayloadReceived endpointId=$endpointId transferId=$transferId type=${payload.type}")
                if (payloadPipeline.trySend(payload).isFailure) {
                    Log.w(tag, "Receiver: payload dropped, pipeline closed transferId=$transferId")
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
                // Confirms delivery of receiver-sent control messages (offset response).
                nearbyTransferManager.onOutgoingPayloadUpdate(update)
            }
        }
    }

    private suspend fun handleReceiverPayloadReceived(
        transferId: Long,
        payload: Payload,
        receiveDir: java.io.File,
    ) = withContext(Dispatchers.IO) {
        try {
            val data = when (payload.type) {
                Payload.Type.BYTES -> payload.asBytes()
                else -> {
                    Log.w(tag, "Receiver: unexpected payload type ${payload.type}, transferId=$transferId")
                    return@withContext
                }
            }
            if (data == null || data.isEmpty()) {
                Log.w(tag, "Receiver: empty or null bytes payload, transferId=$transferId")
                return@withContext
            }
            val type = data[0].toInt() and 0xFF
            Log.d(tag, "Receiver: handleReceiverPayloadReceived type=$type transferId=$transferId dir=${receiveDir.absolutePath} size=${data.size}")
            when (type) {
                TransferProtocol.MSG_FILE_METADATA -> {
                    val decoded = TransferProtocol.decodeFileMetadata(data)
                    if (decoded == null || decoded.files.isEmpty()) {
                        failReceiverTransfer(transferId, "File metadata was incomplete")
                        return@withContext
                    }
                    val overLimit = decoded.files.any { file ->
                        file.sizeBytes <= 0L || file.sizeBytes > ReceivedTransferIntake.MAX_FILE_BYTES
                    }
                    if (overLimit) {
                        failReceiverTransfer(transferId, ReceiveFailure.OVER_LIMIT.userMessage())
                        return@withContext
                    }
                    val totalBytes = decoded.totalBytes
                    Log.d(tag, "Receiver: MSG_FILE_METADATA transferId=$transferId totalBytes=$totalBytes fileCount=${decoded.files.size} receiveDir=${receiveDir.absolutePath}")
                    transferRepository.updateTransferTotalBytes(transferId, totalBytes)
                    transferRepository.updateTransferStatus(transferId, TransferStatus.TRANSFERRING.name)
                    val fileInfos = decoded.files.map { file ->
                        IncomingTransferFile(
                            fileName = file.name,
                            fileSize = file.sizeBytes,
                            checksum = file.checksum,
                        )
                    }
                    Log.d(tag, "Receiver: inserting ${fileInfos.size} file entities for transferId=$transferId")
                    transferRepository.insertReceiverFiles(
                        transferId = transferId,
                        receiveDirPath = receiveDir.absolutePath,
                        fileInfos = fileInfos,
                    )
                    val transfer = transferRepository.getTransferWithFiles(transferId)
                    receiverFiles = transfer?.files?.map {
                        ReceiverFileState(
                            fileName = it.fileName,
                            filePath = it.filePath,
                            fileSize = it.fileSize,
                            transferredBytes = it.transferredBytes,
                        )
                    }?.toMutableList()
                    receiverDeviceName = transfer?.deviceName ?: "Sender"
                    receiverTotalBytes = totalBytes
                    receiverTransferredBytes = transfer?.transferredBytes ?: 0L
                    receiverStatusCache = TransferStatus.TRANSFERRING
                    notificationBridge.updateState(
                        TransferServiceState.Transferring(
                            deviceName = receiverDeviceName,
                            currentFileName = fileInfos.firstOrNull()?.fileName ?: "",
                            transferredBytes = 0L,
                            totalBytes = totalBytes,
                            bytesPerSec = 0L,
                        )
                    )
                }
                TransferProtocol.MSG_CHUNK -> {
                    if (data.size >= 17) {
                        val fileIndex = java.nio.ByteBuffer.wrap(data.copyOfRange(1, 9)).order(ByteOrder.BIG_ENDIAN).long.toInt()
                        val offset = java.nio.ByteBuffer.wrap(data.copyOfRange(9, 17)).order(ByteOrder.BIG_ENDIAN).long
                        val chunkData = data.copyOfRange(17, data.size)
                        Log.v(tag, "Receiver: MSG_CHUNK transferId=$transferId fileIndex=$fileIndex offset=$offset chunkSize=${chunkData.size}")
                        val now = System.currentTimeMillis()
                        // Refresh cancel/failure status from Room at most every 500ms.
                        if (now - lastReceiverStatusCheckAt >= 500) {
                            lastReceiverStatusCheckAt = now
                            transferRepository.getTransferWithFiles(transferId)?.let { fresh ->
                                receiverStatusCache = fresh.overallStatus
                            }
                        }
                        if (receiverStatusCache == TransferStatus.COMPLETED ||
                            receiverStatusCache == TransferStatus.CANCELLED ||
                            receiverStatusCache == TransferStatus.FAILED ||
                            receiverStatusCache == TransferStatus.CHECKSUM_VALIDATING
                        ) {
                            return@withContext
                        }
                        val files = receiverFiles ?: rebuildReceiverCache(transferId)
                        val file = files?.getOrNull(fileIndex)
                        if (file == null) {
                            Log.w(tag, "Receiver: no file entity for index=$fileIndex transferId=$transferId, knownFiles=${files?.size ?: 0}")
                            return@withContext
                        }
                        // Write chunk into the staged file; progress is tracked in memory
                        // and persisted on a 500ms cadence plus every file boundary.
                        fileChunkReceiver.writeChunk(file.filePath, offset, chunkData)
                        file.transferredBytes += chunkData.size
                        receiverTransferredBytes += chunkData.size
                        val fileCompleted = file.transferredBytes >= file.fileSize
                        if (fileCompleted || now - lastReceiverDbWriteAt >= 500) {
                            lastReceiverDbWriteAt = now
                            transferRepository.updateTransferProgress(
                                transferId,
                                receiverTransferredBytes,
                                TransferStatus.TRANSFERRING.name,
                            )
                            transferRepository.updateReceiverFileProgress(
                                transferId,
                                fileIndex,
                                file.transferredBytes,
                                if (fileCompleted) TransferStatus.COMPLETED.name else TransferStatus.TRANSFERRING.name,
                            )
                            notificationBridge.updateState(
                                TransferServiceState.Transferring(
                                    deviceName = receiverDeviceName,
                                    currentFileName = file.fileName,
                                    transferredBytes = receiverTransferredBytes,
                                    totalBytes = receiverTotalBytes,
                                    bytesPerSec = 0L,
                                )
                            )
                        }
                    }
                }
                TransferProtocol.MSG_COMPLETE -> {
                    Log.d(tag, "Receiver: MSG_COMPLETE transferId=$transferId, validating checksums")
                    if (receiverFiles != null) {
                        transferRepository.updateTransferProgress(
                            transferId,
                            receiverTransferredBytes,
                            TransferStatus.TRANSFERRING.name,
                        )
                    }
                    try {
                        finishVerifiedReceive(transferId)
                    } catch (e: Exception) {
                        Log.e(tag, "Receiver: checksum validation failed transferId=$transferId", e)
                        incomingDir().listFiles()?.forEach { child ->
                            if (child.isFile) child.delete()
                        }
                        failReceiverTransfer(transferId, ReceiveFailure.CHECKSUM_MISMATCH.userMessage())
                    }
                }
                TransferProtocol.MSG_CANCEL -> {
                    Log.d(tag, "Receiver: MSG_CANCEL transferId=$transferId")
                    receiveDir.listFiles()?.forEach { child ->
                        if (child.isFile) child.delete()
                    }
                }
                else -> Log.w(tag, "Receiver: unknown message type=$type transferId=$transferId")
            }
        } catch (e: Exception) {
            Log.e(tag, "Receiver: error handling payload transferId=$transferId", e)
        }
    }

    /** Rebuilds in-memory receiver state from Room (e.g. metadata handled before a process restart). */
    private suspend fun rebuildReceiverCache(transferId: Long): MutableList<ReceiverFileState>? {
        val transfer = transferRepository.getTransferWithFiles(transferId) ?: return null
        if (transfer.files.isEmpty()) return null
        receiverDeviceName = transfer.deviceName
        receiverTotalBytes = transfer.totalBytes
        receiverTransferredBytes = transfer.transferredBytes
        receiverStatusCache = transfer.overallStatus
        return transfer.files.map {
            ReceiverFileState(
                fileName = it.fileName,
                filePath = it.filePath,
                fileSize = it.fileSize,
                transferredBytes = it.transferredBytes,
            )
        }.toMutableList().also { receiverFiles = it }
    }

    private data class PlacedReceive(
        val index: Int,
        val fileName: String,
        val libraryPath: String,
        val contentHash: String,
        val fileSize: Long,
    )

    private data class ReceiverFileState(
        val fileName: String,
        val filePath: String,
        val fileSize: Long,
        var transferredBytes: Long,
    )

    /**
     * Sends MSG_OFFSET_RESPONSE to tell the sender to start sending chunks.
     * Call this when the receiver's connection is established.
     */
    fun sendOffsetResponseToStartTransfer() {
        val message = ByteArray(9)
        message[0] = TransferProtocol.MSG_OFFSET_RESPONSE.toByte()
        java.nio.ByteBuffer.wrap(message, 1, 8).order(ByteOrder.BIG_ENDIAN).putLong(0L)
        nearbyTransferManager.sendBytes(message)
    }

    /**
     * Hash every staged file before any of them leave `.incoming`. A size or
     * checksum failure deletes the staged bytes and writes no catalog row.
     */
    private suspend fun finishVerifiedReceive(transferId: Long) {
        val finalTransfer = transferRepository.getTransferWithFiles(transferId)
        if (finalTransfer == null || finalTransfer.files.isEmpty()) {
            failReceiverTransfer(transferId, "Transfer could not be finished")
            return
        }
        transferRepository.updateTransferStatus(transferId, TransferStatus.CHECKSUM_VALIDATING.name)
        receiverStatusCache = TransferStatus.CHECKSUM_VALIDATING
        notificationBridge.updateState(
            TransferServiceState.Transferring(
                deviceName = finalTransfer.deviceName,
                currentFileName = "",
                transferredBytes = finalTransfer.transferredBytes,
                totalBytes = finalTransfer.totalBytes.coerceAtLeast(1L),
                bytesPerSec = 0L,
            )
        )
        val failure = firstReceiveFailure(transferId, finalTransfer)
        if (failure != null) {
            discardStaged(finalTransfer)
            finalTransfer.files.forEachIndexed { index, file ->
                transferRepository.updateReceiverFileProgress(
                    transferId,
                    index,
                    file.transferredBytes,
                    TransferStatus.FAILED.name,
                )
            }
            failReceiverTransfer(transferId, failure.userMessage())
            return
        }
        val libraryRoot = grooveDownloads.directory()
        val known = SongHashRemap.privateHashes(songIdentity.rows()).toMutableSet()
        val placed = mutableListOf<PlacedReceive>()
        for ((index, file) in finalTransfer.files.withIndex()) {
            val decision = ReceivedTransferIntake.acceptVerified(
                staged = java.io.File(file.filePath),
                libraryRoot = libraryRoot,
                displayName = file.fileName,
                expectedSize = file.fileSize,
                contentHash = file.checksum.orEmpty(),
                knownPrivateHashes = known,
            )
            val hash = ReceivedTransferIntake.catalogRowOrNull(decision, libraryRoot)
            if (hash != null && decision.libraryPath != null) {
                known += hash
                placed += PlacedReceive(index, file.fileName, decision.libraryPath, hash, file.fileSize)
            } else if (decision.alreadyInLibrary) {
                transferRepository.updateReceiverFileProgress(
                    transferId,
                    index,
                    file.fileSize,
                    TransferStatus.COMPLETED.name,
                )
            } else {
                placed.forEach { java.io.File(it.libraryPath).delete() }
                discardStaged(finalTransfer)
                failReceiverTransfer(
                    transferId,
                    decision.failure?.userMessage() ?: ReceiveFailure.UNREADABLE.userMessage(),
                )
                return
            }
        }
        var stored = 0
        for (item in placed) {
            try {
                songIdentity.adoptVerifiedCopy(
                    hash = item.contentHash,
                    libraryPath = item.libraryPath,
                    title = item.fileName.substringBeforeLast('.', item.fileName),
                    durationMs = 0L,
                )
                stored++
                transferRepository.updateReceiverFileProgress(
                    transferId,
                    item.index,
                    item.fileSize,
                    TransferStatus.COMPLETED.name,
                )
            } catch (e: Exception) {
                Log.e(tag, "Receiver: catalog insert failed for ${item.fileName}", e)
                placed.forEach { java.io.File(it.libraryPath).delete() }
                failReceiverTransfer(transferId, ReceiveFailure.UNREADABLE.userMessage())
                return
            }
        }
        Log.d(tag, "Receiver: stored $stored/${finalTransfer.files.size} files in ${libraryRoot.absolutePath}")
        transferRepository.completeTransfer(transferId, TransferStatus.COMPLETED.name)
        notificationBridge.updateState(TransferServiceState.Completed)
        if (stored > 0) {
            try {
                refreshMusicCatalogUseCase()
                Log.d(tag, "Receiver: library catalog refreshed after transferId=$transferId")
            } catch (e: Exception) {
                Log.e(tag, "Receiver: catalog refresh failed after transferId=$transferId", e)
            }
        }
    }

    private suspend fun firstReceiveFailure(transferId: Long, transfer: Transfer): ReceiveFailure? {
        for ((index, file) in transfer.files.withIndex()) {
            transferRepository.updateReceiverFileProgress(
                transferId,
                index,
                file.transferredBytes,
                TransferStatus.CHECKSUM_VALIDATING.name,
            )
            val staged = java.io.File(file.filePath)
            val sizeProblem = ReceivedTransferIntake.sizeFailure(staged, file.fileSize)
            if (sizeProblem != null) return sizeProblem
            val expected = file.checksum
            val checksumOk = !expected.isNullOrBlank() &&
                fileChunkReceiver.validateChecksum(staged.absolutePath, expected)
            if (!checksumOk) return ReceiveFailure.CHECKSUM_MISMATCH
        }
        return null
    }

    private fun discardStaged(transfer: Transfer) {
        transfer.files.forEach { file ->
            val staged = java.io.File(file.filePath)
            if (staged.exists()) staged.delete()
        }
        incomingDir().listFiles()?.forEach { child ->
            if (child.isFile) child.delete()
        }
    }

    private suspend fun failReceiverTransfer(transferId: Long, message: String) {
        Log.e(tag, "Receiver: $message transferId=$transferId")
        receiverStatusCache = TransferStatus.FAILED
        transferRepository.failTransfer(transferId, TransferStatus.FAILED.name)
        transferController.setActiveTransfer(null)
        notificationBridge.updateState(TransferServiceState.Failed(message))
    }

    /**
     * In-progress chunks stay under the private library's incoming dir.
     * MSG_COMPLETE moves finished files into the library root after SHA-256 checks.
     */
    private fun incomingDir(): java.io.File {
        return java.io.File(grooveDownloads.directory(), PrivateLibrarySongs.INCOMING_DIR).apply { mkdirs() }
    }

    fun cleanup() {
        currentTransferId = null
        transferController.setActiveTransfer(null)
        notificationBridge.reset()
        scope.cancel()
    }
}
