package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.domain.model.BackupJobStep
import com.aethelsoft.grooveplayer.domain.model.CloudBackupPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupProgressLabelTest {

    @Test
    fun consolidateUploadAndCatalogLabelsStayDistinct() {
        assertEquals(
            "Consolidating 2/5 files",
            BackupProgressLabel.status(BackupJobStep.CONSOLIDATING, 2, 5, 0, 0),
        )
        assertEquals(
            "Uploading files 1/5",
            BackupProgressLabel.status(BackupJobStep.UPLOADING_FILES, 5, 5, 1, 5),
        )
        assertEquals(
            "Uploading catalog",
            BackupProgressLabel.status(BackupJobStep.UPLOADING_CATALOG, 5, 5, 5, 5),
        )
    }

    @Test
    fun linesMarkOnlyTheActiveStageUntilSuccess() {
        val duringUpload = BackupProgressLabel.lines(
            phase = CloudBackupPhase.UPLOADING,
            step = BackupJobStep.UPLOADING_FILES,
            consolidateCompleted = 3,
            consolidateTotal = 3,
            uploadCompleted = 1,
            uploadTotal = 3,
        )
        assertEquals("Consolidating 3/3 files", duringUpload[0].text)
        assertEquals(BackupProgressTone.DONE, duringUpload[0].tone)
        assertEquals("Uploading files 1/3", duringUpload[1].text)
        assertEquals(BackupProgressTone.ACTIVE, duringUpload[1].tone)
        assertEquals(BackupProgressTone.PENDING, duringUpload[2].tone)

        val failed = BackupProgressLabel.lines(
            phase = CloudBackupPhase.ERROR,
            step = BackupJobStep.CONSOLIDATING,
            consolidateCompleted = 1,
            consolidateTotal = 4,
            uploadCompleted = 0,
            uploadTotal = 0,
        )
        assertEquals(BackupProgressTone.FAILED, failed[0].tone)
        assertEquals(BackupProgressTone.PENDING, failed[1].tone)
        assertTrue(
            BackupProgressLabel.failure(
                BackupJobStep.CONSOLIDATING, 1, 4, 0, 0, "disk full",
            ).contains("library snapshot was not uploaded"),
        )
        assertTrue(
            BackupProgressLabel.failure(
                BackupJobStep.UPLOADING_CATALOG, 4, 4, 4, 4, null,
            ).contains("Tap Retry"),
        )
    }

    @Test
    fun percentAdvancesThroughConsolidateThenFilesThenCatalog() {
        assertTrue(
            BackupProgressLabel.percent(BackupJobStep.CONSOLIDATING, 0, 4) <
                BackupProgressLabel.percent(BackupJobStep.CONSOLIDATING, 4, 4),
        )
        assertTrue(
            BackupProgressLabel.percent(BackupJobStep.CONSOLIDATING, 4, 4) <=
                BackupProgressLabel.percent(BackupJobStep.UPLOADING_FILES, 0, 4),
        )
        assertTrue(
            BackupProgressLabel.percent(BackupJobStep.UPLOADING_FILES, 4, 4) <
                BackupProgressLabel.percent(BackupJobStep.UPLOADING_CATALOG, 1, 1),
        )
    }
}
