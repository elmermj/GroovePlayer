package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PrivateLibrarySongsTest {

    @Test
    fun listedAudioIgnoresStagingAndScratchFiles() {
        assertTrue(PrivateLibrarySongs.isListedAudio("track.mp3"))
        assertFalse(PrivateLibrarySongs.isListedAudio(".incoming"))
        assertFalse(PrivateLibrarySongs.isListedAudio("track.mp3.tmp"))
        assertFalse(PrivateLibrarySongs.isListedAudio("notes.txt"))
    }

    @Test
    fun destinationStaysInsideThePrivateRoot() {
        val root = tempDir()
        val dest = PrivateLibrarySongs.destination(root, "Song.mp3")
        assertEquals(root, dest.parentFile)
        assertEquals("Song.mp3", dest.name)
        assertFalse(dest.path.contains("/Music/"))
        dest.writeBytes(byteArrayOf(1))
        val next = PrivateLibrarySongs.destination(root, "Song.mp3")
        assertEquals("Song__2.mp3", next.name)
        assertEquals(root, next.parentFile)
    }

    @Test
    fun promoteMovesStagingIntoTheLibraryAndRemovesTheStagingFile() {
        val root = tempDir()
        val incoming = File(root, PrivateLibrarySongs.INCOMING_DIR).apply { mkdirs() }
        val staged = File(incoming, "live.mp3")
        staged.writeBytes(byteArrayOf(4, 5, 6))
        val dest = PrivateLibrarySongs.destination(root, "live.mp3")
        assertTrue(PrivateLibrarySongs.promote(staged, dest))
        assertTrue(dest.isFile)
        assertEquals(3, dest.length())
        assertFalse(staged.exists())
        assertFalse(dest.path.contains("Groove Downloads"))
    }

    @Test
    fun catalogMergeAndPagingKeepPrivateSongsAheadOfMediaStore() {
        val privateSong = PrivateLibrarySongs.toSong("/data/groove-library/a.mp3", "a.mp3", 3)
        val media = song("/storage/Music/b.mp3", "media")
        val merged = PrivateLibrarySongs.merge(listOf(media), listOf(privateSong))
        assertEquals(listOf(media.id, privateSong.id), merged.map { it.id })
        assertEquals(privateSong.filePath, merged[1].filePath)

        val first = PrivateLibrarySongs.pageRequest(privateCount = 2, offset = 0, limit = 1)
        assertEquals(1, first.privateLimit)
        assertEquals(0, first.mediaLimit)
        val overlap = PrivateLibrarySongs.pageRequest(privateCount = 2, offset = 1, limit = 2)
        assertEquals(1, overlap.privateLimit)
        assertEquals(0, overlap.mediaOffset)
        assertEquals(1, overlap.mediaLimit)
        val later = PrivateLibrarySongs.pageRequest(privateCount = 2, offset = 4, limit = 25)
        assertEquals(0, later.privateLimit)
        assertEquals(2, later.mediaOffset)
    }

    private fun song(path: String, id: String) = Song(
        id = id,
        title = "T",
        artist = "A",
        uri = "content://$id",
        genre = "",
        durationMs = 1L,
        filePath = path,
    )

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "groove-recv-${System.nanoTime()}").apply { mkdirs() }
}
