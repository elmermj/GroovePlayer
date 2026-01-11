package com.aethelsoft.grooveplayer.domain.repository

import com.aethelsoft.grooveplayer.domain.model.FavoriteAlbum
import com.aethelsoft.grooveplayer.domain.model.FavoriteArtist
import com.aethelsoft.grooveplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaybackHistoryRepository {
    suspend fun recordPlayback(song: Song)
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<Song>>
    suspend fun getFavoriteTracks(sinceTimestamp: Long, limit: Int = 50): List<Song>
    suspend fun getFavoriteArtists(sinceTimestamp: Long, limit: Int = 50): List<FavoriteArtist>
    suspend fun getFavoriteAlbums(sinceTimestamp: Long, limit: Int = 50): List<FavoriteAlbum>
    suspend fun getLastPlayedSongs(sinceTimestamp: Long, limit: Int = 8): List<Song>
}

