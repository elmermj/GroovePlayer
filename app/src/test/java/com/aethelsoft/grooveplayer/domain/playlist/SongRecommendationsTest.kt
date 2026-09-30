package com.aethelsoft.grooveplayer.domain.playlist

import com.aethelsoft.grooveplayer.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SongRecommendationsTest {

    @Test
    fun `most played ranks above an otherwise identical song`() {
        val low = song("low")
        val high = song("high")

        val result = SongRecommendations.top(
            songs = listOf(low, high),
            playCounts = mapOf("low" to 1, "high" to 8),
            order = SongRecommendations.Order.TITLE_ASC,
        )

        assertEquals(listOf("high", "low"), result.map { it.id })
    }

    @Test
    fun `songs from the artist with more tracks rank above a one-song artist`() {
        val solo = song("solo", artist = "Solo")
        val bandA = song("band-a", artist = "Band")
        val bandB = song("band-b", artist = "Band")

        val result = SongRecommendations.top(
            songs = listOf(solo, bandA, bandB),
            playCounts = emptyMap(),
            order = SongRecommendations.Order.TITLE_ASC,
        )

        assertEquals(listOf("band-a", "band-b"), result.take(2).map { it.id })
        assertEquals("solo", result.last().id)
    }

    @Test
    fun `title year and artist orders break a tie between equal play and catalog scores`() {
        val alpha = song("alpha", title = "Alpha", artist = "Ada", year = 1990)
        val zulu = song("zulu", title = "Zulu", artist = "Zoe", year = 2020)

        assertEquals(
            listOf("zulu", "alpha"),
            ids(listOf(alpha, zulu), SongRecommendations.Order.TITLE_DESC),
        )
        assertEquals(
            listOf("alpha", "zulu"),
            ids(listOf(alpha, zulu), SongRecommendations.Order.TITLE_ASC),
        )
        assertEquals(
            listOf("zulu", "alpha"),
            ids(listOf(alpha, zulu), SongRecommendations.Order.YEAR_DESC),
        )
        assertEquals(
            listOf("alpha", "zulu"),
            ids(listOf(alpha, zulu), SongRecommendations.Order.YEAR_ASC),
        )
        assertEquals(
            listOf("zulu", "alpha"),
            ids(listOf(alpha, zulu), SongRecommendations.Order.ARTIST_DESC),
        )
        assertEquals(
            listOf("alpha", "zulu"),
            ids(listOf(alpha, zulu), SongRecommendations.Order.ARTIST_ASC),
        )
    }

    @Test
    fun `songs without a year sort after dated songs`() {
        val dated = song("dated", year = 2001)
        val undated = song("undated", year = null)

        val result = SongRecommendations.top(
            songs = listOf(undated, dated),
            playCounts = emptyMap(),
            order = SongRecommendations.Order.YEAR_DESC,
        )

        assertEquals(listOf("dated", "undated"), result.map { it.id })
    }

    @Test
    fun `returns at most ten songs`() {
        val songs = (1..12).map { index -> song("song-$index", title = "Track $index") }

        val result = SongRecommendations.top(
            songs = songs,
            playCounts = emptyMap(),
            order = SongRecommendations.Order.TITLE_ASC,
        )

        assertEquals(10, result.size)
    }

    @Test
    fun `empty library returns nothing`() {
        val result = SongRecommendations.top(
            songs = emptyList(),
            playCounts = emptyMap(),
            order = SongRecommendations.Order.TITLE_ASC,
        )

        assertTrue(result.isEmpty())
    }

    private fun ids(songs: List<Song>, order: SongRecommendations.Order): List<String> =
        SongRecommendations.top(songs, emptyMap(), order).map { it.id }

    private fun song(
        id: String,
        title: String = "Same",
        artist: String = "Ada",
        year: Int? = 2000,
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        uri = "",
        genre = "",
        durationMs = 1_000,
        year = year,
    )
}
