package com.aethelsoft.grooveplayer.data.transfer

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import java.nio.ByteOrder
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelsoft.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelsoft.grooveplayer.domain.usecase.home_category.RefreshMusicCatalogUseCase
import com.aethelsoft.grooveplayer.services.NearbyTransferService
import com.aethelsoft.grooveplayer.services.TransferServiceState
import com.aethelsoft.grooveplayer.utils.helpers.logShareNearbyP2PTag
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
    private val receivedMediaPublisher: ReceivedMediaPublisher,
    private val refreshMusicCatalogUseCase: RefreshMusicCatalogUseCase,
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

    private fun sendFileMetadata(filePaths: List<String>) {
        val totalBytes = filePaths.sumOf { java.io.File(it).length() }
        Log.d(tag, "Sender: sendFileMetadata fileCount=${filePaths.size} totalBytes=$totalBytes")
        val fileCount = filePaths.size
        val nameBytes = filePaths.map { it.substringAfterLast('/').toByteArray(Charsets.UTF_8) }
        val totalSize = 1 + 8 + 4 + nameBytes.sumOf { 2 + it.size + 8 }
        val message = ByteArray(totalSize)
        var pos = 0
        message[pos++] = TransferProtocol.MSG_FILE_METADATA.toByte()
        java.nio.ByteBuffer.wrap(message, pos, 8).order(ByteOrder.BIG_ENDIAN).putLong(totalBytes)
        pos += 8
        java.nio.ByteBuffer.wrap(message, pos, 4).order(ByteOrder.BIG_ENDIAN).putInt(fileCount)
        pos += 4
        for (i in filePaths.indices) {
            val name = nameBytes[i]
            java.nio.ByteBuffer.wrap(message, pos, 2).order(ByteOrder.BIG_ENDIAN).putShort(name.size.toShort())
            pos += 2
            System.arraycopy(name, 0, message, pos, name.size)
            pos += name.size
            val size = java.io.File(filePaths[i]).length()
            java.nio.ByteBuffer.wrap(message, pos, 8).order(ByteOrder.BIG_ENDIAN).putLong(size)
            pos += 8
        }
        nearbyTransferManager.sendBytes(message)
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
                            sendFileMetadata(filePaths)
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
        // Stage into app-private dir during chunked writes, then publish to Music/Groove Downloads.
        val receiveDir = getGrooveDownloadsStagingDir(context)
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
                handleReceiverPayloadReceived(context, transferId, payload, receiveDir)
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
        ctx: android.content.Context,
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
                    if (data.size >= 13) {
                        val totalBytes = java.nio.ByteBuffer.wrap(data.copyOfRange(1, 9)).order(ByteOrder.BIG_ENDIAN).long
                        val fileCount = java.nio.ByteBuffer.wrap(data.copyOfRange(9, 13)).order(ByteOrder.BIG_ENDIAN).int
                        Log.d(tag, "Receiver: MSG_FILE_METADATA transferId=$transferId totalBytes=$totalBytes fileCount=$fileCount receiveDir=${receiveDir.absolutePath}")
                        transferRepository.updateTransferTotalBytes(transferId, totalBytes)
                        transferRepository.updateTransferStatus(transferId, TransferStatus.TRANSFERRING.name)
                        val fileInfos = mutableListOf<Pair<String, Long>>()
                        var pos = 13
                        for (i in 0 until fileCount) {
                            if (pos + 2 > data.size) break
                            val nameLen = java.nio.ByteBuffer.wrap(data.copyOfRange(pos, pos + 2)).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
                            pos += 2
                            if (pos + nameLen + 8 > data.size) break
                            val fileName = data.copyOfRange(pos, pos + nameLen).toString(Charsets.UTF_8)
                            pos += nameLen
                            val fileSize = java.nio.ByteBuffer.wrap(data.copyOfRange(pos, pos + 8)).order(ByteOrder.BIG_ENDIAN).long
                            pos += 8
                            fileInfos.add(fileName to fileSize)
                        }
                        if (fileInfos.isNotEmpty()) {
                            Log.d(tag, "Receiver: inserting ${fileInfos.size} file entities for transferId=$transferId -> $fileInfos")
                            transferRepository.insertReceiverFiles(
                                transferId = transferId,
                                receiveDirPath = receiveDir.absolutePath,
                                fileInfos = fileInfos,
                            )
                        }
                        // Cache file paths/sizes so chunk handling needs no per-chunk Room queries.
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
                                currentFileName = fileInfos.firstOrNull()?.first ?: "",
                                transferredBytes = 0L,
                                totalBytes = totalBytes,
                                bytesPerSec = 0L,
                            )
                        )
                    }
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
                            receiverStatusCache == TransferStatus.FAILED
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
                    Log.d(tag, "Receiver: MSG_COMPLETE transferId=$transferId, marking COMPLETED")
                    // Flush progress not yet persisted by the 500ms throttle.
                    if (receiverFiles != null) {
                        transferRepository.updateTransferProgress(
                            transferId,
                            receiverTransferredBytes,
                            TransferStatus.TRANSFERRING.name,
                        )
                    }
                    val finalTransfer = transferRepository.getTransferWithFiles(transferId)
                    // Mark terminal immediately so the notification leaves "Transferring 99%"
                    // before MediaStore publish / catalog refresh (can take seconds).
                    notificationBridge.updateState(
                        TransferServiceState.Transferring(
                            deviceName = finalTransfer?.deviceName ?: "Sender",
                            currentFileName = "",
                            transferredBytes = finalTransfer?.totalBytes ?: 0L,
                            totalBytes = (finalTransfer?.totalBytes ?: 0L).coerceAtLeast(1L),
                            bytesPerSec = 0L,
                        )
                    )
                    notificationBridge.updateState(TransferServiceState.Completed)
                    transferRepository.completeTransfer(transferId, TransferStatus.COMPLETED.name)

                    if (finalTransfer != null) {
                        // App-private paths are invisible to MediaStore/MediaScanner (scan → null URI).
                        // Publish into public Music/Groove Downloads so Files + library can see them.
                        var published = 0
                        finalTransfer.files.forEach { fileEntity ->
                            val staged = java.io.File(fileEntity.filePath)
                            val uri = receivedMediaPublisher.publishToMusicDownloads(
                                context = ctx,
                                sourceFile = staged,
                                displayName = fileEntity.fileName,
                            )
                            if (uri != null) {
                                published++
                                if (!staged.delete()) {
                                    Log.w(tag, "Receiver: could not delete staging file ${staged.absolutePath}")
                                }
                            } else {
                                Log.e(
                                    tag,
                                    "Receiver: failed to publish ${fileEntity.fileName}; " +
                                        "left at ${staged.absolutePath} (not visible in library)"
                                )
                            }
                        }
                        Log.d(
                            tag,
                            "Receiver: published $published/${finalTransfer.files.size} files to Music/Groove Downloads"
                        )
                        try {
                            refreshMusicCatalogUseCase()
                            Log.d(tag, "Receiver: library catalog refreshed after transferId=$transferId")
                        } catch (e: Exception) {
                            Log.e(tag, "Receiver: catalog refresh failed after transferId=$transferId", e)
                        }
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
     * App-private staging directory for in-progress chunk writes.
     * Final media is published to MediaStore `Music/Groove Downloads` on MSG_COMPLETE —
     * app-private paths are not MediaStore-visible (MediaScanner returns null).
     */
    private fun getGrooveDownloadsStagingDir(context: android.content.Context): java.io.File {
        val externalBase = context.getExternalFilesDir(null)
        val baseDir = externalBase ?: context.filesDir
        return java.io.File(baseDir, "Groove Downloads staging").apply { mkdirs() }
    }

    fun cleanup() {
        currentTransferId = null
        transferController.setActiveTransfer(null)
        notificationBridge.reset()
        scope.cancel()
    }
}
