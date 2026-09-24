package com.aethelsoft.grooveplayer.domain.backup

/**
 * Cross-device backup lease (SCRUM-73).
 *
 * Server: POST/GET `/v1/backup/lease`, heartbeat, and release.
 * TTL is 600s. Heartbeat about every [DEFAULT_HEARTBEAT_INTERVAL_MS].
 * The primary backup control uses [BackupLeaseCopy.OTHER_DEVICE_BUTTON] when
 * [BackupLeasePolicy.blocksThisDevice] is true (`other_device_active` or
 * `code` [CODE_OTHER_DEVICE]).
 */
object BackupLeaseCopy {
    const val OTHER_DEVICE_BUTTON = "Your other device is currently backing up your data"
}

/**
 * Lease snapshot from GET/POST `/v1/backup/lease`.
 * [heldByThisDevice] is null when the server omitted the field.
 * A 409 does not include the holder's device id.
 */
data class RemoteBackupLease(
    val active: Boolean = false,
    val heldByThisDevice: Boolean? = null,
    val otherDeviceActive: Boolean = false,
    val code: String? = null,
    val deviceId: String? = null,
    val expiresAtEpochMs: Long? = null,
)

object BackupLeasePolicy {
    const val CODE_OTHER_DEVICE = "OTHER_DEVICE_BACKUP"
    const val CODE_NOT_FOUND = "LEASE_NOT_FOUND"

    /** Server `heartbeat_interval_sec` when the body omits it. TTL is 600s. */
    const val DEFAULT_HEARTBEAT_INTERVAL_MS = 120_000L

    fun heartbeatIntervalMs(heartbeatIntervalSec: Int?): Long {
        val seconds = heartbeatIntervalSec ?: return DEFAULT_HEARTBEAT_INTERVAL_MS
        if (seconds <= 0) return DEFAULT_HEARTBEAT_INTERVAL_MS
        return seconds * 1000L
    }

    /**
     * True when this install must not start backup.
     * `other_device_active` or [CODE_OTHER_DEVICE] disables the button.
     * The same [localDeviceId] may re-acquire (200, same lease_id) and is never blocked.
     * An expiry at or before [nowEpochMs] is not a live lock.
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
        if (lease.code == CODE_OTHER_DEVICE) return true
        if (lease.otherDeviceActive) return true
        if (httpStatus == 409) return true
        return lease.active && holder != null && holder != localDeviceId
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
