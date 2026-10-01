package com.aethelworks.grooveplayer.data.repository

import com.aethelworks.grooveplayer.data.local.db.dao.TransferDao
import com.aethelworks.grooveplayer.data.local.db.dao.TransferFileDao
import com.aethelworks.grooveplayer.data.local.db.entity.TransferEntity
import com.aethelworks.grooveplayer.data.local.db.entity.TransferFileEntity
import com.aethelworks.grooveplayer.data.mapper.TransferMapper
import com.aethelworks.grooveplayer.domain.model.transfer.Transfer
import com.aethelworks.grooveplayer.domain.model.transfer.TransferStatus
import com.aethelworks.grooveplayer.domain.repository.transfer.IncomingTransferFile
import com.aethelworks.grooveplayer.domain.repository.transfer.TransferRepository
import com.aethelworks.grooveplayer.domain.transfer.ReceivedTransferIntake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val transferDao: TransferDao,
    private val transferFileDao: TransferFileDao,
) : TransferRepository {

    override fun observeActiveTransfers(): Flow<List<Transfer>> =
        transferDao.observeActiveTransfers().flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { transferFileDao.observeByTransferId(it.id) }) { fileArrays ->
                entities.mapIndexed { index, entity ->
                    TransferMapper.mapToDomain(entity, fileArrays.getOrNull(index) ?: emptyList())
                }
            }
        }

    override fun observeTransferHistory(): Flow<List<Transfer>> =
        transferDao.observeTransferHistory().flatMapLatest { entities ->
            if (entities.isEmpty()) flowOf(emptyList())
            else combine(entities.map { transferFileDao.observeByTransferId(it.id) }) { fileArrays ->
                entities.mapIndexed { index, entity ->
                    TransferMapper.mapToDomain(entity, fileArrays.getOrNull(index) ?: emptyList())
                }
            }
        }

    override suspend fun getTransferWithFiles(transferId: Long): Transfer? {
        val entity = transferDao.getById(transferId) ?: return null
        val files = transferFileDao.getByTransferId(transferId)
        return TransferMapper.mapToDomain(entity, files)
    }

    override suspend fun insertTransfer(
        deviceName: String,
        totalBytes: Long,
        filePaths: List<String>,
        isSender: Boolean,
    ): Long {
        val now = System.currentTimeMillis()
        val transferId = transferDao.insert(
            TransferEntity(
                deviceName = deviceName,
                startTime = now,
                endTime = null,
                totalBytes = totalBytes,
                transferredBytes = 0L,
                overallStatus = TransferStatus.PENDING.name,
                isSender = isSender,
            )
        )
        val files = filePaths.map { path ->
            val file = File(path)
            TransferFileEntity(
                transferId = transferId,
                fileName = file.name,
                filePath = path,
                fileSize = file.length(),
                transferredBytes = 0L,
                checksum = null,
                status = TransferStatus.PENDING.name,
                retryCount = 0,
            )
        }
        transferFileDao.insertAll(files)
        return transferId
    }

    override suspend fun updateTransferProgress(transferId: Long, transferredBytes: Long, status: String) {
        transferDao.updateProgress(transferId, transferredBytes, status)
    }

    override suspend fun updateFileProgress(fileId: Long, transferredBytes: Long, status: String, retryCount: Int) {
        transferFileDao.updateProgress(fileId, transferredBytes, status, retryCount)
    }

    override suspend fun updateFileChecksum(fileId: Long, checksum: String) {
        transferFileDao.updateChecksum(fileId, checksum)
    }

    override suspend fun completeTransfer(transferId: Long, status: String) {
        transferDao.complete(transferId, System.currentTimeMillis(), status)
    }

    override suspend fun failTransfer(transferId: Long, status: String) {
        transferDao.complete(transferId, System.currentTimeMillis(), status)
    }

    override suspend fun updateTransferStatus(transferId: Long, status: String) {
        transferDao.updateStatus(transferId, status)
    }

    override suspend fun updateTransferDeviceName(transferId: Long, deviceName: String) {
        transferDao.updateDeviceName(transferId, deviceName)
    }

    override suspend fun updateTransferTotalBytes(transferId: Long, totalBytes: Long) {
        transferDao.updateTotalBytes(transferId, totalBytes)
    }

    override suspend fun insertReceiverFiles(
        transferId: Long,
        receiveDirPath: String,
        fileInfos: List<IncomingTransferFile>,
    ) {
        val files = fileInfos.mapIndexed { index, info ->
            val safeName = ReceivedTransferIntake.displayName(info.fileName, "file_$index", null)
            val filePath = "$receiveDirPath/$safeName"
            TransferFileEntity(
                transferId = transferId,
                fileName = safeName,
                filePath = filePath,
                fileSize = info.fileSize,
                transferredBytes = 0L,
                checksum = info.checksum,
                status = TransferStatus.PENDING.name,
                retryCount = 0,
            )
        }
        transferFileDao.insertAll(files)
    }

    override suspend fun updateReceiverFileProgress(
        transferId: Long,
        fileIndex: Int,
        transferredBytes: Long,
        status: String,
    ) {
        val files = transferFileDao.getByTransferId(transferId)
        val file = files.getOrNull(fileIndex) ?: return
        transferFileDao.updateProgress(file.id, transferredBytes, status, file.retryCount)
    }

    override suspend fun terminateActiveTransfers() {
        transferDao.terminateAllActive(TransferStatus.FAILED.name, System.currentTimeMillis())
        transferFileDao.terminateAllActive(TransferStatus.FAILED.name)
    }
}
