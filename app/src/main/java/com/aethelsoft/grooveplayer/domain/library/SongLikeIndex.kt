package com.aethelsoft.grooveplayer.domain.library

import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.model.Song
import com.aethelsoft.grooveplayer.domain.model.SongLike
import com.aethelsoft.grooveplayer.domain.model.toSong

/**
 * Favorite tracks are the liked songs, newest like first.
 * Favorite artists and albums exist only while at least one liked track still belongs to them.
 */
object SongLikeIndex {
    /** No practical cap. Home cards and browse screens both show the full like set. */
    const val ALL = Int.MAX_VALUE

    fun tracks(likes: List<SongLike>, limit: Int = ALL): List<Song> {
        return deduped(likes)
            .sortedWith(compareByDescending<SongLike> { it.likedAt }.thenBy { it.title.lowercase() })
            .take(limit.coerceAtLeast(0))
            .map { it.toSong() }
    }

    fun artists(likes: List<SongLike>, limit: Int = ALL): List<FavoriteArtist> {
        return deduped(likes)
            .groupBy { it.artist.ifBlank { "Unknown Artist" } }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<SongLike>>> { entry ->
                    entry.value.maxOf { it.likedAt }
                }.thenBy { it.key.lowercase() },
            )
            .take(limit.coerceAtLeast(0))
            .map { (artist, rows) ->
                FavoriteArtist(
                    artist = artist,
                    playCount = rows.size,
                )
            }
    }

    fun albums(likes: List<SongLike>, limit: Int = ALL): List<FavoriteAlbum> {
        return deduped(likes)
            .groupBy { it.album.ifBlank { "Unknown Album" } to it.artist.ifBlank { "Unknown Artist" } }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<Pair<String, String>, List<SongLike>>> { entry ->
                    entry.value.maxOf { it.likedAt }
                }.thenBy { it.key.first.lowercase() },
            )
            .take(limit.coerceAtLeast(0))
            .map { (key, rows) ->
                val newest = rows.maxBy { it.likedAt }
                FavoriteAlbum(
                    album = key.first,
                    artist = key.second,
                    playCount = rows.size,
                    artworkUrl = newest.artworkUrl,
                )
            }
    }

    private fun deduped(likes: List<SongLike>): List<SongLike> {
        return likes
            .filter { it.songId.isNotBlank() }
            .groupBy { it.songId }
            .map { (_, rows) -> rows.maxBy { it.likedAt } }
    }
}
