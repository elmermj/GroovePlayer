package com.aethelsoft.grooveplayer.domain.backup

import com.aethelsoft.grooveplayer.data.local.db.RecoveryResult
import com.aethelsoft.grooveplayer.data.local.db.RoomDbSwapFiles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FoolproofBackupRestoreTest {

    @Test
    fun sameHashReusesExistingFileAndNeverAddsCopySuffix() {
        val existing = HashedAudio(
            path = "/music/Groove Downloads/track.mp3",
            contentHash = "abc",
            sizeBytes = 10,
        )
        val reused = GrooveDownloadPlacement.place(
            downloadsDir = "/music/Groove Downloads",
            cosmeticFileName = "renamed.mp3",
            contentHash = "abc",
            sizeBytes = 10,
            existing = listOf(existing),
        )
        assertTrue(reused.reusedExisting)
        assertEquals(existing.path, reused.destinationPath)

        val collision = GrooveDownloadPlacement.place(
            downloadsDir = "/music/Groove Downloads",
            cosmeticFileName = "track.mp3",
            contentHash = "def",
            sizeBytes = 10,
            existing = listOf(existing),
        )
        assertFalse(collision.reusedExisting)
        assertFalse(collision.destinationPath.contains(" (1)"))
        assertTrue(collision.destinationPath.endsWith("/def.mp3"))
    }

    @Test
    fun cloudSkipRequiresHashAndSize() {
        val cloud = listOf("abc" to 10L)
        assertTrue(CloudHashDedup.alreadyStored(cloud, "ABC", 10L))
        assertFalse(CloudHashDedup.alreadyStored(cloud, "abc", 11L))
        assertFalse(CloudHashDedup.alreadyStored(cloud, "other", 10L))
    }

    @Test
    fun catalogPathsRewriteExactOrUniqueNameAndReportMissingBytes() {
        val placed = listOf(
            PlacedCloudSong(
                contentHash = "abc",
                sizeBytes = 4,
                logicalPath = "/sdcard/Music/track.mp3",
                localPath = "/music/Groove Downloads/track.mp3",
            ),
        )
        assertEquals(
            "/music/Groove Downloads/track.mp3",
            BackupCatalogPaths.rewrite("/sdcard/Music/track.mp3", placed),
        )
        assertEquals(
            "/music/Groove Downloads/track.mp3",
            BackupCatalogPaths.rewrite("/other-phone/Music/track.mp3", placed),
        )
        assertEquals(
            "/local/only.mp3",
            BackupCatalogPaths.rewrite("/local/only.mp3", placed),
        )
        assertEquals(
            listOf("/music/Groove Downloads/track.mp3"),
            BackupCatalogPaths.missingLocalBytes(placed) { -1L },
        )
        assertTrue(BackupCatalogPaths.missingLocalBytes(placed) { 4L }.isEmpty())
    }

    @Test
    fun swapCommitKeepsPreviousAndRollbackRestoresItWhenLiveIsCorrupt() {
        val root = tempDir()
        val swap = swapFiles(root)
        val original = sqliteFile(File(root, "original.db"), "original-body")
        val snapshot = sqliteFile(File(root, "snapshot.db"), "snapshot-body")
        original.copyTo(swapLive(root), overwrite = true)

        swap.arm(snapshot)
        assertEquals(DbSwapStep.ARMED, swap.step())
        assertEquals(readText(swapLive(root)), readText(original))

        swap.commit()
        assertEquals(DbSwapStep.COMMITTED, swap.step())
        assertEquals(readText(snapshot), readText(swapLive(root)))
        assertEquals(readText(original), readText(File(root, "previous/grooveplayer_database")))
        assertFalse(File(swapLive(root).path + "-wal").exists())

        File(swapLive(root).path + "-wal").writeText("stale-wal")
        swapLive(root).writeBytes(ByteArray(8) { 1 })
        assertEquals(RecoveryResult.ROLLED_BACK, swap.recover())
        assertEquals(readText(original), readText(swapLive(root)))
        assertEquals(DbSwapStep.IDLE, swap.step())
        assertFalse(File(swapLive(root).path + "-wal").exists())
    }

    @Test
    fun idleJournalDoesNotReplaceLiveWithAnOldPreviousCopy() {
        val root = tempDir()
        val live = sqliteFile(swapLive(root), "current-library")
        sqliteFile(File(root, "previous/grooveplayer_database"), "old-library")
        File(root, "restore-swap").mkdirs()
        File(root, "restore-swap/journal").writeText(DbSwapStep.IDLE.name)
        val swap = swapFiles(root)
        assertFalse(swap.restorePreviousIfPresent())
        assertTrue(live.readBytes().decodeToString().contains("current-library"))
    }

    @Test
    fun killAfterParkingLivePromotesIncomingWithoutADuplicateName() {
        val root = tempDir()
        val live = swapLive(root)
        val original = sqliteFile(live, "original-body")
        val incoming = sqliteFile(File(live.path + ".incoming"), "incoming-body")
        original.copyTo(File(live.path + ".aside"), overwrite = true)
        live.delete()
        File(root, "restore-swap").mkdirs()
        File(root, "restore-swap/journal").writeText(DbSwapStep.LIVE_PARKED.name)
        sqliteFile(File(root, "previous/grooveplayer_database"), "original-body")

        val expected = incoming.readBytes()
        val swap = swapFiles(root)
        assertEquals(DbSwapAction.PROMOTE_INCOMING, RoomDbSwap.nextAction(swap.snapshot()))
        assertEquals(RecoveryResult.PROMOTED, swap.recover())
        assertTrue(expected.contentEquals(live.readBytes()))
        assertFalse(live.name.contains(" (1)"))
        assertEquals(DbSwapStep.COMMITTED, swap.step())
    }

    private fun swapFiles(root: File): RoomDbSwapFiles {
        val live = swapLive(root)
        return RoomDbSwapFiles(
            live = live,
            aside = File(live.path + ".aside"),
            incoming = File(live.path + ".incoming"),
            previous = File(root, "previous/grooveplayer_database"),
            journal = File(root, "restore-swap/journal"),
        )
    }

    private fun swapLive(root: File) = File(root, "databases/grooveplayer_database")

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "groove-swap-${System.nanoTime()}").apply { mkdirs() }

    private fun sqliteFile(dest: File, body: String): File {
        dest.parentFile?.mkdirs()
        val bytes = "SQLite format 3\u0000".encodeToByteArray() + body.encodeToByteArray()
        dest.writeBytes(bytes)
        return dest
    }

    private fun readText(file: File): String = file.readBytes().decodeToString()
}
