package com.aethelsoft.grooveplayer.domain.model.transfer

import java.util.Date

/**
 * Domain model for a transfer session (sender or receiver).
 */
data class Transfer(
    val id: Long,
    val deviceName: String,
    val startTime: Date,
    val endTime: Date?,
    val totalBytes: Long,
    val transferredBytes: Long,
    val overallStatus: TransferStatus,
    val files: List<TransferFile> = emptyList(),
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) {
            ((100 * transferredBytes) / totalBytes).toInt().coerceIn(0, 100)
        } else 0
}
