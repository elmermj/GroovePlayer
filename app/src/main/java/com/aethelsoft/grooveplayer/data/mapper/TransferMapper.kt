package com.aethelsoft.grooveplayer.data.mapper

import com.aethelsoft.grooveplayer.data.local.db.entity.TransferEntity
import com.aethelsoft.grooveplayer.data.local.db.entity.TransferFileEntity
import com.aethelsoft.grooveplayer.domain.model.transfer.Transfer
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferFile
import com.aethelsoft.grooveplayer.domain.model.transfer.TransferStatus
import java.util.Date

/**
 * Maps between transfer domain models and Room entities.
 */
object TransferMapper {

    fun mapToDomain(entity: TransferEntity, files: List<TransferFileEntity>): Transfer = Transfer(
        id = entity.id,
        deviceName = entity.deviceName,
        startTime = Date(entity.startTime),
        endTime = entity.endTime?.let { Date(it) },
        totalBytes = entity.totalBytes,
        transferredBytes = entity.transferredBytes,
        overallStatus = parseStatus(entity.overallStatus),
        files = files.map { mapFileToDomain(it) },
    )

    fun mapFileToDomain(entity: TransferFileEntity): TransferFile = TransferFile(
        id = entity.id,
        transferId = entity.transferId,
        fileName = entity.fileName,
        filePath = entity.filePath,
        fileSize = entity.fileSize,
        transferredBytes = entity.transferredBytes,
        checksum = entity.checksum,
        status = parseStatus(entity.status),
        retryCount = entity.retryCount,
    )

    private fun parseStatus(value: String): TransferStatus = try {
        TransferStatus.valueOf(value)
    } catch (e: IllegalArgumentException) {
        TransferStatus.FAILED
    }
}
