package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistM3uMatchTest {

    private val one = song("1", "/storage/emulated/0/Music/one.mp3")
    private val two = song("2", "/data/groove-library/two.mp3")
    private val library = listOf(
        PlaylistLibrarySong(one, "aaa", "one.mp3"),
        PlaylistLibrarySong(two, "bbb", "two.mp3"),
    )

    @Test
    fun pathMatchWinsWhenTheFileCannotBeHashed() {
        val hit = PlaylistM3uMatch.match(
            location = "file:///storage/emulated/0/Music/one.mp3",
            readableHash = null,
            library = library,
        )
        assertEquals("1", hit?.song?.id)
    }

    @Test
    fun relativeNameMatchesWhenThePathDoesNot() {
        val hit = PlaylistM3uMatch.match("Music/two.mp3", readableHash = null, library = library)
        assertEquals("2", hit?.song?.id)
    }

    @Test
    fun hashDisagreementDoesNotKeepTheNameMatch() {
        val hit = PlaylistM3uMatch.match(
            location = "/storage/emulated/0/Music/one.mp3",
            readableHash = "bbb",
            library = library,
        )
        assertEquals("2", hit?.song?.id)
        assertEquals("bbb", hit?.contentHash)
    }

    @Test
    fun unmatchedHashDoesNotFallThroughToAnotherSong() {
        val hit = PlaylistM3uMatch.match(
            location = "/storage/emulated/0/Music/one.mp3",
            readableHash = "ccc",
            library = library,
        )
        assertNull(hit)
    }

    @Test
    fun ambiguousFileNameWithoutAHashStaysUnmatched() {
        val copy = PlaylistLibrarySong(song("3", "/other/one.mp3"), "ddd", "one.mp3")
        val hit = PlaylistM3uMatch.match("one.mp3", readableHash = null, library = library + copy)
        assertNull(hit)
    }

    @Test
    fun sharedHashPicksTheLowestSongId() {
        val copy = PlaylistLibrarySong(song("9", "/data/groove-library/one-copy.mp3"), "aaa", "one-copy.mp3")
        val hit = PlaylistM3uMatch.match("missing.mp3", readableHash = "AAA", library = library + copy)
        assertEquals("1", hit?.song?.id)
    }

    @Test
    fun exportUsesTheLibraryFileNameOnly() {
        assertEquals("two.mp3", PlaylistM3uMatch.exportFileName(two))
        assertEquals("", PlaylistM3uMatch.fileName("content://media/external/audio/media/7"))
    }

    private fun song(id: String, path: String) = Song(
        id = id,
        title = id,
        artist = "Ada",
        uri = "",
        genre = "",
        durationMs = 1_000,
        filePath = path,
    )
}
