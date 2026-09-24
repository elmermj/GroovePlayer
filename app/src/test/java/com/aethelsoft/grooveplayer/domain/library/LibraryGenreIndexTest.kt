package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.model.Genre
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.playCountLabel
import com.aethelsoft.grooveplayer.domain.model.trackCountLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGenreIndexTest {

    @Test
    fun `songs without genre tags produce an empty browse list`() {
        val songs = listOf(
            song(id = "1", genre = ""),
            song(id = "2", genre = "   "),
        )

        assertTrue(LibraryGenreIndex.build(songs).isEmpty())
    }

    @Test
    fun `genres group case-insensitively and split combined tags`() {
        val songs = listOf(
            song(id = "1", title = "A", genre = "Rock, Jazz"),
            song(id = "2", title = "B", genre = "rock"),
            song(id = "3", title = "C", genre = "Jazz; Soul"),
        )

        val genres = LibraryGenreIndex.build(songs)

        assertEquals(listOf("Jazz", "Rock", "Soul"), genres.map { it.name })
        assertEquals(2, genres.first { it.name == "Rock" }.trackCount)
        assertEquals(2, genres.first { it.name == "Jazz" }.trackCount)
        assertEquals(1, genres.first { it.name == "Soul" }.trackCount)
    }

    @Test
    fun `edited metadata replaces the mediastore genre`() {
        val songs = listOf(
            song(id = "1", genre = "Jazz", genres = listOf("Ignored")),
            song(id = "2", genre = "Rock"),
        )
        val edited = mapOf(
            "1" to listOf("Soul", "Funk"),
            "2" to emptyList(),
        )

        val genres = LibraryGenreIndex.build(songs, edited)

        assertEquals(listOf("Funk", "Soul"), genres.map { it.name })
        assertEquals(listOf("1"), LibraryGenreIndex.songsIn("soul", songs, edited).map { it.id })
        assertTrue(LibraryGenreIndex.songsIn("Rock", songs, edited).isEmpty())
        assertTrue(LibraryGenreIndex.songsIn("Jazz", songs, edited).isEmpty())
    }

    @Test
    fun `song genre list is used when metadata was not edited`() {
        val songs = listOf(
            song(id = "1", genre = "Jazz", genres = listOf("Ambient")),
        )

        assertEquals(listOf("Ambient"), LibraryGenreIndex.build(songs).map { it.name })
    }

    @Test
    fun `genre tracks are sorted by title`() {
        val songs = listOf(
            song(id = "b", title = "Beta", genre = "Pop"),
            song(id = "a", title = "alpha", genre = "Pop"),
        )

        assertEquals(
            listOf("a", "b"),
            LibraryGenreIndex.songsIn("pop", songs).map { it.id },
        )
    }

    @Test
    fun `count labels stay singular for one`() {
        assertEquals("1 track", trackCountLabel(1))
        assertEquals("3 tracks", trackCountLabel(3))
        assertEquals("1 play", playCountLabel(1))
        assertEquals("4 plays", playCountLabel(4))
    }

    private fun song(
        id: String,
        title: String = id,
        genre: String = "",
        genres: List<String> = emptyList(),
    ) = Song(
        id = id,
        title = title,
        artist = "Artist",
        uri = "file://$id",
        genre = genre,
        durationMs = 1_000L,
        genres = genres.map { Genre(id = it, name = it) },
    )
}
