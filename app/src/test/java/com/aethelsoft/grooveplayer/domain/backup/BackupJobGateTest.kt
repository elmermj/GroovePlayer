package com.aethelsoft.grooveplayer.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJobGateTest {

    @Test
    fun secondTransferDoesNotStartUntilTheFirstReleases() {
        val gate = BackupJobGate()
        assertTrue(gate.tryAcquire(BackupTransfer.BACKUP))
        assertFalse(gate.tryAcquire(BackupTransfer.RESTORE))
        assertFalse(gate.tryAcquire(BackupTransfer.BACKUP))
        assertEquals(BackupTransfer.BACKUP, gate.current())

        gate.release(BackupTransfer.RESTORE)
        assertEquals(BackupTransfer.BACKUP, gate.current())

        gate.release(BackupTransfer.BACKUP)
        assertNull(gate.current())
        assertTrue(gate.tryAcquire(BackupTransfer.RESTORE))
        assertFalse(gate.tryAcquire(BackupTransfer.BACKUP))
    }
}
