package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.playback.continueListeningIndex
import com.aethelsoft.grooveplayer.domain.playback.restorePlaybackById
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

class LibraryImportPolicyTest {

    @Test
    fun hashDedupCountsAnExistingPrivateHashAsAlreadyInLibrary() {
        val root = tempDir()
        val bytes = ByteArray(32) { it.toByte() }
        val known = mutableSetOf<String>()
        val first = LibraryImporter.importAll(
            sources = listOf(bytesSource("a.mp3", bytes)),
            root = root,
            knownPrivateHashes = known,
            freeBytes = { 10_000_000L },
            cancelled = { false },
        )
        assertEquals(1, first.copied)
        val hash = first.files.single().sha256!!
        val second = LibraryImporter.importAll(
            sources = listOf(bytesSource("copy.mp3", bytes)),
            root = root,
            knownPrivateHashes = setOf(hash),
            freeBytes = { 10_000_000L },
            cancelled = { false },
        )
        assertEquals(0, second.copied)
        assertEquals(1, second.alreadyInLibrary)
        assertEquals(1, root.listFiles()?.count { it.isFile && it.extension == "mp3" })
    }

    @Test
    fun mismatchDiscardsTheCopy() {
        val dest = File(tempDir(), "bad.mp3")
        dest.writeBytes(byteArrayOf(1, 2, 3, 4))
        assertFalse(LibraryFileCopy.discardUnlessMatch(dest, expectedHash = "deadbeef", expectedSize = 4))
        assertFalse(dest.exists())
    }

    @Test
    fun verifiedCopyIsKept() {
        val root = tempDir()
        val bytes = "groove-audio".toByteArray()
        val result = LibraryImporter.importAll(
            sources = listOf(bytesSource("keep.mp3", bytes)),
            root = root,
            knownPrivateHashes = emptySet(),
            freeBytes = { 10_000_000L },
            cancelled = { false },
        )
        val file = result.files.single()
        assertTrue(file.verifiedCopy)
        val dest = File(file.libraryPath!!)
        assertTrue(LibraryFileCopy.discardUnlessMatch(dest, file.sha256!!, bytes.size.toLong()))
        assertTrue(dest.exists())
        assertEquals(bytes.size.toLong(), dest.length())
    }

