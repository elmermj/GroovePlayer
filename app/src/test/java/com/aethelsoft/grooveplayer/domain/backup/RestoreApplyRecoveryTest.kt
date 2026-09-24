package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.domain.model.VisualizationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreApplyRecoveryTest {

    @Test
    fun sqliteHeaderAndUserVersionAreReadBigEndian() {
        val header = sqliteHeader(userVersion = 16, size = 100)
        assertTrue(RestoreApplyRecovery.isSqliteHeader(header))
        assertEquals(16, RestoreApplyRecovery.readUserVersion(header))
        assertEquals(
            StagingVerdict.VALID,
            RestoreApplyRecovery.verdict(fileSize = 4096, header = header, localSchema = 16),
        )
    }

    @Test
    fun olderSnapshotIsValidAndNewerSnapshotIsRejected() {
        val older = sqliteHeader(userVersion = 15, size = 100)
        assertEquals(
            StagingVerdict.VALID,
            RestoreApplyRecovery.verdict(fileSize = 4096, header = older, localSchema = 16),
        )
        val newer = sqliteHeader(userVersion = 17, size = 100)
        assertEquals(
            StagingVerdict.NEWER_THAN_APP,
            RestoreApplyRecovery.verdict(fileSize = 4096, header = newer, localSchema = 16),
        )
    }

    @Test
    fun truncatedOrNonSqliteFilesAreNotApplied() {
        val short = sqliteHeader(userVersion = 16, size = 40)
        assertEquals(
            StagingVerdict.TRUNCATED,
            RestoreApplyRecovery.verdict(fileSize = 40, header = short, localSchema = 16),
        )
        val garbage = ByteArray(100) { 7 }
        assertEquals(
            StagingVerdict.NOT_SQLITE,
            RestoreApplyRecovery.verdict(fileSize = 100, header = garbage, localSchema = 16),
        )
        assertEquals(
            StagingVerdict.NOT_SQLITE,
            RestoreApplyRecovery.verdict(
                fileSize = 4096,
                header = sqliteHeader(userVersion = 0, size = 100),
                localSchema = 16,
            ),
        )
    }

    @Test
    fun coldStartResumesOnlyAValidatedStagedSnapshot() {
        assertEquals(
            ColdStartAction.OPEN_HOME,
            RestoreApplyRecovery.coldStartAction(RestorePhase.IDLE, null),
        )
        assertEquals(
            ColdStartAction.DISCARD_AND_HOME,
            RestoreApplyRecovery.coldStartAction(RestorePhase.DOWNLOADING, StagingVerdict.VALID),
        )
        assertEquals(
            ColdStartAction.RESUME_APPLY,
            RestoreApplyRecovery.coldStartAction(RestorePhase.STAGED, StagingVerdict.VALID),
        )
        assertEquals(
            ColdStartAction.RESUME_APPLY,
            RestoreApplyRecovery.coldStartAction(RestorePhase.APPLYING, StagingVerdict.VALID),
        )
        assertEquals(
            ColdStartAction.DISCARD_AND_HOME,
            RestoreApplyRecovery.coldStartAction(RestorePhase.APPLYING, StagingVerdict.TRUNCATED),
        )
        assertEquals(
            ColdStartAction.DISCARD_AND_HOME,
            RestoreApplyRecovery.coldStartAction(RestorePhase.STAGED, null),
        )
        assertEquals(
            ColdStartAction.DISCARD_AND_HOME,
            RestoreApplyRecovery.coldStartAction(RestorePhase.APPLYING, StagingVerdict.NEWER_THAN_APP),
        )
    }

    @Test
    fun unknownPhaseAndLegacyRestartFlagDoNotResume() {
        assertEquals(RestorePhase.IDLE, RestoreApplyRecovery.parsePhase(null))
        assertEquals(RestorePhase.IDLE, RestoreApplyRecovery.parsePhase("needs_restart_after_library_restore"))
        assertEquals(RestorePhase.APPLYING, RestoreApplyRecovery.parsePhase("APPLYING"))
        assertEquals(
            ColdStartAction.OPEN_HOME,
            RestoreApplyRecovery.coldStartAction(
                RestoreApplyRecovery.parsePhase("needs_restart_after_library_restore"),
                StagingVerdict.VALID,
            ),
        )
    }

    @Test
    fun corruptRoomFileIsUnrecoverableButClosedPoolIsNot() {
        assertTrue(
            RestoreApplyRecovery.isUnrecoverableDatabaseFailure(
                IllegalStateException(
                    "Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number.",
                ),
            ),
        )
        assertTrue(
            RestoreApplyRecovery.isUnrecoverableDatabaseFailure(
                IllegalStateException("file is not a database (code 26 SQLITE_NOTADB)"),
            ),
        )
        assertTrue(
            RestoreApplyRecovery.isUnrecoverableDatabaseFailure(
                RuntimeException("database disk image is malformed"),
            ),
        )
        assertTrue(
            RestoreApplyRecovery.isUnrecoverableDatabaseFailure(
                IllegalStateException("A migration from 16 to 15 was required but not found. Cannot downgrade database from version 17 to 16"),
            ),
        )
        assertFalse(
            RestoreApplyRecovery.isUnrecoverableDatabaseFailure(
                IllegalStateException(
                    "Cannot perform this operation because the connection pool has been closed.",
                ),
            ),
        )
    }

    @Test
    fun settingsRowFillsMissingColumnsFromDefaults() {
        val settings = restoredSettings(
            mapOf(
                "uiStyleId" to "amoled",
                "equalizerEnabled" to "1",
                "notificationsEnabled" to "0",
                "excludedFolders" to "/Music/Voice||/Music/Ringtones",
                "visualizationMode" to "OFF",
            ),
        )
        assertEquals(1, settings.id)
        assertEquals("amoled", settings.uiStyleId)
        assertTrue(settings.equalizerEnabled)
        assertFalse(settings.notificationsEnabled)
        assertEquals(listOf("/Music/Voice", "/Music/Ringtones"), settings.excludedFolders)
        assertEquals(VisualizationMode.OFF, settings.visualizationMode)
        assertEquals(3, settings.lastPlayedSongsTimer)
        assertEquals("OFF", settings.repeatMode)
    }

    @Test
    fun playbackAndMetadataRowsWithoutSongIdAreDropped() {
        assertNull(restoredPlayback(mapOf("songTitle" to "Track")))
        val playback = restoredPlayback(
            mapOf(
                "songId" to "42",
                "songTitle" to "Track",
                "artist" to "Ada",
                "playedAt" to "10",
            ),
        )
        assertEquals("42", playback?.songId)
        assertEquals("Track", playback?.songTitle)
        assertEquals(10L, playback?.playedAt)
        assertNull(restoredMetadata(mapOf("title" to "Track")))
        assertNull(restoredSourcePath(mapOf("songId" to "42", "sourcePath" to "  ")))
        assertEquals(
            "42" to "/storage/emulated/0/Music/Track.mp3",
            restoredSourcePath(mapOf("songId" to "42", "sourcePath" to "/storage/emulated/0/Music/Track.mp3")),
        )
    }

    private fun sqliteHeader(userVersion: Int, size: Int): ByteArray {
        val header = ByteArray(size)
        val magic = "SQLite format 3\u0000".encodeToByteArray()
        if (size >= magic.size) magic.copyInto(header)
        if (size >= 64) {
            header[60] = (userVersion ushr 24).toByte()
            header[61] = (userVersion ushr 16).toByte()
            header[62] = (userVersion ushr 8).toByte()
            header[63] = userVersion.toByte()
        }
        return header
    }
}
