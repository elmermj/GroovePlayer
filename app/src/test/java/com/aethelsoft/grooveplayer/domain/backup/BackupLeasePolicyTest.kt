package com.aethelsoft.grooveplayer.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupLeasePolicyTest {

    private val localId = "device-a"
    private val now = 1_700_000_000_000L

    @Test
    fun inactiveLeaseDoesNotBlock() {
        assertFalse(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(active = false),
                localId,
                now,
            ),
        )
    }

    @Test
    fun sameDeviceMayReacquire() {
        assertFalse(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(active = true, deviceId = localId, expiresAtEpochMs = now + 60_000L),
                localId,
                now,
            ),
        )
        assertFalse(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(active = true, deviceId = localId),
                localId,
                now,
                httpStatus = 409,
            ),
        )
    }

    @Test
    fun heldByThisDeviceDoesNotBlock() {
        assertFalse(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(active = true, heldByThisDevice = true, deviceId = "other"),
                localId,
                now,
                httpStatus = 409,
            ),
        )
    }

    @Test
    fun otherDeviceActiveLeaseBlocks() {
        assertTrue(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(
                    active = true,
                    heldByThisDevice = false,
                    deviceId = "device-b",
                    expiresAtEpochMs = now + 30_000L,
                ),
                localId,
                now,
            ),
        )
    }

    @Test
    fun expiredOtherDeviceLeaseDoesNotBlock() {
        assertFalse(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(
                    active = true,
                    deviceId = "device-b",
                    expiresAtEpochMs = now,
                ),
                localId,
                now,
                httpStatus = 409,
            ),
        )
    }

    @Test
    fun conflictWithoutBodyBlocks() {
        assertTrue(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(),
                localId,
                now,
                httpStatus = 409,
            ),
        )
    }

    @Test
    fun explicitNotHeldBlocksEvenWithoutHolderId() {
        assertTrue(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(active = true, heldByThisDevice = false),
                localId,
                now,
            ),
        )
    }

    @Test
    fun activeWithoutIdentityDoesNotBlock() {
        assertFalse(
            BackupLeasePolicy.blocksThisDevice(
                RemoteBackupLease(active = true),
                localId,
                now,
            ),
        )
    }

    @Test
    fun primaryButtonUsesExactOtherDeviceCopy() {
        assertEquals(
            "Your other device is currently backing up your data",
            BackupPrimaryAction.label(
                otherDeviceHoldingLease = true,
                busy = false,
                canRetry = true,
                progressLabel = "Uploading files 1/2",
            ),
        )
        assertEquals(BackupLeaseCopy.OTHER_DEVICE_BUTTON, BackupPrimaryAction.label(
            otherDeviceHoldingLease = true,
            busy = false,
            canRetry = false,
            progressLabel = "Preparing included folders…",
        ))
    }

    @Test
    fun busyBackupKeepsProgressLabel() {
        assertEquals(
            "Uploading files 1/2",
            BackupPrimaryAction.label(
                otherDeviceHoldingLease = true,
                busy = true,
                canRetry = false,
                progressLabel = "Uploading files 1/2",
            ),
        )
    }

    @Test
    fun retryAndStartLabelsWhenLeaseIsFree() {
        assertEquals(
            "Retry backup",
            BackupPrimaryAction.label(
                otherDeviceHoldingLease = false,
                busy = false,
                canRetry = true,
                progressLabel = "Preparing included folders…",
            ),
        )
        assertEquals(
            "Back up now",
            BackupPrimaryAction.label(
                otherDeviceHoldingLease = false,
                busy = false,
                canRetry = false,
                progressLabel = "Preparing included folders…",
            ),
        )
    }

    @Test
    fun otherDeviceDisablesPrimaryButton() {
        assertFalse(
            BackupPrimaryAction.enabled(
                otherDeviceHoldingLease = true,
                busy = false,
                canStart = true,
                canRetry = true,
            ),
        )
        assertTrue(
            BackupPrimaryAction.enabled(
                otherDeviceHoldingLease = false,
                busy = false,
                canStart = true,
                canRetry = false,
            ),
        )
        assertFalse(
            BackupPrimaryAction.enabled(
                otherDeviceHoldingLease = false,
                busy = true,
                canStart = false,
                canRetry = false,
            ),
        )
    }
}
