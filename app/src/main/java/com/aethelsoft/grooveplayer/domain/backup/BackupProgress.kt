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
    /** Library scan stays at or below this so file upload (5–90) cannot jump backward. */
    const val PREPARING_PERCENT_CAP = 4

    @Suppress("UNUSED_PARAMETER")
    fun status(
        step: BackupJobStep,
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
    ): String = when (step) {
        BackupJobStep.PREPARING -> "Preparing library…"
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
            BackupJobStep.UPLOADING_FILES -> 5 to 90
            BackupJobStep.UPLOADING_CATALOG -> return 95
        }
        if (total <= 0) return end
        val fraction = completed.coerceIn(0, total).toFloat() / total
        return (start + fraction * (end - start)).toInt().coerceIn(0, 99)
    }

    @Suppress("UNUSED_PARAMETER")
    fun failure(
        step: BackupJobStep,
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
        detail: String?,
    ): String {
        val where = when (step) {
            BackupJobStep.PREPARING -> "Backup failed while preparing the library."
            BackupJobStep.UPLOADING_FILES ->
                "Uploading files failed at $uploadCompleted/$uploadTotal."
            BackupJobStep.UPLOADING_CATALOG -> "Uploading catalog failed."
        }
        val safe = when (step) {
            BackupJobStep.UPLOADING_FILES ->
                " The library snapshot was not uploaded."
            BackupJobStep.UPLOADING_CATALOG ->
                " Song files already in the cloud are kept."
            BackupJobStep.PREPARING -> ""
        }
        val why = detail?.takeIf { it.isNotBlank() }?.let { " $it" }.orEmpty()
        return "$where$why$safe Tap Retry."
    }

    @Suppress("UNUSED_PARAMETER")
    fun lines(
        phase: CloudBackupPhase,
        step: BackupJobStep,
        consolidateCompleted: Int,
        consolidateTotal: Int,
        uploadCompleted: Int,
        uploadTotal: Int,
    ): List<BackupProgressLine> {
        return listOf(
            BackupJobStep.UPLOADING_FILES,
            BackupJobStep.UPLOADING_CATALOG,
        ).map { stage ->
            val tone = tone(stage, step, phase)
            val text = when (stage) {
                BackupJobStep.UPLOADING_FILES -> when {
                    tone == BackupProgressTone.PENDING || uploadTotal == 0 -> "Uploading files"
                    else -> "Uploading files $uploadCompleted/$uploadTotal"
                }
                BackupJobStep.UPLOADING_CATALOG -> when (tone) {
                    BackupProgressTone.FAILED -> "Uploading catalog failed"
                    else -> "Uploading catalog"
                }
                BackupJobStep.PREPARING -> "Preparing library…"
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
