package com.aethelworks.grooveplayer.domain.playlist

import com.aethelworks.grooveplayer.domain.model.Song
import com.aethelworks.grooveplayer.utils.ArtistParser

/**
 * Top library tracks from three equal signals: play count, how many songs each
 * artist has, and place in a catalog order chosen by the caller.
 */
object SongRecommendations {
    const val LIMIT = 10

    enum class Order {
        TITLE_ASC,
        TITLE_DESC,
        YEAR_ASC,
        YEAR_DESC,
        ARTIST_ASC,
        ARTIST_DESC,
    }

    fun top(
        songs: List<Song>,
        playCounts: Map<String, Int>,
        order: Order,
        limit: Int = LIMIT,
    ): List<Song> {
        if (songs.isEmpty() || limit <= 0) return emptyList()
        val scale = songs.size
        val artistCounts = artistSongCounts(songs)
        val maxPlays = songs.maxOf { plays(it, playCounts) }
        val maxArtistSongs = songs.maxOf { artistSongs(it, artistCounts) }
        val orderRank = orderRanks(songs, comparator(order))
        val lastOrderRank = orderRank.values.maxOrNull() ?: 0
        return songs
            .sortedWith(
                compareByDescending<Song> { song ->
                    points(plays(song, playCounts), maxPlays, scale) +
                        points(artistSongs(song, artistCounts), maxArtistSongs, scale) +
                        orderPoints(orderRank[song.id] ?: 0, lastOrderRank, scale)
                }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.id },
            )
            .take(limit)
    }

    private fun plays(song: Song, playCounts: Map<String, Int>): Int =
        (playCounts[song.id] ?: 0).coerceAtLeast(0)

    private fun artistSongs(song: Song, counts: Map<String, Int>): Int =
        ArtistParser.parseArtists(song.artist).maxOf { name -> counts[name.lowercase()] ?: 0 }

    private fun artistSongCounts(songs: List<Song>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        songs.forEach { song ->
            ArtistParser.parseArtists(song.artist).forEach { name ->
                val key = name.lowercase()
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
        return counts
    }

    private fun orderRanks(songs: List<Song>, comparator: Comparator<Song>): Map<String, Int> {
        val sorted = songs.sortedWith(comparator.thenBy { it.id })
        val ranks = mutableMapOf<String, Int>()
        var rank = 0
        sorted.forEachIndexed { index, song ->
            if (index > 0 && comparator.compare(sorted[index - 1], song) != 0) {
                rank = index
            }
            ranks[song.id] = rank
        }
        return ranks
    }

    private fun comparator(order: Order): Comparator<Song> = when (order) {
        Order.TITLE_ASC -> compareBy { it.title.lowercase() }
        Order.TITLE_DESC -> compareByDescending { it.title.lowercase() }
        Order.YEAR_ASC -> compareBy<Song> { it.year == null }.thenBy { it.year ?: Int.MAX_VALUE }
        Order.YEAR_DESC -> compareBy<Song> { it.year == null }.thenByDescending { it.year ?: Int.MIN_VALUE }
        Order.ARTIST_ASC -> compareBy<Song> { ArtistParser.getPrimaryArtist(it.artist).lowercase() }
            .thenBy { it.title.lowercase() }
        Order.ARTIST_DESC -> compareByDescending<Song> { ArtistParser.getPrimaryArtist(it.artist).lowercase() }
            .thenBy { it.title.lowercase() }
    }

    /** Best value on a signal scores [scale]; a missing or zero max scores 0. */
    private fun points(value: Int, max: Int, scale: Int): Int {
        if (max <= 0 || scale <= 0) return 0
        return (value.toLong() * scale / max).toInt()
    }

    private fun orderPoints(rank: Int, lastRank: Int, scale: Int): Int {
        if (lastRank <= 0) return scale
        return points(lastRank - rank, lastRank, scale)
    }
}
