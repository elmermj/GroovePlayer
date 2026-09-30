package com.aethelsoft.grooveplayer.domain.repository.transfer

import com.aethelsoft.grooveplayer.domain.model.transfer.Transfer
import kotlinx.coroutines.flow.Flow

/** A file the receiver learned from transfer metadata, including the sender SHA-256. */
data class IncomingTransferFile(
    val fileName: String,
    val fileSize: Long,
    val checksum: String,
)

/**
 * Repository for nearby P2P transfers.
 * Handles persistence and business logic for transfer operations.
 */
interface TransferRepository {

    /** Observe active transfers (pending, connecting, transferring, paused, retrying, checksum_validating) */
    fun observeActiveTransfers(): Flow<List<Transfer>>

    /** Observe transfer history (completed, failed, cancelled) */
    fun observeTransferHistory(): Flow<List<Transfer>>

    /** Get a transfer by ID with its files */
    suspend fun getTransferWithFiles(transferId: Long): Transfer?

    /** Insert a new transfer and its files */
    suspend fun insertTransfer(
        deviceName: String,
        totalBytes: Long,
        filePaths: List<String>,
        isSender: Boolean,
    ): Long

    /** Update transfer progress */
    suspend fun updateTransferProgress(transferId: Long, transferredBytes: Long, status: String)

    /** Update file progress */
    suspend fun updateFileProgress(fileId: Long, transferredBytes: Long, status: String, retryCount: Int)

    /** Store the SHA-256 the sender computed for this file. */
    suspend fun updateFileChecksum(fileId: Long, checksum: String)

    /** Complete a transfer */
    suspend fun completeTransfer(transferId: Long, status: String)

    /** Fail a transfer */
    suspend fun failTransfer(transferId: Long, status: String)

    /** Update transfer status (for pause/resume) */
    suspend fun updateTransferStatus(transferId: Long, status: String)

    /** Update transfer device name (e.g. when connection established) */
    suspend fun updateTransferDeviceName(transferId: Long, deviceName: String)

    /** Update transfer total bytes (e.g. when receiver gets metadata) */
    suspend fun updateTransferTotalBytes(transferId: Long, totalBytes: Long)

    /**
     * Insert file records for receiver when MSG_FILE_METADATA is received.
     * @param transferId Receiver's transfer ID
     * @param receiveDirPath Absolute path to base directory for received files
     * @param fileInfos Files parsed from metadata, including SHA-256
     */
    suspend fun insertReceiverFiles(
        transferId: Long,
        receiveDirPath: String,
        fileInfos: List<IncomingTransferFile>,
    )

    /**
     * Update progress for a specific file by transfer and index (for receiver).
     */
    suspend fun updateReceiverFileProgress(
        transferId: Long,
        fileIndex: Int,
        transferredBytes: Long,
        status: String,
    )

    /**
     * Mark every non-terminal transfer (and its files) as FAILED.
     * Used to reconcile rows left behind by a killed process and to clear
     * stale sessions before starting a new one.
     */
    suspend fun terminateActiveTransfers()
}
