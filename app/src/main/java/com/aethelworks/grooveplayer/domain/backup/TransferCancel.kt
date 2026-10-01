package com.aethelworks.grooveplayer.domain.backup

import com.aethelworks.grooveplayer.domain.model.BackupJobStep
import com.aethelworks.grooveplayer.domain.model.CloudBackupPhase
import com.aethelworks.grooveplayer.domain.model.CloudBackupState
import com.aethelworks.grooveplayer.domain.model.isUploadInProgress
import kotlin.coroutines.cancellation.CancellationException

/**
 * User cancel of a running backup or restore.
 * The job gate and backup lease must be released, and the phase must not stay
 * in progress, so the next backup or restore can start.
 * Half-written library files are scratch names only. Finished songs stay.
 */
object TransferCancel {
    const val BACKUP_CANCELLED = "Backup cancelled."
    const val RESTORE_CANCELLED = "Restore cancelled."

    /** Gzip of the Room snapshot written under cache during upload. */
    const val BACKUP_SNAPSHOT_SCRATCH = "backup-room.db.gz"

    fun isCancel(error: Throwable?): Boolean {
        var current = error
        while (current != null) {
            if (current is CancellationException) return true
            current = current.cause
        }
        return false
    }

    /**
     * Idle, not a failure, and not retry-blocked. [CloudBackupState.lastBackupAtEpochMs]
     * is kept so the last successful backup is still known.
     */
    fun backupState(current: CloudBackupState): CloudBackupState = current.copy(
        phase = CloudBackupPhase.IDLE,
        progressPercent = 0,
        bytesPrepared = 0L,
        bytesUploaded = 0L,
        filesTotal = 0,
        filesCompleted = 0,
        consolidateCompleted = 0,
        consolidateTotal = 0,
        uploadCompleted = 0,
        uploadTotal = 0,
        filesDeduped = 0,
        filesSkipped = 0,
        jobStep = BackupJobStep.PREPARING,
        lastError = null,
        message = BACKUP_CANCELLED,
        canRetry = false,
        lastRunDryRun = false,
        otherDeviceHoldingLease = false,
    )

    fun backupCanStartAgain(state: CloudBackupState): Boolean =
        !state.phase.isUploadInProgress() &&
            !state.canRetry &&
            state.phase == CloudBackupPhase.IDLE &&
            state.lastError == null

    /**
     * A swap that already started must keep its phase so cold start can finish
     * or roll it back. Every earlier phase returns to idle so cancel does not
     * resume a download the user stopped.
     */
    fun restorePhaseAfterCancel(phase: RestorePhase): RestorePhase = when (phase) {
        RestorePhase.SWAPPING, RestorePhase.COMMITTED -> phase
        else -> RestorePhase.IDLE
    }

    fun isBackupSnapshotScratch(name: String): Boolean = name == BACKUP_SNAPSHOT_SCRATCH

    /** Scratch inside the app-private library. Complete audio names are not included. */
    fun halfWrittenLibraryNames(names: Iterable<String>): List<String> =
        names.filter { AppPrivateLibrary.isScratchFile(it) }.sorted()
}

/** Shown when a cloud snapshot exists and restore can be started. */
object RestoreOfferCopy {
    fun available(songCount: Int, songBytes: Long): String {
        val songs = if (songCount == 1) "1 song" else "$songCount songs"
        val size = RestoreProgressLabel.formatBytes(songBytes)
        return "Restore is available · $songs · $size in the cloud. " +
            "Songs that are not on this device are downloaded into the app library " +
            "and appear after restore."
    }
}
