package com.aethelworks.grooveplayer.domain.backup

import com.aethelworks.grooveplayer.domain.model.BackupJobStep
import com.aethelworks.grooveplayer.domain.model.CloudBackupPhase
import com.aethelworks.grooveplayer.domain.model.CloudBackupState
import com.aethelworks.grooveplayer.domain.model.isUploadInProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class TransferCancelTest {

    @Test
    fun cancelledBackupIsIdleAndCanBeStartedAgain() {
        val running = CloudBackupState(
            phase = CloudBackupPhase.UPLOADING,
            progressPercent = 40,
            bytesPrepared = 80L,
            bytesUploaded = 20L,
            filesTotal = 4,
            filesCompleted = 1,
            uploadCompleted = 1,
            uploadTotal = 4,
            jobStep = BackupJobStep.UPLOADING_FILES,
            filesSkipped = 1,
            lastBackupAtEpochMs = 42L,
            lastError = "old",
            includedFolders = listOf("groove-library"),
            message = "Uploading files 1/4",
            canRetry = true,
            lastRunDryRun = true,
            otherDeviceHoldingLease = true,
        )

        val next = TransferCancel.backupState(running)

        assertTrue(TransferCancel.backupCanStartAgain(next))
        assertFalse(next.phase.isUploadInProgress())
        assertEquals(CloudBackupPhase.IDLE, next.phase)
        assertEquals(TransferCancel.BACKUP_CANCELLED, next.message)
        assertNull(next.lastError)
        assertFalse(next.canRetry)
        assertFalse(next.otherDeviceHoldingLease)
        assertFalse(next.lastRunDryRun)
        assertEquals(0, next.progressPercent)
        assertEquals(0, next.uploadCompleted)
        assertEquals(0L, next.bytesUploaded)
        assertEquals(42L, next.lastBackupAtEpochMs)
        assertEquals(listOf("groove-library"), next.includedFolders)
        assertFalse(next.message!!.contains("Tap Retry"))
        assertEquals(
            "Back up now",
            BackupPrimaryAction.label(
                otherDeviceHoldingLease = next.otherDeviceHoldingLease,
                busy = next.phase.isUploadInProgress(),
                canRetry = next.canRetry,
                progressLabel = "Uploading files 1/4",
            ),
        )
        assertTrue(
            BackupPrimaryAction.enabled(
                otherDeviceHoldingLease = false,
                busy = next.phase.isUploadInProgress(),
                canStart = true,
                canRetry = next.canRetry,
            ),
        )
    }

    @Test
    fun ordinaryIoFailureIsNotAUserCancel() {
        assertFalse(TransferCancel.isCancel(IOException("connection abort")))
        assertFalse(TransferCancel.isCancel(IllegalStateException("quota")))
        assertTrue(TransferCancel.isCancel(CancellationException(TransferCancel.BACKUP_CANCELLED)))
        assertTrue(
            TransferCancel.isCancel(
                IllegalStateException("wrapped", CancellationException(TransferCancel.RESTORE_CANCELLED)),
            ),
        )
    }

    @Test
    fun cancelReleasesTheJobGateSoAnotherTransferCanStart() {
        val gate = BackupJobGate()
        assertTrue(gate.tryAcquire(BackupTransfer.BACKUP))
        val next = TransferCancel.backupState(
            CloudBackupState(phase = CloudBackupPhase.PREPARING),
        )
        assertTrue(TransferCancel.backupCanStartAgain(next))
        gate.release(BackupTransfer.BACKUP)
        assertNull(gate.current())
        assertTrue(gate.tryAcquire(BackupTransfer.RESTORE))
        gate.release(BackupTransfer.RESTORE)
        assertNull(gate.current())
    }

    @Test
    fun cancelledRestoreReturnsToIdleUnlessTheSwapAlreadyStarted() {
        assertEquals(RestorePhase.IDLE, TransferCancel.restorePhaseAfterCancel(RestorePhase.DOWNLOADING))
        assertEquals(RestorePhase.IDLE, TransferCancel.restorePhaseAfterCancel(RestorePhase.FILES_READY))
        assertEquals(RestorePhase.IDLE, TransferCancel.restorePhaseAfterCancel(RestorePhase.STAGED))
        assertEquals(RestorePhase.SWAPPING, TransferCancel.restorePhaseAfterCancel(RestorePhase.SWAPPING))
        assertEquals(RestorePhase.COMMITTED, TransferCancel.restorePhaseAfterCancel(RestorePhase.COMMITTED))

        val afterCancel = TransferCancel.restorePhaseAfterCancel(RestorePhase.DOWNLOADING)
        assertEquals(
            ColdStartAction.OPEN_HOME,
            RestoreApplyRecovery.coldStartAction(afterCancel, StagingVerdict.VALID),
        )
        assertEquals(
            ColdStartAction.RESUME_DOWNLOAD,
            RestoreApplyRecovery.coldStartAction(RestorePhase.DOWNLOADING, null),
        )
    }

    @Test
    fun cancelDeletesHalfWrittenLibraryFilesAndKeepsFinishedSongs() {
        val dir = File(System.getProperty("java.io.tmpdir"), "groove-cancel-${System.nanoTime()}")
        assertTrue(dir.mkdirs())
        try {
            File(dir, "song.mp3").writeText("audio")
            File(dir, "song.mp3.tmp").writeText("partial-copy")
            File(dir, "abc.partial").writeText("partial-download")
            File(dir, ".groove_write_1.tmp").writeText("probe")
            File(dir, TransferCancel.BACKUP_SNAPSHOT_SCRATCH).writeText("snapshot")

            val doomed = TransferCancel.halfWrittenLibraryNames(dir.list()!!.toList())
            doomed.forEach { name -> File(dir, name).delete() }
            if (TransferCancel.isBackupSnapshotScratch(TransferCancel.BACKUP_SNAPSHOT_SCRATCH)) {
                File(dir, TransferCancel.BACKUP_SNAPSHOT_SCRATCH).delete()
            }

            assertTrue(File(dir, "song.mp3").isFile)
            assertEquals("audio", File(dir, "song.mp3").readText())
            assertFalse(File(dir, "song.mp3.tmp").exists())
            assertFalse(File(dir, "abc.partial").exists())
            assertFalse(File(dir, ".groove_write_1.tmp").exists())
            assertFalse(File(dir, TransferCancel.BACKUP_SNAPSHOT_SCRATCH).exists())
            assertFalse(doomed.contains("song.mp3"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun restorePhasesMoveFromPreparingThroughFinishing() {
        val progress = RestoreProgress()
        var lines = RestoreProgressLabel.phaseLines(progress.snapshot())
        assertEquals("Preparing", lines[0].text)
        assertEquals(BackupProgressTone.ACTIVE, lines[0].tone)
        assertEquals(BackupProgressTone.PENDING, lines[1].tone)
        assertEquals(BackupProgressTone.PENDING, lines[3].tone)

        progress.onLibrarySnapshot(10L, 100L)
        lines = RestoreProgressLabel.phaseLines(progress.snapshot())
        assertEquals(RestoreVisiblePhase.PREPARING, RestoreProgressLabel.visiblePhase(progress.snapshot()))
        assertEquals(BackupProgressTone.ACTIVE, lines[0].tone)

        progress.planDownloads(listOf(100L, 100L))
        progress.beginFile(0)
        lines = RestoreProgressLabel.phaseLines(progress.snapshot())
        assertEquals(BackupProgressTone.DONE, lines[0].tone)
        assertEquals("Downloading 1 of 2", lines[1].text)
        assertEquals(BackupProgressTone.ACTIVE, lines[1].tone)
        assertEquals(BackupProgressTone.PENDING, lines[2].tone)

        progress.finishFile(0)
        progress.beginFile(1)
        progress.finishFile(1)
        progress.showVerifyingFiles()
        lines = RestoreProgressLabel.phaseLines(progress.snapshot())
        assertEquals("Verifying 2 of 2", lines[2].text)
        assertEquals(BackupProgressTone.ACTIVE, lines[2].tone)
        assertEquals(BackupProgressTone.DONE, lines[1].tone)

        progress.onApplying()
        lines = RestoreProgressLabel.phaseLines(progress.snapshot())
        assertEquals("Finishing", lines[3].text)
        assertEquals(BackupProgressTone.ACTIVE, lines[3].tone)
        assertEquals(BackupProgressTone.DONE, lines[2].tone)
        assertEquals(RestoreProgressLabel.APPLYING, progress.snapshot().status)
    }

    @Test
    fun failedDownloadMarksTheActivePhaseAndKeepsTheRetryLabel() {
        val progress = RestoreProgress()
        progress.planDownloads(listOf(100L, 100L))
        progress.beginFile(0)
        progress.onRetry(0, retryNumber = 2, maxAttempts = 3)
        val snap = progress.snapshot()
        assertEquals("Retrying (2/3)…", snap.retry)
        assertEquals("Downloading 1 of 2 files", snap.status)

        val lines = RestoreProgressLabel.phaseLines(snap, failed = true)
        assertEquals(BackupProgressTone.DONE, lines[0].tone)
        assertEquals(BackupProgressTone.FAILED, lines[1].tone)
        assertEquals("Downloading 1 of 2", lines[1].text)
        assertEquals(BackupProgressTone.PENDING, lines[2].tone)
        assertEquals(BackupProgressTone.PENDING, lines[3].tone)
    }

    @Test
    fun restoreOfferNamesCloudSongsAndSaysTheyAppearAfterRestore() {
        val text = RestoreOfferCopy.available(songCount = 3, songBytes = 5_242_880L)
        assertTrue(text.contains("Restore is available"))
        assertTrue(text.contains("3 songs"))
        assertTrue(text.contains("5.0 MB"))
        assertTrue(text.contains("not on this device"))
        assertTrue(text.contains("appear after restore"))
        assertEquals(
            "Restore is available · 1 song · 0 B in the cloud. " +
                "Songs that are not on this device are downloaded into the app library " +
                "and appear after restore.",
            RestoreOfferCopy.available(1, 0L),
        )
    }
}
