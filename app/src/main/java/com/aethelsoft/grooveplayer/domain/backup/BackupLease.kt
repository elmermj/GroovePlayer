package com.aethelsoft.grooveplayer.domain.backup

/**
 * Cross-device backup lease (SCRUM-73).
 *
 * One Premium account may consolidate/upload from only one install at a time.
 * The primary backup control uses [BackupLeaseCopy.OTHER_DEVICE_BUTTON] when
 * [BackupLeasePolicy.blocksThisDevice] is true.
 */
object BackupLeaseCopy {
    const val OTHER_DEVICE_BUTTON = "Your other device is currently backing up your data"
}

/**
 * Lease snapshot from GET/POST `/v1/backup/lease`.
 * [heldByThisDevice] is null when the server omitted the field.
 */
data class RemoteBackupLease(
    val active: Boolean = false,
    val heldByThisDevice: Boolean? = null,
    val deviceId: String? = null,
    val expiresAtEpochMs: Long? = null,
)

object BackupLeasePolicy {
    /**
     * Heartbeat while consolidating and uploading.
     * Assumed server TTL is at least 45s so one delayed beat does not expire the lease.
     */
    const val HEARTBEAT_INTERVAL_MS = 15_000L

    /**
     * True when this install must not start backup.
     * HTTP 409 is a conflict unless the body says this device holds the lease or it has expired.
     * The same [localDeviceId] may re-acquire and is never treated as blocked.
     */
    fun blocksThisDevice(
        lease: RemoteBackupLease,
        localDeviceId: String,
        nowEpochMs: Long,
        httpStatus: Int? = null,
    ): Boolean {
        if (lease.heldByThisDevice == true) return false
        val holder = lease.deviceId?.takeIf { it.isNotBlank() }
        if (holder != null && holder == localDeviceId) return false
        val expiresAt = lease.expiresAtEpochMs
        if (expiresAt != null && expiresAt <= nowEpochMs) return false
        if (httpStatus == 409) return true
        if (!lease.active) return false
        if (lease.heldByThisDevice == false) return true
        return holder != null && holder != localDeviceId
    }
}

/** Label and enabled state for the primary Back up now / Retry backup control. */
object BackupPrimaryAction {
    fun label(
        otherDeviceHoldingLease: Boolean,
        busy: Boolean,
        canRetry: Boolean,
        progressLabel: String,
    ): String = when {
        otherDeviceHoldingLease && !busy -> BackupLeaseCopy.OTHER_DEVICE_BUTTON
        busy -> progressLabel
        canRetry -> "Retry backup"
        else -> "Back up now"
    }

    fun enabled(
        otherDeviceHoldingLease: Boolean,
        busy: Boolean,
        canStart: Boolean,
        canRetry: Boolean,
    ): Boolean = !otherDeviceHoldingLease && !busy && (canStart || canRetry)
}
