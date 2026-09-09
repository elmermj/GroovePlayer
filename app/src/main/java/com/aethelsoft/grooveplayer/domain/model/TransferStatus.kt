package com.aethelsoft.grooveplayer.domain.model.transfer

/**
 * Status of a transfer or transfer file.
 * Used for both overall transfer and per-file progress.
 */
enum class TransferStatus {
    /** Queued, not yet started */
    PENDING,
    /** Establishing connection to remote device */
    CONNECTING,
    /** Actively transferring data */
    TRANSFERRING,
    /** Transfer paused by user */
    PAUSED,
    /** Transfer completed successfully */
    COMPLETED,
    /** Transfer failed (checksum, IO, connection, etc.) */
    FAILED,
    /** Transfer cancelled by user */
    CANCELLED,
    /** Validating file checksum after completion */
    CHECKSUM_VALIDATING,
    /** Retrying after failure (up to 3 times) */
    RETRYING,
}
