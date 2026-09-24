package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.M3uTrack
import com.aethelsoft.grooveplayer.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistLibraryPathsTest {

    @Test
    fun `file uris and windows paths normalize to library paths`() {
        assertEquals(
            "/storage/emulated/0/Music/a.mp3",
            PlaylistLibraryPaths.normalize("file:///storage/emulated/0/Music/a.mp3"),
        )
        assertEquals(
            "/storage/emulated/0/Music/a b.mp3",
            PlaylistLibraryPaths.normalize("file://localhost/storage/emulated/0/Music/a%20b.mp3"),
        )
        assertEquals(
            "C:/Music/a.mp3",
            PlaylistLibraryPaths.normalize("C:\\Music\\a.mp3"),
        )
        assertEquals(
            "content://media/external/audio/media/7",
            PlaylistLibraryPaths.normalize("content://media/external/audio/media/7"),
        )
    }

    @Test
    fun `export then parse matches the same library songs in order`() {
        val first = song(id = "1", filePath = "/storage/emulated/0/Music/one.mp3")
        val second = song(id = "2", filePath = "/storage/emulated/0/Music/two.mp3", durationMs = 0)
        val library = listOf(second, first)

        val exported = M3uCodec.serialize(
            "Mix",
            PlaylistLibraryPaths.toM3uTracks(listOf(first, second)),
        )
        val parsed = M3uCodec.parse(exported)
        val match = PlaylistLibraryPaths.match(parsed.tracks, library)

        assertEquals(listOf("1", "2"), match.matched.map { it.id })
        assertTrue(match.missingLocations.isEmpty())
        assertEquals("/storage/emulated/0/Music/one.mp3", parsed.tracks[0].location)
        assertEquals("Ada - One", parsed.tracks[0].title)
        assertEquals(180L, parsed.tracks[0].durationSeconds)
        assertEquals(-1L, parsed.tracks[1].durationSeconds)
    }

    @Test
    fun `file uri entries match stored paths and missing files are reported`() {
        val song = song(id = "9", filePath = "/storage/emulated/0/Music/nine.mp3")
        val tracks = listOf(
            M3uTrack("file:///storage/emulated/0/Music/nine.mp3"),
            M3uTrack("/storage/emulated/0/Music/missing.mp3"),
            M3uTrack("file:///storage/emulated/0/Music/nine.mp3"),
        )

        val match = PlaylistLibraryPaths.match(tracks, listOf(song))

        assertEquals(listOf("9", "9"), match.matched.map { it.id })
        assertEquals(listOf("/storage/emulated/0/Music/missing.mp3"), match.missingLocations)
    }

    @Test
    fun `content uri matches when the library has no file path`() {
        val uri = "content://media/external/audio/media/4"
        val song = song(id = "4", filePath = null, uri = uri)

        val match = PlaylistLibraryPaths.match(listOf(M3uTrack(uri)), listOf(song))

        assertEquals(listOf("4"), match.matched.map { it.id })
        assertEquals(uri, PlaylistLibraryPaths.exportLocation(song))
    }

    @Test
    fun `path match ignores case`() {
        val song = song(id = "3", filePath = "/storage/emulated/0/Music/Song.mp3")

        val match = PlaylistLibraryPaths.match(
            listOf(M3uTrack("/storage/emulated/0/music/song.mp3")),
            listOf(song),
        )

        assertEquals(listOf("3"), match.matched.map { it.id })
    }

    private fun song(
        id: String,
        filePath: String?,
        uri: String = "content://media/external/audio/media/$id",
        durationMs: Long = 180_000,
    ) = Song(
        id = id,
        title = "One",
        artist = "Ada",
        uri = uri,
        genre = "",
        durationMs = durationMs,
        filePath = filePath,
    )
}
