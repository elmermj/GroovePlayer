package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Explicit song likes. Available to every user. Artists and albums are derived.
 */
interface SongLikeRepository {
    fun observeLikedSongIds(): Flow<Set<String>>

    fun observeFavoriteTracks(limit: Int): Flow<List<Song>>

    fun observeFavoriteArtists(limit: Int): Flow<List<FavoriteArtist>>

    fun observeFavoriteAlbums(limit: Int): Flow<List<FavoriteAlbum>>

    suspend fun toggle(song: Song)
}
