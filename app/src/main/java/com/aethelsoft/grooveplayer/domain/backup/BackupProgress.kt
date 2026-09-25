package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.domain.model.BackupJobStep
import com.aethelsoft.grooveplayer.domain.model.CloudBackupPhase

enum class BackupProgressTone {
    PENDING,
    ACTIVE,
    DONE,
    FAILED,
}

data class BackupProgressLine(
    val text: String,
    val tone: BackupProgressTone,
)

object BackupProgressLabel {
    /** Folder scan stays at or below this so consolidate (5–40) cannot jump backward. */
    const val PREPARING_PERCENT_CAP = 4

    fun status(
        step: BackupJobStep,
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
    ): String = when (step) {
        BackupJobStep.PREPARING -> "Preparing included folders…"
        BackupJobStep.CONSOLIDATING ->
            "Consolidating $consolidateCompleted/$consolidateTotal files"
        BackupJobStep.UPLOADING_FILES -> "Uploading files $uploadCompleted/$uploadTotal"
        BackupJobStep.UPLOADING_CATALOG -> "Uploading catalog"
    }

    fun percent(
        step: BackupJobStep,
        completed: Int,
        total: Int,
    ): Int {
        val (start, end) = when (step) {
            BackupJobStep.PREPARING -> return PREPARING_PERCENT_CAP
            BackupJobStep.CONSOLIDATING -> 5 to 40
            BackupJobStep.UPLOADING_FILES -> 40 to 90
            BackupJobStep.UPLOADING_CATALOG -> return 95
        }
        if (total <= 0) return end
        val fraction = completed.coerceIn(0, total).toFloat() / total
        return (start + fraction * (end - start)).toInt().coerceIn(0, 99)
    }

    /**
     * Consolidate band is 5–40. [bytesDone] moves the bar inside that band while a file
     * is still hashing or copying, so one large song is not stuck until it finishes.
     */
    fun consolidatePercent(bytesDone: Long, bytesTotal: Long): Int {
        if (bytesTotal <= 0L) return 5
        val done = bytesDone.coerceAtLeast(0L).coerceAtMost(bytesTotal)
        val fraction = done.toDouble() / bytesTotal.toDouble()
        return (5.0 + fraction * 35.0).toInt().coerceIn(5, 40)
    }

    fun failure(
        step: BackupJobStep,
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
        detail: String?,
    ): String {
        val where = when (step) {
            BackupJobStep.PREPARING -> "Backup failed while preparing folders."
            BackupJobStep.CONSOLIDATING ->
                "Consolidating failed at $consolidateCompleted/$consolidateTotal files."
            BackupJobStep.UPLOADING_FILES ->
                "Uploading files failed at $uploadCompleted/$uploadTotal."
            BackupJobStep.UPLOADING_CATALOG -> "Uploading catalog failed."
        }
        val safe = when (step) {
            BackupJobStep.CONSOLIDATING, BackupJobStep.UPLOADING_FILES ->
                " The library snapshot was not uploaded."
            BackupJobStep.UPLOADING_CATALOG ->
                " Song files already in the cloud are kept."
            BackupJobStep.PREPARING -> ""
        }
        val why = detail?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        return "$where$why$safe Tap Retry."
    }

    fun lines(
        phase: CloudBackupPhase,
        step: BackupJobStep,
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
    ): List<BackupProgressLine> {
        return listOf(
            BackupJobStep.CONSOLIDATING,
            BackupJobStep.UPLOADING_FILES,
            BackupJobStep.UPLOADING_CATALOG,
        ).map { stage ->
            val tone = tone(stage, step, phase)
            val text = when (stage) {
                BackupJobStep.CONSOLIDATING -> when {
                    tone == BackupProgressTone.PENDING || consolidateTotal == 0 -> "Consolidating"
                    else -> "Consolidating $consolidateCompleted/$consolidateTotal files"
                }
                BackupJobStep.UPLOADING_FILES -> when {
                    tone == BackupProgressTone.PENDING || uploadTotal == 0 -> "Uploading files"
                    else -> "Uploading files $uploadCompleted/$uploadTotal"
                }
                BackupJobStep.UPLOADING_CATALOG -> when (tone) {
                    BackupProgressTone.FAILED -> "Uploading catalog failed"
                    else -> "Uploading catalog"
                }
                BackupJobStep.PREPARING -> "Preparing included folders…"
            }
            BackupProgressLine(text, tone)
        }
    }

    private fun tone(
        stage: BackupJobStep,
        current: BackupJobStep,
        phase: CloudBackupPhase,
    ): BackupProgressTone {
        if (phase == CloudBackupPhase.SUCCESS) return BackupProgressTone.DONE
        if (phase == CloudBackupPhase.ERROR && stage == current) return BackupProgressTone.FAILED
        if (stage.ordinal < current.ordinal) return BackupProgressTone.DONE
        if (stage == current && phase != CloudBackupPhase.IDLE) return BackupProgressTone.ACTIVE
        return BackupProgressTone.PENDING
    }
}

/**
 * Byte budget for copying approved songs into the app library.
 * Bytes only increase, so the bar cannot move backward inside one backup run.
 * A new run starts a new instance after the preparing step resets the UI to 0.
 */
class ConsolidateByteProgress(
    val fileCount: Int,
    plannedBytes: Long,
) {
    private val planned = plannedBytes.coerceAtLeast(1L)
    private var bytesDone = 0L
    private var opRead = 0L

    /** Files whose consolidate work has started. Stays 0 while the app library is indexed. */
    var filesShown: Int = 0
        private set

    fun percent(): Int = BackupProgressLabel.consolidatePercent(bytesDone, planned)

    fun beginOperation() {
        opRead = 0L
    }

    /** [bytesRead] is the absolute offset within the current hash or copy. */
    fun onAbsoluteRead(bytesRead: Long) {
        val read = bytesRead.coerceAtLeast(0L)
        val delta = (read - opRead).coerceAtLeast(0L)
        opRead = read
        bytesDone += delta
    }

    /** Count work that will not be performed, such as a skipped copy of a reused file. */
    fun credit(bytes: Long) {
        if (bytes > 0L) bytesDone += bytes
    }

    fun showFile(oneBasedInclusive: Int) {
        filesShown = oneBasedInclusive.coerceIn(0, fileCount.coerceAtLeast(0))
    }

    fun complete() {
        if (bytesDone < planned) bytesDone = planned
        filesShown = fileCount
    }
}
