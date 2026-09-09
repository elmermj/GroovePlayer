package com.aethelsoft.grooveplayer.domain.model.transfer

/**
 * Domain model for a single file in a transfer.
 */
data class TransferFile(
    val id: Long,
    val transferId: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val transferredBytes: Long,
    val checksum: String?,
    val status: TransferStatus,
    val retryCount: Int,
) {
    val progressPercent: Int
        get() = if (fileSize > 0) {
            ((100 * transferredBytes) / fileSize).toInt().coerceIn(0, 100)
        } else 0
}
