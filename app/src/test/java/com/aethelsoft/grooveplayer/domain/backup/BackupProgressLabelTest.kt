package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.domain.model.BackupJobStep
import com.aethelsoft.grooveplayer.domain.model.CloudBackupPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupProgressLabelTest {

    @Test
    fun uploadAndCatalogLabelsStayDistinct() {
        assertEquals(
            "Preparing library…",
            BackupProgressLabel.status(BackupJobStep.PREPARING, 0, 0, 0, 0),
        )
        assertEquals(
            "Uploading files 1/5",
            BackupProgressLabel.status(BackupJobStep.UPLOADING_FILES, 0, 0, 1, 5),
        )
        assertEquals(
            "Uploading catalog",
            BackupProgressLabel.status(BackupJobStep.UPLOADING_CATALOG, 0, 0, 5, 5),
        )
    }

    @Test
    fun linesMarkOnlyTheActiveStageUntilSuccess() {
        val duringUpload = BackupProgressLabel.lines(
            phase = CloudBackupPhase.UPLOADING,
            step = BackupJobStep.UPLOADING_FILES,
            consolidateCompleted = 0,
            consolidateTotal = 0,
            uploadCompleted = 1,
            uploadTotal = 3,
        )
        assertEquals("Uploading files 1/3", duringUpload[0].text)
        assertEquals(BackupProgressTone.ACTIVE, duringUpload[0].tone)
        assertEquals(BackupProgressTone.PENDING, duringUpload[1].tone)

        val failed = BackupProgressLabel.lines(
            phase = CloudBackupPhase.ERROR,
            step = BackupJobStep.UPLOADING_FILES,
            consolidateCompleted = 0,
            consolidateTotal = 0,
            uploadCompleted = 1,
            uploadTotal = 4,
        )
        assertEquals(BackupProgressTone.FAILED, failed[0].tone)
        assertEquals(BackupProgressTone.PENDING, failed[1].tone)
        assertTrue(
            BackupProgressLabel.failure(
                BackupJobStep.UPLOADING_FILES, 0, 0, 1, 4, "disk full",
            ).contains("library snapshot was not uploaded"),
        )
        assertTrue(
            BackupProgressLabel.failure(
                BackupJobStep.UPLOADING_CATALOG, 0, 0, 4, 4, null,
            ).contains("Tap Retry"),
        )
    }

    @Test
    fun percentAdvancesThroughFilesThenCatalog() {
        assertEquals(4, BackupProgressLabel.PREPARING_PERCENT_CAP)
        assertTrue(
            BackupProgressLabel.percent(BackupJobStep.PREPARING, 0, 0) <
                BackupProgressLabel.percent(BackupJobStep.UPLOADING_FILES, 0, 4),
        )
        assertTrue(
            BackupProgressLabel.percent(BackupJobStep.UPLOADING_FILES, 0, 4) <
                BackupProgressLabel.percent(BackupJobStep.UPLOADING_FILES, 4, 4),
        )
        assertTrue(
            BackupProgressLabel.percent(BackupJobStep.UPLOADING_FILES, 4, 4) <
                BackupProgressLabel.percent(BackupJobStep.UPLOADING_CATALOG, 1, 1),
        )
    }
}