    @Test
    fun cancelDeletesOnlyThePartialFile() {
        val root = tempDir()
        val done = LibraryImporter.importAll(
            sources = listOf(bytesSource("done.mp3", ByteArray(16) { 1 })),
            root = root,
            knownPrivateHashes = emptySet(),
            freeBytes = { 10_000_000L },
            cancelled = { false },
        )
        assertTrue(File(done.files.single().libraryPath!!).isFile)
        var reads = 0
        val big = ByteArray(64 * 1024) { 7 }
        val cancelled = LibraryImporter.importAll(
            sources = listOf(object : ImportByteSource {
                override val displayName = "partial.mp3"
                override val sizeBytes = big.size.toLong()
                override val stableKey = "partial"
                override fun open(): InputStream = object : InputStream() {
                    private val delegate = ByteArrayInputStream(big)
                    override fun read(): Int = delegate.read()
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        if (reads > 0) return delegate.read(b, off, len)
                        reads++
                        return delegate.read(b, off, len.coerceAtMost(LibraryFileCopy.BUFFER_BYTES))
                    }
                }
            }),
            root = root,
            knownPrivateHashes = emptySet(),
            freeBytes = { 10_000_000L },
            cancelled = { reads > 0 },
        )
        assertTrue(cancelled.cancelled)
        assertEquals(ImportFileStatus.CANCELLED, cancelled.files.last().status)
        val incoming = File(root, ".incoming")
        val partials = incoming.listFiles()?.filter { it.name.endsWith(".partial") }.orEmpty()
        assertTrue(partials.isEmpty())
        assertTrue(File(done.files.single().libraryPath!!).isFile)
    }

    @Test
    fun catalogListsOnlyExistingPrivateLibraryFiles() {
        val root = "/data/user/0/com.aethelsoft.grooveplayer/files/groove-library"
        val songs = listOf(
            song("$root/a.mp3", "private"),
            song("/storage/Music/b.mp3", "shared"),
            song("$root/missing.mp3", "gone"),
        )
        val listed = PrivateCatalogFilter.listedSongs(songs, root) { path -> !path.endsWith("missing.mp3") }
        assertEquals(listOf("private"), listed.map { it.id })
    }

    @Test
    fun backupSelectsPrivateLibrarySongsOnly() {
        val root = "/data/groove-library"
        val songs = listOf(
            song("$root/a.mp3", "a"),
            song("/storage/Music/b.mp3", "b"),
            song(null, "cloud"),
        )
        val selected = BackupLibraryFiles.select(songs) { path -> path.startsWith(root) }
        assertEquals(listOf("$root/a.mp3"), selected)
    }

    @Test
    fun missingStartSongDoesNotResolveToAnotherTrack() {
        val library = listOf(song("/lib/a.mp3", "a"), song("/lib/c.mp3", "c"))
        assertNull(continueListeningIndex(library, "missing"))
        assertEquals(1, continueListeningIndex(library, "c"))
        assertNull(
            restorePlaybackById(
                savedIds = listOf("a", "b", "c"),
                savedStartIndex = 1,
                available = library,
            ),
        )
        val restored = restorePlaybackById(
            savedIds = listOf("a", "c"),
            savedStartIndex = 1,
            available = library,
        )
        assertEquals("c", restored?.songs?.get(restored.startIndex)?.id)
    }

    @Test
    fun remapFollowsLikesHistoryAndPlaylistsByHash() {
        val rows = listOf(
            SongHashRow("42", "abc", inPrivateLibrary = false),
            SongHashRow("7", "abc", inPrivateLibrary = false),
            SongHashRow("other", "zzz", inPrivateLibrary = false),
        )
        val links = LinkedSongIds(
            likes = setOf("42", "7"),
            plays = listOf("42", "42", "7"),
            metadata = setOf("42"),
            playlistMembers = setOf("7"),
            queue = listOf("7", "other"),
            lastPlayed = "7",
        )
        val remap = SongHashRemap.remap("abc", rows, links)
        assertEquals("42", remap.canonicalSongId)
        assertFalse(remap.createdNewId)
        assertEquals(setOf("42"), remap.links.likes)
        assertEquals(listOf("42", "42", "42"), remap.links.plays)
        assertEquals(setOf("42"), remap.links.playlistMembers)
        assertEquals(listOf("42", "other"), remap.links.queue)
        assertEquals("42", remap.links.lastPlayed)
        assertTrue("other" !in remap.retiredSongIds)
    }

    @Test
    fun remapPrefersThePrivateLibraryRow() {
        val rows = listOf(
            SongHashRow("42", "abc", inPrivateLibrary = false),
            SongHashRow("private:abc", "abc", inPrivateLibrary = true),
        )
        val links = LinkedSongIds(likes = setOf("42"), plays = listOf("42", "42", "42"))
        val remap = SongHashRemap.remap("ABC", rows, links)
        assertEquals("private:abc", remap.canonicalSongId)
        assertEquals(setOf("private:abc"), remap.links.likes)
        assertEquals(listOf("private:abc", "private:abc", "private:abc"), remap.links.plays)
    }

    @Test
    fun dismissingOrIgnoringDeleteRemovesNothing() {
        val eligible = listOf(
            ImportFileResult(
                displayName = "a.mp3",
                stableKey = "doc/a",
                status = ImportFileStatus.COPIED,
                sha256 = "abc",
                verifiedCopy = true,
            ),
        )
        val present = { hash: String -> hash == "abc" }
        assertTrue(ImportOriginalDeletion.keysToDelete(OriginalDeleteChoice.KEEP, eligible, present).isEmpty())
        assertTrue(ImportOriginalDeletion.keysToDelete(OriginalDeleteChoice.DISMISSED, eligible, present).isEmpty())
        assertEquals(1, ImportOriginalDeletion.keysToDelete(OriginalDeleteChoice.DELETE, eligible, present).size)
        assertTrue(
            ImportOriginalDeletion.keysToDelete(OriginalDeleteChoice.DELETE, eligible) { false }.isEmpty(),
        )
        val skipped = eligible.map { it.copy(status = ImportFileStatus.ALREADY_IN_LIBRARY, verifiedCopy = false) }
        assertTrue(ImportOriginalDeletion.eligibleKeys(skipped).isEmpty())
        assertTrue(ImportOriginalDeletion.shouldPrompt(copied = 0, alreadyInLibrary = 1))
        assertFalse(ImportOriginalDeletion.shouldPrompt(copied = 0, alreadyInLibrary = 0))
    }

    @Test
    fun upgradeMigrationHidesSharedSongsAndKeepsRows() {
        val sql = PrivateLibrarySchemaMigration.STATEMENTS.joinToString("\n")
        assertFalse(sql.contains("DELETE", ignoreCase = true))
        assertFalse(sql.contains("DROP", ignoreCase = true))
        assertTrue(sql.contains("contentHash"))
        assertTrue(sql.contains("inPrivateLibrary"))
        val root = "/data/groove-library"
        assertTrue(PrivateLibrarySchemaMigration.inPrivateLibrary("$root/a.mp3", root))
        assertFalse(PrivateLibrarySchemaMigration.inPrivateLibrary("/storage/Music/a.mp3", root))
        assertFalse(PrivateLibrarySchemaMigration.inPrivateLibrary(null, root))
        assertEquals(
            LibraryUpgradePrompt.BODY,
            "GroovePlayer now keeps its own library. Import your music folders to see your songs again.",
        )
        assertTrue(LibraryUpgradePrompt.shouldShow(hiddenSongCount = 2, dismissed = false))
        assertFalse(LibraryUpgradePrompt.shouldShow(hiddenSongCount = 2, dismissed = true))
        assertFalse(LibraryUpgradePrompt.shouldShow(hiddenSongCount = 0, dismissed = false))
    }

    @Test
    fun importPromptCopyMatchesTheProductText() {
        assertEquals("Import complete", ImportPromptCopy.TITLE)
        assertEquals("Keep originals", ImportPromptCopy.KEEP)
        assertEquals("Delete originals", ImportPromptCopy.DELETE)
        assertEquals(
            "3 songs were copied into GroovePlayer. Do you want to delete the original files from Albums? Your copies in GroovePlayer stay.",
            ImportPromptCopy.body(3, "Albums"),
        )
        assertEquals("Importing 2 of 5", ImportPromptCopy.progress(2, 5))
        assertEquals("Deleted 2 of 3; 1 couldn't be deleted", ImportPromptCopy.deleteReport(2, 1))
        assertEquals("Music", folderDisplayName("primary:Music/Albums/../Music".let { "primary:Download/Music" }))
    }

    @Test
    fun outOfSpaceStopsWithoutDeletingCompletedCopies() {
        val root = tempDir()
        val first = ByteArray(8) { 1 }
        var checks = 0
        val result = LibraryImporter.importAll(
            sources = listOf(
                bytesSource("ok.mp3", first),
                bytesSource("big.mp3", ByteArray(100)),
            ),
            root = root,
            knownPrivateHashes = emptySet(),
            freeBytes = {
                checks++
                if (checks == 1) 10_000L else 20L
            },
            cancelled = { false },
        )
        assertEquals(LibraryFileCopy.OUT_OF_SPACE, result.stoppedReason)
        assertEquals(1, result.copied)
        assertTrue(File(result.files.first().libraryPath!!).isFile)
    }

    @Test
    fun mediaStoreMatchRequiresThePickedFolder() {
        val picked = "primary:Download/gp_qa_a1"
        val rows = listOf(
            MediaStoreAudioRow(id = 1, relativePath = "Download/other/", volumeName = "external_primary"),
            MediaStoreAudioRow(id = 2, relativePath = "Music/gp_qa_a1/", volumeName = "external_primary"),
        )
        assertNull(MediaStoreOriginalMatch.uniqueId(rows, picked, importedSha256 = "abc"))
        val exact = listOf(
            MediaStoreAudioRow(id = 7, relativePath = "Download/gp_qa_a1/", contentHash = "abc", volumeName = "external_primary"),
            MediaStoreAudioRow(id = 8, relativePath = "Download/other/", contentHash = "abc", volumeName = "external_primary"),
        )
        assertEquals(7L, MediaStoreOriginalMatch.uniqueId(exact, picked, "ABC"))
    }

    @Test
    fun mediaStoreMatchRejectsADifferentHashAndDuplicateFolders() {
        val picked = "primary:Download/gp_qa_a1"
        val wrongHash = listOf(
            MediaStoreAudioRow(id = 3, relativePath = "Download/gp_qa_a1/", contentHash = "other", volumeName = "external_primary"),
        )
        assertNull(MediaStoreOriginalMatch.uniqueId(wrongHash, picked, "abc"))
        val twins = listOf(
            MediaStoreAudioRow(id = 4, relativePath = "Download/gp_qa_a1/", volumeName = "external_primary"),
            MediaStoreAudioRow(id = 5, relativePath = "Download/gp_qa_a1/sub/", volumeName = "external_primary"),
        )
        assertNull(MediaStoreOriginalMatch.uniqueId(twins, picked, importedSha256 = null))
        val hashed = listOf(
            MediaStoreAudioRow(id = 4, relativePath = "Download/gp_qa_a1/", contentHash = "nope", volumeName = "external_primary"),
            MediaStoreAudioRow(id = 5, relativePath = "Download/gp_qa_a1/sub/", contentHash = "abc", volumeName = "external_primary"),
        )
        assertEquals(5L, MediaStoreOriginalMatch.uniqueId(hashed, picked, "abc"))
        assertNull(MediaStoreOriginalMatch.uniqueId(hashed, "", "abc"))
    }

    @Test
    fun mediaStoreMatchRequiresThePickedVolume() {
        val picked = "primary:Download/gp_qa_a1"
        val otherVolume = listOf(
            MediaStoreAudioRow(
                id = 9,
                relativePath = "Download/gp_qa_a1/",
                contentHash = "abc",
                volumeName = "aaaa-bbbb",
            ),
        )
        assertNull(MediaStoreOriginalMatch.uniqueId(otherVolume, picked, "abc"))
        val onCard = listOf(
            MediaStoreAudioRow(
                id = 9,
                relativePath = "Download/gp_qa_a1/",
                contentHash = "abc",
                volumeName = "abcd-1234",
            ),
        )
        assertEquals(9L, MediaStoreOriginalMatch.uniqueId(onCard, "ABCD-1234:Download/gp_qa_a1", "abc"))
        assertNull(MediaStoreOriginalMatch.uniqueId(onCard, picked, "abc"))
    }

    @Test
    fun failedPlacementDoesNotDeleteAPreExistingFile() {
        val root = tempDir()
        val existing = File(root, "song.mp3")
        existing.writeBytes(ByteArray(100) { 9 })
        val partial = File(root, "song.mp3.partial")
        partial.writeBytes(ByteArray(20) { 1 })
        assertFalse(LibraryFilePlacement.placeVerified(partial, existing, partial.length()))
        assertTrue(existing.isFile)
        assertEquals(100, existing.length())
        assertTrue(partial.isFile)
    }

    @Test
    fun placementKeepsANewVerifiedFile() {
        val root = tempDir()
        val dest = File(root, "song.mp3")
        val partial = File(root, "incoming.partial")
        val bytes = ByteArray(24) { 3 }
        partial.writeBytes(bytes)
        assertTrue(LibraryFilePlacement.placeVerified(partial, dest, bytes.size.toLong()))
        assertTrue(dest.isFile)
        assertEquals(bytes.size.toLong(), dest.length())
        assertFalse(partial.exists())
    }

    @Test
    fun unavailableTrackOffersRestoreOnlyWhenACloudCopyExists() {
        assertEquals(TrackPresence(playable = true, canRestore = false), trackPresence(true, true))
        assertEquals(TrackPresence(playable = false, canRestore = true), trackPresence(false, true))
        assertEquals(TrackPresence(playable = false, canRestore = false), trackPresence(false, false))
    }

    private fun bytesSource(name: String, bytes: ByteArray) = object : ImportByteSource {
        override val displayName = name
        override val sizeBytes = bytes.size.toLong()
        override val stableKey = name
        override fun open(): InputStream = ByteArrayInputStream(bytes)
    }

    private fun song(path: String?, id: String) = Song(
        id = id,
        title = id,
        artist = "A",
        uri = "file://$id",
        genre = "",
        durationMs = 1L,
        filePath = path,
    )

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "groove-import-${System.nanoTime()}").apply { mkdirs() }
}
