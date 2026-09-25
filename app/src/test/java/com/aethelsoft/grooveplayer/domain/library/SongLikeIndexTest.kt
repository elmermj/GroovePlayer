package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.backup.restoredSongLike
import com.aethelsoft.grooveplayer.domain.model.Album
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.toSongLike
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongLikeIndexTest {

    @Test
    fun tracksAreNewestLikeFirstAndNeverDuplicateASong() {
        val likes = listOf(
            like("1", "A", "Ada", "One", likedAt = 10),
            like("1", "A", "Ada", "One", likedAt = 30),
            like("2", "B", "Ada", "One", likedAt = 20),
            like(" ", "Nope", "Ada", "One", likedAt = 99),
        )
        val tracks = SongLikeIndex.tracks(likes)
        assertEquals(listOf("1", "2"), tracks.map { it.id })
        assertEquals("A", tracks.first().title)
    }

    @Test
    fun artistAndAlbumDropWhenTheirLastLikedTrackIsRemoved() {
        val both = listOf(
            like("1", "Come Together", "The Beatles", "Abbey Road", likedAt = 1),
            like("2", "Something", "The Beatles", "Abbey Road", likedAt = 2),
            like("3", "Blue", "Joni Mitchell", "Blue", likedAt = 3),
        )
        assertEquals(listOf("Joni Mitchell", "The Beatles"), SongLikeIndex.artists(both).map { it.artist })
        assertEquals(2, SongLikeIndex.artists(both).first { it.artist == "The Beatles" }.playCount)
        assertEquals(listOf("Blue", "Abbey Road"), SongLikeIndex.albums(both).map { it.album })

        val withoutBeatles = both.filter { it.songId != "1" && it.songId != "2" }
        assertEquals(listOf("Joni Mitchell"), SongLikeIndex.artists(withoutBeatles).map { it.artist })
        assertEquals(listOf("Blue"), SongLikeIndex.albums(withoutBeatles).map { it.album })

        val oneBeatlesLeft = both.filter { it.songId != "2" }
        assertEquals(listOf("The Beatles"), SongLikeIndex.artists(oneBeatlesLeft).map { it.artist }.filter { it == "The Beatles" })
        assertEquals(1, SongLikeIndex.albums(oneBeatlesLeft).first { it.album == "Abbey Road" }.playCount)
    }

    @Test
    fun albumArtworkComesFromTheNewestLikedTrack() {
        val likes = listOf(
            like("1", "A", "Ada", "One", artwork = "old", likedAt = 1),
            like("2", "B", "Ada", "One", artwork = "new", likedAt = 5),
        )
        assertEquals("new", SongLikeIndex.albums(likes).single().artworkUrl)
        assertEquals(2, SongLikeIndex.albums(likes).single().playCount)
    }

    @Test
    fun missingAlbumBecomesASingleAndBlankSongIdDoesNotRestore() {
        val song = Song(
            id = "9",
            title = "Demo",
            artist = "  ",
            uri = "file://demo",
            genre = "",
            durationMs = 1_000,
            album = Album(id = "x", name = "  ", artist = "", artworkUrl = null, songs = emptyList()),
        )
        val like = song.toSongLike(4)
        assertEquals("Unknown Artist", like.artist)
        assertEquals("Single - Demo", like.album)
        assertNull(restoredSongLike(mapOf("title" to "Demo")))
        val restored = restoredSongLike(
            mapOf(
                "songId" to "9",
                "title" to "Demo",
                "likedAt" to "4",
            ),
        )
        assertEquals("9", restored?.songId)
        assertEquals("Unknown Artist", restored?.artist)
        assertEquals("Single - Demo", restored?.album)
        assertEquals(4L, restored?.likedAt)
        assertTrue(SongLikeIndex.tracks(listOf(like)).single().durationMs == 1_000L)
    }

    private fun like(
        id: String,
        title: String,
        artist: String,
        album: String,
        artwork: String? = null,
        likedAt: Long,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        uri = "file://$id",
        genre = "Pop",
        durationMs = 1,
        artworkUrl = artwork,
        album = Album(
            id = album,
            name = album,
            artist = artist,
            artworkUrl = artwork,
            songs = emptyList(),
        ),
    ).toSongLike(likedAt)
}
